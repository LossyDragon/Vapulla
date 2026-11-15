package `in`.dragonbra.vapulla.util

import android.util.Log
import timber.log.Timber

/**
 * The log tree of a release build: it leaves out verbose and debug lines, which a debug
 * build's [Timber.DebugTree] keeps.
 */
class ReleaseTree : Timber.Tree() {

    /** Timber asks this before every line, so [log] only ever sees info and above. */
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

    /** Timber has already added the stack trace of [t] to [message]. */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val priorityChar = when (priority) {
            Log.INFO -> 'I'
            Log.WARN -> 'W'
            Log.ERROR -> 'E'
            else -> 'A'
        }

        Log.println(priority, tag, "$priorityChar: $message")
    }
}
