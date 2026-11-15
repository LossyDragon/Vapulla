package `in`.dragonbra.vapulla.service.download

import `in`.dragonbra.vapulla.db.entity.Download
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Turns the downloader's per-depot callbacks into figures for the whole app. The callbacks
 * arrive on the downloader's threads, many per second, so this only stores the latest
 * values; [DownloadManager] reads them at its own pace.
 *
 * @param sizes depot ID to its size on disk, the weights for turning depot progress into app
 *   progress.
 * @param connections how many chunks the downloader fetches at once, passed on as it is.
 */
internal class ProgressTracker(
    private val download: Download,
    sizes: Map<Int, Long>,
    private val connections: Int,
) {

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
    private val depotWrittenBytes = ConcurrentHashMap<Int, Long>()
    private val completed = ConcurrentHashMap.newKeySet<Int>()
    private val skipped = CopyOnWriteArrayList<String>()
    private val activityCounter = AtomicLong()
    private val chunkCounter = AtomicInteger()

    @Volatile
    private var status = DownloadProgress.STATUS_PREPARING

    /** Changes whenever the downloader reports anything at all. */
    val activity: Long get() = activityCounter.get()

    /** Chunks written in this run. */
    val chunks: Int get() = chunkCounter.get()

    val completedDepots: Int get() = completed.size

    /** Why the downloader left depots out, in its own words. */
    val skipReasons: List<String> get() = skipped.toList()

    fun onDepotSkipped(reason: String) {
        skipped.add(reason)
        activityCounter.incrementAndGet()
    }

    fun onStatus(message: String) {
        status = message
        activityCounter.incrementAndGet()
    }

    /**
     * One chunk was written.
     *
     * @param networkBytes compressed bytes the depot has received so far, when known.
     * @param writtenBytes the same, decompressed.
     * @return how many of the compressed bytes are new since the depot last reported.
     */
    fun onProgress(
        depotId: Int,
        fraction: Float,
        networkBytes: Long? = null,
        writtenBytes: Long? = null,
    ): Long {
        chunkCounter.incrementAndGet()

        // A depot with nothing to download reports 0 / 0.
        if (fraction.isFinite()) {
            depotFractions.merge(depotId, fraction.coerceIn(0f, 1f)) { a, b -> maxOf(a, b) }
        }
        if (writtenBytes != null) {
            depotWrittenBytes.merge(depotId, writtenBytes) { a, b -> maxOf(a, b) }
        }

        var added = 0L
        if (networkBytes != null) {
            depotNetworkBytes.compute(depotId) { _, before ->
                added = (networkBytes - (before ?: 0L)).coerceAtLeast(0)
                maxOf(before ?: 0L, networkBytes)
            }
        }
        status = DownloadProgress.STATUS_DOWNLOADING
        activityCounter.incrementAndGet()
        return added
    }

    fun onDepotCompleted(depotId: Int, networkBytes: Long, writtenBytes: Long) {
        completed.add(depotId)
        depotFractions[depotId] = 1f
        depotNetworkBytes.merge(depotId, networkBytes) { a, b -> maxOf(a, b) }
        depotWrittenBytes.merge(depotId, writtenBytes) { a, b -> maxOf(a, b) }
        activityCounter.incrementAndGet()
    }

    /** Compressed bytes received in this run. */
    fun networkBytes(): Long = depotNetworkBytes.values.sum()

    fun downloadedBytes(): Long = (fraction() * download.totalBytes).toLong()

    fun snapshot(bytesPerSecond: Long, chunksPerSecond: Float = 0f) = DownloadProgress(
        appId = download.appId,
        workshopId = download.workshopId,
        fraction = fraction(),
        downloadedBytes = downloadedBytes(),
        totalBytes = download.totalBytes,
        bytesPerSecond = bytesPerSecond,
        status = status,
        chunks = chunks,
        chunksPerSecond = chunksPerSecond,
        networkBytes = networkBytes(),
        writtenBytes = depotWrittenBytes.values.sum(),
        connections = connections,
    )

    /**
     * A depot only reports once its first chunk is written, and until then what it already
     * has on disk from an earlier run counts as nothing. So progress never shows less than
     * what the last run saved.
     */
    private fun fraction(): Float {
        // A workshop item's depot is not known beforehand, only once it reports.
        val weights = weights.ifEmpty { depotFractions.keys.associateWith { 1L } }
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
