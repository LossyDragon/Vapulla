package `in`.dragonbra.vapulla.service.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.os.storage.StorageManager
import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.depotdownloader.DepotDownloader
import `in`.dragonbra.javasteam.depotdownloader.IDownloadListener
import `in`.dragonbra.javasteam.depotdownloader.data.AppItem
import `in`.dragonbra.javasteam.depotdownloader.data.DownloadItem
import `in`.dragonbra.javasteam.depotdownloader.data.PubFileItem
import `in`.dragonbra.javasteam.steam.handlers.steamapps.License
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.api.WorkshopItem
import `in`.dragonbra.vapulla.service.download.DownloadPlanner.manifest
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.runSuspendCatching
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
 * @property chunks pieces written to disk in this run; a chunk is about a megabyte.
 * @property networkBytes compressed bytes received in this run.
 * @property writtenBytes what those came to once decompressed.
 * @property connections how many chunks the downloader fetches at once.
 */
@Immutable
data class DownloadProgress(
    val appId: Int,
    val workshopId: Long,
    val fraction: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val status: String,
    val chunks: Int = 0,
    val chunksPerSecond: Float = 0f,
    val networkBytes: Long = 0L,
    val writtenBytes: Long = 0L,
    val connections: Int = 0,
) {
    /** True while chunks arrive, as opposed to files being checked or made room for. */
    val isDownloading: Boolean get() = status == STATUS_DOWNLOADING

    companion object {
        /** The [status] before the downloader has reported anything. */
        const val STATUS_PREPARING = "Preparing"

        /**
         * The [status] while chunks arrive. Any other status is the downloader's own words
         * for something else it is busy with, such as checking the files already there.
         */
        const val STATUS_DOWNLOADING = "Downloading"
    }
}

/**
 * A place downloads can be installed to: the app's own folder on one of the storage volumes,
 * or a folder the user picked.
 *
 * @property name what Android calls the volume, e.g. "SD card", or "Your folder".
 */
@Immutable
data class InstallLocation(val path: String, val name: String, val freeBytes: Long)

/** A finished download on its way to another install location; [fraction] is 0 to 1. */
@Immutable
data class MoveProgress(val appId: Int, val workshopId: Long, val fraction: Float)

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
 *
 * Six settings shape how it runs: Wi-Fi only holds the queue while the network is metered,
 * the speed limit paces the running download, connections, decompress threads and file
 * writes are how many chunks a download fetches, unpacks and writes at once, and the install
 * location is where new downloads go.
 */
class DownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val appDao: SteamAppDao,
    private val settingsManager: SettingsManager,
) {

    /** What a download needs from the signed-in client. */
    private class Session(val client: SteamClient, val licenses: List<License>)

    /** How the running download ended. */
    private sealed interface Outcome {
        data object Completed : Outcome
        data class Failed(val reason: String) : Outcome
        data object Paused : Outcome

        /**
         * The session went away, or the network became one downloads may not use. The
         * download waits in the queue until it can run again.
         */
        data object Interrupted : Outcome
        data object Removed : Outcome
    }

    /**
     * @property outcome completed by whoever ends the download first: the downloader's
     *   listener, the stall check, or the user.
     * @property finished completed once the downloader is closed and no longer writes files.
     */
    private class Active(val download: Download) {
        val outcome = CompletableDeferred<Outcome>()
        val finished = CompletableDeferred<Unit>()

        fun isFor(appId: Int, workshopId: Long) =
            download.appId == appId && download.workshopId == workshopId
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

    @Volatile
    private var settings = AppSettings()

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    /** Whether the network in use is free of data charges: Wi-Fi or Ethernet, as a rule. */
    private val unmetered = MutableStateFlow(
        connectivity.getNetworkCapabilities(connectivity.activeNetwork).isUnmetered(),
    )

    private val _waitingForWifi = MutableStateFlow(false)

    /** True while the Wi-Fi only setting is what keeps the queue from running. */
    val waitingForWifi: StateFlow<Boolean> = _waitingForWifi.asStateFlow()

    /**
     * The folder everything is installed under, laid out like a Steam library. It is the
     * app's own folder on a storage volume, the only kind of place Android lets an app write
     * plain files to without asking for access to all of the user's files.
     */
    private val libraryRoot: File
        get() = File(settings.downloadLocation)
            .takeIf { settings.downloadLocation.isNotEmpty() && it.isDirectory }
            ?: context.getExternalFilesDir(null)
            ?: context.filesDir

    init {
        connectivity.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    unmetered.value = caps.isUnmetered()
                }

                override fun onLost(network: Network) {
                    unmetered.value = false
                }
            },
        )

        scope.launch {
            downloadDao.requeueInterrupted()
            settings = settingsManager.settings.first()

            launch { followSettings() }

            for (signal in wake) {
                // Whatever goes wrong must not end this loop: nothing would run the queue
                // again until the app is restarted.
                runSuspendCatching { drainQueue() }
                    .onFailure { Timber.e(it, "The download queue stopped until its next signal") }
            }
        }
    }

    /** Keeps [settings] current, and stops or starts the queue as Wi-Fi only requires. */
    private suspend fun followSettings() {
        combine(settingsManager.settings, unmetered) { latest, unmetered ->
            latest to (latest.downloadWifiOnly && !unmetered)
        }.collect { (latest, waiting) ->
            settings = latest
            _waitingForWifi.value = waiting

            if (waiting) {
                active?.outcome?.complete(Outcome.Interrupted)
            } else {
                wake.trySend(Unit)
            }
        }
    }

    private fun NetworkCapabilities?.isUnmetered(): Boolean =
        this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true

    /**
     * Where downloads can be installed: the app's own folder on each storage volume, the
     * device's own first, and after them the folder the user picked in Settings, if any.
     *
     * @param pickedFolder the folder picked in Settings; given by a caller that knows of a
     *   change to it before this class has heard of it.
     */
    fun installLocations(pickedFolder: String = settings.downloadLocation): List<InstallLocation> {
        val storage = context.getSystemService(StorageManager::class.java)

        val volumes = context.getExternalFilesDirs(null).filterNotNull().map { dir ->
            InstallLocation(
                path = dir.absolutePath,
                name = storage.getStorageVolume(dir)?.getDescription(context) ?: "Storage",
                freeBytes = dir.usableSpace,
            )
        }

        val picked = File(pickedFolder)
            .takeIf { pickedFolder.isNotEmpty() && it.isDirectory }
            ?.takeIf { folder -> volumes.none { it.path == folder.absolutePath } }
            ?.let { InstallLocation(it.absolutePath, "Your folder", it.usableSpace) }

        return volumes + listOfNotNull(picked)
    }

    private val _moving = MutableStateFlow<MoveProgress?>(null)

    /** The move that is running, or null. Only one runs at a time. */
    val moving: StateFlow<MoveProgress?> = _moving.asStateFlow()

    private fun isMoving(appId: Int, workshopId: Long): Boolean =
        _moving.value?.let { it.appId == appId && it.workshopId == workshopId } == true

    /**
     * Moves the files of a finished download to the install location [toRoot], keeping its
     * place under `steamapps`. The files are copied first and the old ones deleted only once
     * everything arrived, so a move that fails or is cut short leaves the game where it was.
     * How it ended is noted on the download.
     */
    fun move(appId: Int, workshopId: Long = 0L, toRoot: String) {
        scope.launch {
            val download = downloadDao.find(appId, workshopId) ?: return@launch
            val source = File(download.installDir)

            // The part of the path that is the same in every install location.
            val relative = download.installDir.substringAfter("/$LIBRARY_FOLDER/", "")
            val target = File(toRoot, "$LIBRARY_FOLDER/$relative")

            val refusal = when {
                download.state != DownloadState.COMPLETED -> "Only a finished download can be moved"

                relative.isEmpty() || target == source -> "It is already there"

                !source.isDirectory -> "Its files are no longer where they were"

                target.exists() -> "There is already a folder for it at the new location"

                // Claims the one move there can be, so two started together cannot both run.
                !_moving.compareAndSet(null, MoveProgress(appId, workshopId, 0f)) ->
                    "Another download is being moved"

                else -> null
            }
            if (refusal != null) {
                setState(download, DownloadState.COMPLETED, "Not moved: $refusal")
                return@launch
            }

            try {
                val files = source.walkTopDown().filter { it.isFile }.toList()
                val total = files.sumOf { it.length() }

                target.parentFile?.mkdirs()
                if (total > (target.parentFile?.usableSpace ?: 0L)) {
                    throw IOException("not enough free space, ${total.toFileSize()} needed")
                }

                var copied = 0L
                files.forEach { file ->
                    val copy = File(target, file.relativeTo(source).path)
                    copy.parentFile?.mkdirs()
                    file.copyTo(copy, overwrite = true)

                    copied += file.length()
                    _moving.value = MoveProgress(appId, workshopId, fraction(copied, total))
                }

                downloadDao.setInstallDir(appId, workshopId, target.absolutePath)
                source.deleteRecursively()
                setState(download, DownloadState.COMPLETED, "Moved to ${target.absolutePath}")
            } catch (e: Exception) {
                Timber.w(e, "Moving $appId / $workshopId to $toRoot failed")
                target.deleteRecursively()
                setState(download, DownloadState.COMPLETED, "Not moved: ${e.message}")
            } finally {
                _moving.value = null
            }
        }
    }

    private fun fraction(done: Long, total: Long): Float =
        if (total <= 0L) 1f else (done.toFloat() / total).coerceIn(0f, 1f)

    /** Free space where new downloads are installed. */
    fun freeBytes(): Long = libraryRoot.apply { mkdirs() }.usableSpace

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
     * Space a download could use: what is free, less what the other unfinished downloads
     * have yet to fill. A game that is downloaded already stays where it is, so for an
     * update it is the space on that storage that counts, not on the one new downloads go to.
     */
    suspend fun availableBytes(appId: Int, workshopId: Long = 0L): Long =
        withContext(Dispatchers.IO) {
            val installed = downloadDao.find(appId, workshopId)?.installDir?.let(::File)
            val free = installed?.apply { mkdirs() }?.usableSpace ?: freeBytes()

            (free - downloadDao.reservedBytes(appId, workshopId)).coerceAtLeast(0)
        }

    /**
     * Adds [app] to the queue with the depots of [plan]. Does nothing if it is in it already.
     *
     * @param branchPassword the password of the plan's branch, when it is behind one.
     * @return null when queued, otherwise why it was not.
     */
    suspend fun enqueue(app: SteamApp, plan: DownloadPlan, branchPassword: String = ""): String? {
        val folder = app.installDir.replace(UNSAFE_IN_FILE_NAME, "_").trim().trim('.')
            .ifEmpty { app.id.toString() }

        return enqueue(
            Download(
                appId = app.id,
                name = app.name.ifEmpty { "App ${app.id}" },
                headerUrl = app.headerImageUrl,
                os = plan.os.name,
                depotIds = plan.depotIds,
                branch = plan.branch,
                branchPassword = branchPassword,
                language = plan.language,
                installDir = "$LIBRARY_FOLDER/common/$folder",
                totalBytes = plan.installBytes,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Adds a workshop [item] of [appId] to the queue. Does nothing if it is in it already.
     *
     * @return null when queued, otherwise why it was not.
     */
    suspend fun enqueueWorkshop(appId: Int, item: WorkshopItem): String? = enqueue(
        Download(
            appId = appId,
            workshopId = item.id,
            name = item.title.ifEmpty { "Workshop item ${item.id}" },
            headerUrl = item.previewUrl,
            os = SteamApp.OS.none.name,
            depotIds = emptyList(),
            installDir = "$LIBRARY_FOLDER/workshop/content/$appId/${item.id}",
            totalBytes = item.fileSize,
            addedAt = System.currentTimeMillis(),
        ),
    )

    /** Queues [download], whose `installDir` is still relative to the library folder. */
    private suspend fun enqueue(download: Download): String? =
        // Touches the disk (free space, old files), and callers are on the main thread.
        withContext(Dispatchers.IO) {
            val existing = downloadDao.find(download.appId, download.workshopId)
            val state = existing?.state
            if (state == DownloadState.QUEUED || state == DownloadState.DOWNLOADING) {
                return@withContext null
            }
            // Paused: carry on with what is there. Queued anew, it would start from nothing.
            if (state == DownloadState.PAUSED &&
                existing.os == download.os && existing.language == download.language &&
                existing.branch == download.branch
            ) {
                resume(download.appId, download.workshopId)
                return@withContext null
            }
            if (isMoving(download.appId, download.workshopId)) {
                return@withContext "It is being moved; try again when that is done"
            }

            // Checked here as well as in the dialog: the figure on screen can be out of date.
            val available = availableBytes(download.appId, download.workshopId)
            if (download.totalBytes > available) {
                return@withContext "Not enough space: ${download.totalBytes.toFileSize()} " +
                    "needed, ${available.toFileSize()} available"
            }

            // The downloader only cleans up after depots it is asked for again, so another
            // platform's or language's files would be left lying next to the new ones.
            if (existing != null &&
                (existing.os != download.os || existing.language != download.language)
            ) {
                File(existing.installDir).deleteRecursively()
            }

            // Downloaded again, it stays where it is, wherever new downloads go by now.
            val installDir = existing?.installDir
                ?: File(libraryRoot, download.installDir).absolutePath

            downloadDao.insert(download.copy(installDir = installDir))
            wake.trySend(Unit)
            null
        }

    /**
     * Pauses a download, whether it is running or waiting its turn. Both are done, the row
     * first: a download that is just being started is caught by one or the other.
     */
    fun pause(appId: Int, workshopId: Long = 0L) {
        scope.launch {
            downloadDao.pauseQueued(appId, workshopId)
            active?.takeIf { it.isFor(appId, workshopId) }?.outcome?.complete(Outcome.Paused)
        }
    }

    /** Puts a paused or failed download back in the queue. It picks up where it stopped. */
    fun resume(appId: Int, workshopId: Long = 0L) {
        scope.launch {
            downloadDao.requeue(appId, workshopId)
            wake.trySend(Unit)
        }
    }

    /**
     * Queues a download that is not running to have its files checked against Steam's. Any
     * that are damaged or missing are downloaded again.
     */
    fun verify(appId: Int, workshopId: Long = 0L) {
        // Its files are on the move; checking them now would find half of them missing.
        if (isMoving(appId, workshopId)) return

        scope.launch {
            downloadDao.requeueToVerify(appId, workshopId)
            wake.trySend(Unit)
        }
    }

    /** Takes a download off the list, stopping it first, and with [deleteFiles] off the disk. */
    fun remove(appId: Int, workshopId: Long = 0L, deleteFiles: Boolean) {
        scope.launch {
            val download = downloadDao.find(appId, workshopId) ?: return@launch
            if (isMoving(appId, workshopId)) return@launch

            // Files can only be deleted once the downloader has stopped writing them.
            active?.takeIf { it.isFor(appId, workshopId) }?.let { current ->
                current.outcome.complete(Outcome.Removed)
                current.finished.await()
            }

            downloadDao.delete(appId, workshopId)
            NotificationHelper.cancelDownloadFinished(context, download.notificationId)

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
            if (_waitingForWifi.value) return
            val next = downloadDao.nextQueued() ?: return

            runSuspendCatching { download(next, current) }.onFailure {
                Timber.e(it, "Download of ${next.appId} failed to run")
                setState(next, DownloadState.FAILED, it.message ?: it.toString())
            }
        }
    }

    private suspend fun setState(
        download: Download,
        state: DownloadState,
        message: String? = null,
    ) {
        downloadDao.setState(download.appId, download.workshopId, state, message)
    }

    private suspend fun download(download: Download, session: Session) {
        val installDir = File(download.installDir).apply { mkdirs() }

        val missing = download.totalBytes - download.downloadedBytes
        val free = installDir.usableSpace
        if (missing > free) {
            setState(
                download,
                DownloadState.FAILED,
                "Not enough free space: ${missing.toFileSize()} needed, ${free.toFileSize()} free",
            )
            return
        }

        val current = Active(download)
        // Read once: the downloader is built with it, and the figures on screen should match.
        val connections = settings.downloadConnections
        val decompressThreads = settings.downloadDecompressThreads.takeIf { it > 0 }
            ?: (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        val tracker = ProgressTracker(download, depotSizes(download), connections)
        val perf = DownloadPerf(
            context = context,
            label = "${download.appId} / ${download.workshopId}",
            settings = "$connections connections, $decompressThreads decompress threads, " +
                "${settings.downloadFileWrites} file writes, " +
                "limit ${settings.downloadSpeedLimit} MB/s, verify ${download.verify}",
        )

        // The service keeps the process alive, but not the CPU once the screen is off.
        val wakeLock = context.getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vapulla:download")
            .apply { acquire(WAKE_LOCK_LIMIT.inWholeMilliseconds) }

        var downloader: DepotDownloader? = null

        try {
            // From here on pause() and remove() reach this download through `active`. One
            // that came just before could only change the row, so the row is asked once more.
            active = current
            val state = downloadDao.find(download.appId, download.workshopId)?.state
            if (state != DownloadState.QUEUED) return

            setState(download, DownloadState.DOWNLOADING)
            _progress.value = tracker.snapshot(bytesPerSecond = 0)

            downloader = DepotDownloader(
                steamClient = session.client,
                licenses = session.licenses,
                // Logs a line per chunk, which is too much even for a debug build.
                debug = false,
                maxDownloads = connections,
                maxFileWrites = settings.downloadFileWrites,
                maxDecompress = decompressThreads,
            )

            downloader.addListener(listener(current, tracker))

            // The listener is not told about every way a download can end, so the queue
            // running empty without a result counts as a failure. First completion wins.
            downloader.getCompletion().whenComplete { _, _ ->
                current.outcome.complete(Outcome.Failed("The downloader stopped without a result"))
            }

            downloader.add(download.toItem())
            downloader.finishAdding()

            val ticker = scope.launch { reportProgress(current, tracker, perf) }
            val outcome = try {
                current.outcome.await()
            } finally {
                ticker.cancel()
            }
            perf.finish(tracker.snapshot(bytesPerSecond = 0), outcome.toString())

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

    /** What the downloader is asked for: the app's chosen depots, or the workshop item. */
    private fun Download.toItem(): DownloadItem = if (isWorkshop) {
        PubFileItem(
            appId = appId,
            pubFile = workshopId,
            installDirectory = installDir,
            verify = verify,
        )
    } else {
        AppItem(
            appId = appId,
            installDirectory = installDir,
            branch = branch,
            branchPassword = branchPassword.ifEmpty { null },
            os = os.takeIf { it != SteamApp.OS.none.name },
            osArch = OS_ARCH,
            language = language,
            depot = depotIds,
            verify = verify,
        )
    }

    private suspend fun finish(download: Download, tracker: ProgressTracker, outcome: Outcome) {
        Timber.i("Download of ${download.appId} / ${download.workshopId} ended: $outcome")

        if (outcome != Outcome.Removed && outcome != Outcome.Completed) {
            downloadDao.updateProgress(
                download.appId,
                download.workshopId,
                tracker.downloadedBytes(),
            )
        }

        when (outcome) {
            Outcome.Completed -> {
                val done = tracker.completedDepots
                val wanted = download.depotIds.size

                // A workshop item can be a single file from a web address, with no depot at all.
                if (done == 0 && !download.isWorkshop) {
                    // The downloader reports success even when it had to skip every depot.
                    fail(download, "Steam did not provide any of the depots to this account")
                } else {
                    val note = when {
                        done < wanted -> tracker.skipReasons.joinToString(" ").ifEmpty {
                            "$done of $wanted depots; the rest were not available"
                        }

                        download.verify -> VERIFIED

                        else -> null
                    }
                    // What Steam's catalogue says the depots hold now, to tell later when
                    // that has moved on. A workshop item has no depots to compare.
                    val manifestIds = appDao.findApp(download.appId)
                        ?.takeIf { !download.isWorkshop }
                        ?.let { app ->
                            DownloadPlanner.manifestIds(app, download.depotIds, download.branch)
                        }
                        .orEmpty()

                    downloadDao.complete(
                        appId = download.appId,
                        workshopId = download.workshopId,
                        message = note,
                        finishedAt = System.currentTimeMillis(),
                        manifestIds = manifestIds,
                    )
                    NotificationHelper.showDownloadFinished(
                        context = context,
                        id = download.notificationId,
                        title = download.name,
                        text = if (download.verify) VERIFIED else "Download complete",
                    )
                }
            }

            is Outcome.Failed -> fail(download, outcome.reason)

            Outcome.Paused -> setState(download, DownloadState.PAUSED)

            Outcome.Interrupted -> setState(download, DownloadState.QUEUED)

            Outcome.Removed -> Unit
        }
    }

    private suspend fun fail(download: Download, reason: String) {
        setState(download, DownloadState.FAILED, reason)
        NotificationHelper.showDownloadFinished(
            context = context,
            id = download.notificationId,
            title = download.name,
            text = "Download failed: $reason",
        )
    }

    private fun listener(current: Active, tracker: ProgressTracker) = object : IDownloadListener {
        val limiter = SpeedLimiter(
            bytesPerSecond = { settings.downloadSpeedLimit * BYTES_PER_MEGABYTE },
            stopped = { current.outcome.isCompleted },
        )

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
            Timber.w("Depot $depotId of ${current.download.appId} skipped: $reason")
            tracker.onDepotSkipped(reason)
        }

        override fun onStatusUpdate(message: String) {
            tracker.onStatus(message)
        }

        // Called for every chunk, from the threads that write chunks to disk, so holding it
        // up is what holds the download to the speed limit. onFileCompleted follows a
        // file's last chunk and has nothing to add.
        override fun onChunkCompleted(
            depotId: Int,
            depotPercentComplete: Float,
            compressedBytes: Long,
            uncompressedBytes: Long,
        ) {
            val received = tracker.onProgress(
                depotId = depotId,
                fraction = depotPercentComplete,
                networkBytes = compressedBytes,
                writtenBytes = uncompressedBytes,
            )
            limiter.pace(received)
        }

        override fun onDepotCompleted(
            depotId: Int,
            compressedBytes: Long,
            uncompressedBytes: Long,
        ) {
            tracker.onDepotCompleted(depotId, compressedBytes, uncompressedBytes)
        }
    }

    /**
     * Publishes the tracker's figures twice a second while a download runs, saves them now and
     * then, and ends the download when the downloader has gone quiet for too long.
     */
    private suspend fun CoroutineScope.reportProgress(
        current: Active,
        tracker: ProgressTracker,
        perf: DownloadPerf,
    ) {
        var tick = 0
        var quietTicks = 0
        var lastActivity = tracker.activity
        var lastNetworkBytes = tracker.networkBytes()
        var lastChunks = tracker.chunks
        var speed = 0.0
        var chunkRate = 0.0

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
                val minutes = STALL_AFTER.inWholeMinutes
                current.outcome.complete(
                    Outcome.Failed(
                        "Stalled: nothing arrived for $minutes minutes. Retry to continue.",
                    ),
                )
                return
            }

            val networkBytes = tracker.networkBytes()
            val measured = (networkBytes - lastNetworkBytes) * 1000.0 / TICK.inWholeMilliseconds
            lastNetworkBytes = networkBytes
            speed = speed * SPEED_SMOOTHING + measured * (1 - SPEED_SMOOTHING)

            val chunks = tracker.chunks
            val measuredChunks = (chunks - lastChunks) * 1000.0 / TICK.inWholeMilliseconds
            lastChunks = chunks
            chunkRate = chunkRate * SPEED_SMOOTHING + measuredChunks * (1 - SPEED_SMOOTHING)

            val snapshot = tracker.snapshot(speed.toLong(), chunkRate.toFloat())
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
            if (tick % PERF_EVERY == 0) {
                perf.sample(snapshot)
            }
            if (tick % SAVE_EVERY == 0) {
                downloadDao.updateProgress(
                    current.download.appId,
                    current.download.workshopId,
                    snapshot.downloadedBytes,
                )
            }
        }
    }

    /** Depot ID to its size on disk, the weights for turning depot progress into app progress. */
    private suspend fun depotSizes(download: Download): Map<Int, Long> {
        val depots = appDao.findApp(download.appId)?.depots.orEmpty()
        return download.depotIds.associateWith { id ->
            depots[id]?.manifest(download.branch)?.size ?: 0L
        }
    }

    private companion object {
        val TICK = 500.milliseconds

        /** Long, because checking a large file that is already there takes a while. */
        val STALL_AFTER = 2.minutes
        val STALL_TICKS = (STALL_AFTER / TICK).toInt()
        const val NOTIFY_EVERY = 2
        const val SAVE_EVERY = 10

        /** Ten seconds of [TICK]s between two lines of [DownloadPerf]. */
        const val PERF_EVERY = 20

        /** Share of the previous speed kept at each tick. */
        const val SPEED_SMOOTHING = 0.8

        const val BYTES_PER_MEGABYTE = 1024L * 1024L

        /** The folder of an install location that everything is under, as in a Steam library. */
        const val LIBRARY_FOLDER = "steamapps"

        /** Depots are picked for 64-bit, the only kind of Steam client there still is. */
        const val OS_ARCH = "64"

        /** The note on a download whose files were checked rather than fetched. */
        const val VERIFIED = "Files verified"

        /** Characters an app's folder name may not keep, so it is a valid name anywhere. */
        val UNSAFE_IN_FILE_NAME = Regex("""[\\/:*?"<>|]""")

        /** Upper bound on holding the wake lock, in case a download never ends. */
        val WAKE_LOCK_LIMIT = 12.hours
    }
}
