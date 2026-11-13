package `in`.dragonbra.vapulla.util

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build

/**
 * How an earlier run of the app ended.
 *
 * @property abnormal true for the endings worth a second look: crashes, freezes, memory kills.
 * @property detail what the system noted about it, often empty.
 */
data class ProcessExit(
    val timestamp: Long,
    val reason: String,
    val abnormal: Boolean,
    val detail: String,
)

/**
 * Reads the system's record of how earlier runs of the app ended. A run that the system
 * killed, or that froze, leaves no trace of its own, so this is the only place it shows up.
 */
object ProcessExits {

    /** The last [max] exits, newest first. Empty before Android 11, which keeps no record. */
    fun recent(context: Context, max: Int): List<ProcessExit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()

        val exits = runCatching {
            context.getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(null, 0, max)
        }.getOrNull().orEmpty()

        return exits.map { exit ->
            ProcessExit(
                timestamp = exit.timestamp,
                reason = describe(exit.reason),
                abnormal = exit.reason in ABNORMAL,
                detail = exit.description.orEmpty(),
            )
        }
    }

    private fun describe(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "froze (ANR)"
        ApplicationExitInfo.REASON_CRASH -> "crashed"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "crashed in native code"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "killed for memory"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "killed for using too many resources"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "failed to start"
        ApplicationExitInfo.REASON_SIGNALED -> "killed by a signal"
        ApplicationExitInfo.REASON_EXIT_SELF -> "exited by itself"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "stopped by the user"
        ApplicationExitInfo.REASON_USER_STOPPED -> "stopped with its user"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "killed by a permission change"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "killed with a process it depended on"
        ApplicationExitInfo.REASON_FREEZER -> "killed while frozen in the background"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "stopped by a change to the app's state"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "stopped to install an update"
        ApplicationExitInfo.REASON_OTHER -> "killed by the system"
        else -> "unknown reason $reason"
    }

    private val ABNORMAL = setOf(
        ApplicationExitInfo.REASON_ANR,
        ApplicationExitInfo.REASON_CRASH,
        ApplicationExitInfo.REASON_CRASH_NATIVE,
        ApplicationExitInfo.REASON_LOW_MEMORY,
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
    )
}
