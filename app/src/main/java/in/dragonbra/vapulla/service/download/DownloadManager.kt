package `in`.dragonbra.vapulla.service.download

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Live figures of the download that is running. Everything else about a download is in its
 * [Download] row.
 *
 * @property bytesPerSecond network speed, measured on the compressed data.
 * @property status what the downloader is busy with, e.g. validating a file.
 */
@Immutable
data class DownloadProgress(
    val appId: Int,
    val fraction: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val status: String,
)

/**
 * Runs the download queue on top of JavaSteam's [DepotDownloader]: one app at a time, oldest
 * first, for as long as there is a Steam session to download with.
 *
 * The queue itself is the `download` table, so it survives the process. This class only holds
 * the download that is running. An app-wide singleton rather than part of the service, since
 * screens use it whether or not the service is bound.
 *
 * [DepotDownloader] can only be stopped as a whole, so each download gets its own instance,
 * and pausing closes it. Resuming is a new instance over the same folder: the downloader
 * checks the files that are already there and fetches only the chunks still missing.
 */
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val appDao: SteamAppDao,
) {

    /** What a download needs from the signed-in client. */
    private class Session(val client: SteamClient, val licenses: List<License>)

    /** How the running download ended. */
    private sealed interface Outcome {
        data object Completed : Outcome
        data class Failed(val reason: String) : Outcome
        data object Paused : Outcome

        /** The session went away; the download waits in the queue for the next one. */
        data object Interrupted : Outcome
        data object Removed : Outcome
    }

    /**
     * @property outcome completed by whoever ends the download first: the downloader's
     *   listener, the stall check, or the user.
     * @property finished completed once the downloader is closed and no longer writes files.
     */
    private class Active(val appId: Int) {
        val outcome = CompletableDeferred<Outcome>()
        val finished = CompletableDeferred<Unit>()
    }

    private val scope = CoroutineScope(
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("DownloadManager") +
            CoroutineExceptionHandler { _, error -> Timber.e(error, "Uncaught download error") },
    )

    /** Tells the queue loop to look again; conflated, so signals never pile up. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    @Volatile
    private var session: Session? = null

    @Volatile
    private var active: Active? = null

    private val _progress = MutableStateFlow<DownloadProgress?>(null)

    /** The running download's live figures, or null while nothing is downloading. */
    val progress: StateFlow<DownloadProgress?> = _progress.asStateFlow()

    /** Where apps are installed: app-specific storage, which needs no permission. */
    private val libraryDir: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "steamapps/common")

    init {
        scope.launch {
            downloadDao.requeueInterrupted()

            for (signal in wake) {
                drainQueue()
            }
        }
    }

    /** Free space where apps are installed. */
    fun freeBytes(): Long = libraryDir.apply { mkdirs() }.usableSpace

    /** A Steam session is up: downloads can run, and whatever is queued starts. */
    fun attach(client: SteamClient, licenses: List<License>) {
        Timber.i("Downloads attached to a session with ${licenses.size} licenses")
        session = Session(client, licenses)
        wake.trySend(Unit)
    }

    /** The session is gone. The running download goes back in the queue for the next one. */
    fun detach() {
        if (session == null) return

        Timber.i("Downloads detached from the session")
        session = null
        active?.outcome?.complete(Outcome.Interrupted)
    }

    /**
     * Space a download of [appId] could use: what is free, less what the other unfinished
     * downloads have yet to fill.
     */
    suspend fun availableBytes(appId: Int): Long = withContext(Dispatchers.IO) {
        (freeBytes() - downloadDao.reservedBytes(appId)).coerceAtLeast(0)
    }

    /**
     * Adds [app] to the queue with the depots of [plan]. Does nothing if it is in it already.
     *
     * @return null when queued, otherwise why it was not.
     */
    suspend fun enqueue(app: SteamApp, plan: DownloadPlan): String? =
        // Touches the disk (free space, old files), and callers are on the main thread.
        withContext(Dispatchers.IO) { enqueueBlocking(app, plan) }

    private suspend fun enqueueBlocking(app: SteamApp, plan: DownloadPlan): String? {
        val existing = downloadDao.find(app.id)
        val state = existing?.state
        if (state == DownloadState.QUEUED || state == DownloadState.DOWNLOADING) {
            return null
        }

        // Checked here as well as in the dialog: the figure on screen can be out of date.
        val available = availableBytes(app.id)
        if (plan.installBytes > available) {
            return "Not enough space: ${plan.installBytes.toFileSize()} needed, " +
                "${available.toFileSize()} available"
        }

        // The downloader only cleans up after depots it is asked for again, so another
        // platform's files would be left lying next to the new ones.
        if (existing != null && existing.os != plan.os.name) {
            File(existing.installDir).deleteRecursively()
        }

        val folder = app.installDir.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().trim('.')
            .ifEmpty { app.id.toString() }

        downloadDao.insert(
            Download(
                appId = app.id,
                name = app.name.ifEmpty { "App ${app.id}" },
                headerUrl = app.getHeaderImageUrl()
                    ?: "https://cdn.cloudflare.steamstatic.com/steam/apps/${app.id}/header.jpg",
                os = plan.os.name,
                depotIds = plan.depotIds,
                installDir = File(libraryDir, folder).absolutePath,
                totalBytes = plan.installBytes,
                addedAt = System.currentTimeMillis(),
            ),
        )

        wake.trySend(Unit)
        return null
    }

    fun pause(appId: Int) {
        val current = active
        if (current?.appId == appId) {
            current.outcome.complete(Outcome.Paused)
        } else {
            scope.launch { downloadDao.pauseQueued(appId) }
        }
    }

    /** Puts a paused or failed download back in the queue. It picks up where it stopped. */
    fun resume(appId: Int) {
        scope.launch {
            downloadDao.requeue(appId)
            wake.trySend(Unit)
        }
    }

    /** Takes a download off the list, stopping it first, and with [deleteFiles] off the disk. */
    fun remove(appId: Int, deleteFiles: Boolean) {
        scope.launch {
            val download = downloadDao.find(appId) ?: return@launch

            // Files can only be deleted once the downloader has stopped writing them.
            active?.takeIf { it.appId == appId }?.let { current ->
                current.outcome.complete(Outcome.Removed)
                current.finished.await()
            }

            downloadDao.delete(appId)
            NotificationHelper.cancelDownloadFinished(context, appId)

            if (deleteFiles) {
                val deleted = File(download.installDir).deleteRecursively()
                Timber.i("Deleted ${download.installDir}: $deleted")
            }
        }
    }

    /** Clears completed and failed downloads from the list. Their files are kept. */
    fun clearFinished() {
        scope.launch { downloadDao.clearFinished() }
    }

    private suspend fun drainQueue() {
        while (true) {
            val current = session ?: return
            val next = downloadDao.nextQueued() ?: return

            try {
                download(next, current)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Download of ${next.appId} failed to run")
                downloadDao.setState(next.appId, DownloadState.FAILED, e.message ?: e.toString())
            }
        }
    }

    private suspend fun download(download: Download, session: Session) {
        val appId = download.appId
        val installDir = File(download.installDir).apply { mkdirs() }

        val missing = download.totalBytes - download.downloadedBytes
        val free = installDir.usableSpace
        if (missing > free) {
            downloadDao.setState(
                appId,
                DownloadState.FAILED,
                "Not enough free space: ${missing.toFileSize()} needed, ${free.toFileSize()} free",
            )
            return
        }

        val current = Active(appId)
        val tracker = ProgressTracker(download, depotSizes(download))

        active = current
        downloadDao.setState(appId, DownloadState.DOWNLOADING)
        _progress.value = tracker.snapshot(bytesPerSecond = 0)

        // The service keeps the process alive, but not the CPU once the screen is off.
        val wakeLock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vapulla:download")
            .apply { acquire(WAKE_LOCK_LIMIT.inWholeMilliseconds) }

        var downloader: DepotDownloader? = null

        try {
            downloader = DepotDownloader(
                steamClient = session.client,
                licenses = session.licenses,
                // Logs a line per chunk, which is too much even for a debug build.
                debug = false,
            )

            downloader.addListener(listener(current, tracker))

            // The listener is not told about every way a download can end, so the queue
            // running empty without a result counts as a failure. First completion wins.
            downloader.getCompletion().whenComplete { _, _ ->
                current.outcome.complete(Outcome.Failed("The downloader stopped without a result"))
            }

            downloader.add(
                AppItem(
                    appId = appId,
                    installDirectory = download.installDir,
                    branch = DownloadPlanner.BRANCH,
                    os = download.os.takeIf { it != SteamApp.OS.none.name },
                    osArch = "64",
                    language = DownloadPlanner.LANGUAGE,
                    depot = download.depotIds,
                ),
            )
            downloader.finishAdding()

            val ticker = scope.launch { reportProgress(current, tracker) }
            val outcome = try {
                current.outcome.await()
            } finally {
                ticker.cancel()
            }

            // Stop the downloader before the result is visible, so that by the time a
            // download shows as paused or failed nothing writes to its files any more.
            downloader.close()
            downloader = null

            finish(download, tracker, outcome)
        } finally {
            runCatching { downloader?.close() }
            if (wakeLock.isHeld) wakeLock.release()

            active = null
            _progress.value = null
            NotificationHelper.cancelDownloadProgress(context)
            current.finished.complete(Unit)
        }
    }

    private suspend fun finish(download: Download, tracker: ProgressTracker, outcome: Outcome) {
        val appId = download.appId
        Timber.i("Download of $appId ended: $outcome")

        if (outcome != Outcome.Removed && outcome != Outcome.Completed) {
            downloadDao.updateProgress(appId, tracker.downloadedBytes())
        }

        when (outcome) {
            Outcome.Completed -> {
                val done = tracker.completedDepots
                val wanted = download.depotIds.size

                if (done == 0) {
                    // The downloader reports success even when it had to skip every depot.
                    fail(download, "Steam did not provide any of the depots to this account")
                } else {
                    val note = tracker.skipReasons.joinToString(" ").ifEmpty {
                        "$done of $wanted depots; the rest were not available"
                    }
                    downloadDao.complete(
                        appId = appId,
                        message = note.takeIf { done < wanted },
                        finishedAt = System.currentTimeMillis(),
                    )
                    NotificationHelper.showDownloadFinished(
                        context = context,
                        appId = appId,
                        title = download.name,
                        text = "Download complete",
                    )
                }
            }

            is Outcome.Failed -> fail(download, outcome.reason)

            Outcome.Paused -> downloadDao.setState(appId, DownloadState.PAUSED)

            Outcome.Interrupted -> downloadDao.setState(appId, DownloadState.QUEUED)

            Outcome.Removed -> Unit
        }
    }

    private suspend fun fail(download: Download, reason: String) {
        downloadDao.setState(download.appId, DownloadState.FAILED, reason)
        NotificationHelper.showDownloadFinished(
            context = context,
            appId = download.appId,
            title = download.name,
            text = "Download failed: $reason",
        )
    }

    private fun listener(current: Active, tracker: ProgressTracker) = object : IDownloadListener {
        override fun onDownloadCompleted(item: DownloadItem) {
            current.outcome.complete(Outcome.Completed)
        }

        override fun onDownloadFailed(item: DownloadItem, error: Throwable) {
            Timber.w(error, "Download of ${item.appId} failed")

            // A manifest that cannot be fetched surfaces as a bare cancellation.
            val reason = if (error is CancellationException) {
                "Steam's content servers could not serve this download"
            } else {
                error.message ?: error.javaClass.simpleName
            }
            current.outcome.complete(Outcome.Failed(reason))
        }

        override fun onDepotSkipped(depotId: Int, reason: String) {
            Timber.w("Depot $depotId of ${current.appId} skipped: $reason")
            tracker.onDepotSkipped(reason)
        }

        override fun onStatusUpdate(message: String) {
            tracker.onStatus(message)
        }

        override fun onFileCompleted(depotId: Int, fileName: String, depotPercentComplete: Float) {
            tracker.onProgress(depotId, depotPercentComplete)
        }

        override fun onChunkCompleted(
            depotId: Int,
            depotPercentComplete: Float,
            compressedBytes: Long,
            uncompressedBytes: Long,
        ) {
            tracker.onProgress(depotId, depotPercentComplete, compressedBytes)
        }

        override fun onDepotCompleted(
            depotId: Int,
            compressedBytes: Long,
            uncompressedBytes: Long,
        ) {
            tracker.onDepotCompleted(depotId, compressedBytes)
        }
    }

    /**
     * Publishes the tracker's figures twice a second while a download runs, saves them now and
     * then, and ends the download when the downloader has gone quiet for too long.
     */
    private suspend fun CoroutineScope.reportProgress(current: Active, tracker: ProgressTracker) {
        var tick = 0
        var quietTicks = 0
        var lastActivity = tracker.activity
        var lastNetworkBytes = tracker.networkBytes()
        var speed = 0.0

        while (isActive) {
            delay(TICK)
            tick++

            // Counted in ticks rather than by the clock: while the device sleeps nothing
            // arrives, but no ticks pass either, so waking up does not look like a stall.
            val activity = tracker.activity
            if (activity != lastActivity) {
                lastActivity = activity
                quietTicks = 0
            } else if (++quietTicks >= STALL_TICKS) {
                current.outcome.complete(
                    Outcome.Failed("Stalled: nothing arrived for 2 minutes. Retry to continue."),
                )
                return
            }

            val networkBytes = tracker.networkBytes()
            val measured = (networkBytes - lastNetworkBytes) * 1000.0 / TICK.inWholeMilliseconds
            lastNetworkBytes = networkBytes
            speed = speed * SPEED_SMOOTHING + measured * (1 - SPEED_SMOOTHING)

            val snapshot = tracker.snapshot(bytesPerSecond = speed.toLong())
            _progress.value = snapshot

            if (tick % NOTIFY_EVERY == 0) {
                NotificationHelper.showDownloadProgress(
                    context = context,
                    title = tracker.name,
                    fraction = snapshot.fraction,
                    text = "${snapshot.downloadedBytes.toFileSize()} of " +
                        "${snapshot.totalBytes.toFileSize()} · " +
                        "${snapshot.bytesPerSecond.toFileSize()}/s",
                )
            }
            if (tick % SAVE_EVERY == 0) {
                downloadDao.updateProgress(current.appId, snapshot.downloadedBytes)
            }
        }
    }

    /** Depot ID to its size on disk, the weights for turning depot progress into app progress. */
    private suspend fun depotSizes(download: Download): Map<Int, Long> {
        val depots = appDao.findApp(download.appId)?.depots.orEmpty()
        return download.depotIds.associateWith { id ->
            depots[id]?.manifests?.get(DownloadPlanner.BRANCH)?.size ?: 0L
        }
    }

    /**
     * Turns the downloader's per-depot callbacks into figures for the whole app. The callbacks
     * arrive on the downloader's threads, many per second, so this only stores the latest
     * values; [reportProgress] reads them at its own pace.
     */
    private class ProgressTracker(private val download: Download, sizes: Map<Int, Long>) {

        val name: String get() = download.name

        /** Sizes when all are known, otherwise every depot counts the same. */
        private val weights: Map<Int, Long> =
            if (sizes.isNotEmpty() && sizes.values.all { it > 0 }) {
                sizes
            } else {
                download.depotIds.associateWith { 1L }
            }

        private val depotFractions = ConcurrentHashMap<Int, Float>()
        private val depotNetworkBytes = ConcurrentHashMap<Int, Long>()
        private val completed = ConcurrentHashMap.newKeySet<Int>()
        private val activityCounter = AtomicLong()

        @Volatile
        private var status = "Preparing"

        /** Changes whenever the downloader reports anything at all. */
        val activity: Long get() = activityCounter.get()

        val completedDepots: Int get() = completed.size

        /** Why the downloader left depots out, in its own words. */
        val skipReasons: List<String> get() = skipped.toList()

        private val skipped = CopyOnWriteArrayList<String>()

        fun onDepotSkipped(reason: String) {
            skipped.add(reason)
            activityCounter.incrementAndGet()
        }

        fun onStatus(message: String) {
            status = message
            activityCounter.incrementAndGet()
        }

        fun onProgress(depotId: Int, fraction: Float, networkBytes: Long? = null) {
            // A depot with nothing to download reports 0 / 0.
            if (fraction.isFinite()) {
                depotFractions.merge(depotId, fraction.coerceIn(0f, 1f)) { a, b -> maxOf(a, b) }
            }
            if (networkBytes != null) {
                depotNetworkBytes.merge(depotId, networkBytes) { a, b -> maxOf(a, b) }
            }
            status = "Downloading"
            activityCounter.incrementAndGet()
        }

        fun onDepotCompleted(depotId: Int, networkBytes: Long) {
            completed.add(depotId)
            depotFractions[depotId] = 1f
            depotNetworkBytes.merge(depotId, networkBytes) { a, b -> maxOf(a, b) }
            activityCounter.incrementAndGet()
        }

        /** Compressed bytes received in this run. */
        fun networkBytes(): Long = depotNetworkBytes.values.sum()

        fun downloadedBytes(): Long = (fraction() * download.totalBytes).toLong()

        fun snapshot(bytesPerSecond: Long) = DownloadProgress(
            appId = download.appId,
            fraction = fraction(),
            downloadedBytes = downloadedBytes(),
            totalBytes = download.totalBytes,
            bytesPerSecond = bytesPerSecond,
            status = status,
        )

        /**
         * A depot only reports once its first chunk is written, and until then what it already
         * has on disk from an earlier run counts as nothing. So progress never shows less than
         * what the last run saved.
         */
        private fun fraction(): Float {
            val total = weights.values.sum()
            if (total <= 0) {
                return download.fraction
            }

            val done = weights.entries.sumOf { (depotId, weight) ->
                (depotFractions[depotId] ?: 0f).toDouble() * weight
            }
            return maxOf((done / total).toFloat(), download.fraction).coerceIn(0f, 1f)
        }
    }

    private companion object {
        val TICK = 500.milliseconds

        /** Two minutes of [TICK]s. Checking a large file that is already there takes a while. */
        const val STALL_TICKS = 240
        const val NOTIFY_EVERY = 2
        const val SAVE_EVERY = 10

        /** Share of the previous speed kept at each tick. */
        const val SPEED_SMOOTHING = 0.8

        /** Upper bound on holding the wake lock, in case a download never ends. */
        val WAKE_LOCK_LIMIT = 12.hours
    }
}
