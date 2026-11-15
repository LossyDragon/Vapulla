package `in`.dragonbra.vapulla.util

import android.system.Os
import android.system.OsConstants
import androidx.compose.runtime.Immutable
import java.io.File

/**
 * How busy one kind of thread was over the last sample.
 *
 * @property cores CPU time used per second of real time: 1.0 is one core kept fully busy.
 */
@Immutable
data class CpuGroup(val name: String, val threads: Int, val cores: Float)

/**
 * Measures how much CPU the app's own threads use, grouped by what they are for, by reading
 * `/proc/self/task`. That is the one place Android lets an app see CPU time per thread, and
 * only for its own process.
 *
 * Threads are told apart by name, which is as fine as this gets: decrypting, decompressing
 * and writing chunks all run on the coroutine workers, so they show up as one group.
 */
class CpuUsage {

    /** CPU ticks each thread had used at the last sample, by thread ID. */
    private var lastTicks: Map<Int, Long> = emptyMap()
    private var lastTime = 0L

    /**
     * The groups that used any CPU since the previous call, busiest first. The first call
     * only takes a starting point and returns nothing.
     */
    fun sample(): List<CpuGroup> {
        val now = System.nanoTime()
        val threads = readThreads()
        val seconds = (now - lastTime) / NANOS_PER_SECOND
        val previous = lastTicks

        lastTicks = threads.associate { it.id to it.ticks }
        val first = lastTime == 0L
        lastTime = now
        if (first || seconds <= 0) return emptyList()

        return threads
            .groupBy { group(it.name) }
            .map { (name, members) ->
                // A thread not seen before was started since the last sample.
                val used = members.sumOf { (it.ticks - (previous[it.id] ?: 0L)).coerceAtLeast(0) }
                CpuGroup(name, members.size, (used / TICKS_PER_SECOND / seconds).toFloat())
            }
            .filter { it.cores >= MIN_CORES }
            .sortedByDescending { it.cores }
    }

    private class ThreadTicks(val id: Int, val name: String, val ticks: Long)

    private fun readThreads(): List<ThreadTicks> =
        File("/proc/self/task").listFiles().orEmpty().mapNotNull { dir ->
            // A thread can end between listing it and reading it.
            runCatching {
                // "tid (name) state ...": the name may hold spaces, so split around the brackets.
                val stat = File(dir, "stat").readText()
                val name = stat.substringAfter('(').substringBeforeLast(')')
                val fields = stat.substringAfterLast(") ").split(' ')

                ThreadTicks(
                    id = dir.name.toInt(),
                    name = name,
                    ticks = fields[USER_TIME].toLong() + fields[SYSTEM_TIME].toLong(),
                )
            }.getOrNull()
        }

    /** The kernel keeps only the first 15 characters of a thread's name. */
    private fun group(thread: String): String = when {
        thread.startsWith("DefaultDispatch") -> "Coroutine workers"
        thread.startsWith("JavaSteam Depot") || thread.startsWith("OkHttp") -> "HTTP downloads"
        thread == "HeapTaskDaemon" || thread.startsWith("GC") -> "Garbage collector"
        thread.startsWith("Jit thread") -> "JIT compiler"
        thread == "RenderThread" || thread.startsWith("hwui") -> "Rendering"
        thread == "main" || thread == "agonbra.vapulla" -> "Main thread"
        else -> "Other"
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0

        /** Fields of `stat`, counted from the one after the name. */
        const val USER_TIME = 11
        const val SYSTEM_TIME = 12

        /** Below this a group is left out as idle. */
        const val MIN_CORES = 0.005f

        val TICKS_PER_SECOND = Os.sysconf(OsConstants._SC_CLK_TCK).toDouble()
    }
}
