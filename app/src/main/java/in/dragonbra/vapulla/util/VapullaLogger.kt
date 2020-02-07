package `in`.dragonbra.vapulla.util

import android.util.Log

/**
 * Anko is deprecated, this class mimics the logging capabilities of it
 * https://github.com/Kotlin/anko/blob/master/anko/library/static/commons/src/main/java/Logging.kt
 **/

interface VapullaLogger {

    val loggerTag: String
        get() = getTag(javaClass)
}

fun VapullaLogger.info(message: String) =Log.i(loggerTag, message)

fun VapullaLogger.warn(message: String) = Log.w(loggerTag, message)

fun VapullaLogger.debug(message: String) = Log.d(loggerTag, message)

private fun getTag(clazz: Class<*>): String {
    val tag = clazz.simpleName
    return if (tag.length <= 23) {
        tag
    } else {
        tag.substring(0, 23)
    }
}