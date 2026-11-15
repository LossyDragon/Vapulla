package `in`.dragonbra.vapulla.service.download

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import java.util.Locale
import timber.log.Timber

/**
 * Logs what a running download costs the phone, so that two builds can be compared: speed,
 * CPU cores in use, memory allocated and time lost to the garbage collector. It writes a
 * line now and then and a summary at the end, all under the logcat tag `DownloadPerf`.
 *
 * The CPU and memory figures are for the whole process, so compare runs made the same way:
 * same game, same settings, the same screen open.
 */
class DownloadPerf(private val context: Context, private val label: String, settings: String) {

    /** The counters at one moment. All of them only ever go up. */
    private class Reading(
        val wallMillis: Long,
        val cpuMillis: Long,
        val allocatedBytes: Long,
        val gcCount: Long,
        val gcMillis: Long,
        val blockingGcCount: Long,
        val blockingGcMillis: Long,
        val networkBytes: Long,
        val writtenBytes: Long,
    )

    private val runtime = Runtime.getRuntime()
    private val start = read(null)
    private var last = start
    private var peakHeapBytes = 0L

    init {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        log(
            "start $label | debuggable $debuggable | ${runtime.availableProcessors()} cores | " +
                "heap limit ${runtime.maxMemory().megabytes()} | $settings",
        )
    }

    /** Logs the figures since the last call. */
    fun sample(progress: DownloadProgress) {
        val now = read(progress)
        val heap = runtime.totalMemory() - runtime.freeMemory()
        peakHeapBytes = maxOf(peakHeapBytes, heap)

        log("${figures(last, now)} | heap ${heap.megabytes()} | thermal ${thermalStatus()}")
        last = now
    }

    /** Logs the figures for the whole run. */
    fun finish(progress: DownloadProgress, outcome: String) {
        val now = read(progress)
        log(
            "summary $label | $outcome | ${figures(start, now)} | " +
                "heap peak ${peakHeapBytes.megabytes()} of ${runtime.maxMemory().megabytes()}",
        )
    }

    private fun read(progress: DownloadProgress?) = Reading(
        wallMillis = SystemClock.elapsedRealtime(),
        cpuMillis = Process.getElapsedCpuTime(),
        allocatedBytes = stat("art.gc.bytes-allocated"),
        gcCount = stat("art.gc.gc-count"),
        gcMillis = stat("art.gc.gc-time"),
        blockingGcCount = stat("art.gc.blocking-gc-count"),
        blockingGcMillis = stat("art.gc.blocking-gc-time"),
        networkBytes = progress?.networkBytes ?: 0L,
        writtenBytes = progress?.writtenBytes ?: 0L,
    )

    private fun stat(name: String): Long = Debug.getRuntimeStat(name)?.toLongOrNull() ?: 0L

    /**
     * What happened between two readings. "blocking" is the collections that threads had to
     * stop and wait for, which is the time a download really loses.
     */
    private fun figures(from: Reading, to: Reading): String {
        val seconds = (to.wallMillis - from.wallMillis).coerceAtLeast(1) / 1000.0
        val network = to.networkBytes - from.networkBytes
        val written = to.writtenBytes - from.writtenBytes
        val allocated = to.allocatedBytes - from.allocatedBytes
        val perByte = if (network > 0) allocated.toDouble() / network else 0.0

        return String.format(
            Locale.US,
            "%.1f s | net %s (%s/s) | disk %s (%s/s) | cpu %.2f cores | " +
                "alloc %s (%.1f per net byte) | gc %d in %d ms | blocking gc %d in %d ms",
            seconds,
            network.megabytes(),
            (network / seconds).toLong().megabytes(),
            written.megabytes(),
            (written / seconds).toLong().megabytes(),
            (to.cpuMillis - from.cpuMillis) / 1000.0 / seconds,
            allocated.megabytes(),
            perByte,
            to.gcCount - from.gcCount,
            to.gcMillis - from.gcMillis,
            to.blockingGcCount - from.blockingGcCount,
            to.blockingGcMillis - from.blockingGcMillis,
        )
    }

    /** 0 is cool; from 3 (severe) up the phone slows its CPU down, and a run is not a fair one. */
    private fun thermalStatus(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.getSystemService(PowerManager::class.java).currentThermalStatus.toString()
    } else {
        "unknown"
    }

    private fun Long.megabytes(): String = String.format(Locale.US, "%.1f MB", this / MEGABYTE)

    private fun log(message: String) = Timber.tag(TAG).i(message)

    private companion object {
        const val TAG = "DownloadPerf"
        const val MEGABYTE = 1024.0 * 1024.0
    }
}
