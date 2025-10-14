package `in`.dragonbra.vapulla.util

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import java.io.Serializable
import androidx.core.net.toUri

inline fun <reified T : View> View.find(id: Int): T {
    return findViewById(id)
}

inline fun <reified T : View> Activity.find(id: Int): T {
    return findViewById(id)
}

inline fun <reified T : View> Fragment.find(id: Int): T {
    return requireView().findViewById(id)
}

interface AnkoLogger {
    val loggerTag: String
        get() = this::class.java.simpleName
}

fun AnkoLogger.verbose(message: Any?) {
    android.util.Log.v(loggerTag, message.toString())
}

fun AnkoLogger.verbose(message: Any?, thr: Throwable) {
    android.util.Log.v(loggerTag, message.toString(), thr)
}

fun AnkoLogger.debug(message: Any?) {
    android.util.Log.d(loggerTag, message.toString())
}

fun AnkoLogger.debug(message: Any?, thr: Throwable) {
    android.util.Log.d(loggerTag, message.toString(), thr)
}

fun AnkoLogger.info(message: Any?) {
    android.util.Log.i(loggerTag, message.toString())
}

fun AnkoLogger.info(message: Any?, thr: Throwable) {
    android.util.Log.i(loggerTag, message.toString(), thr)
}

fun AnkoLogger.warn(message: Any?) {
    android.util.Log.w(loggerTag, message.toString())
}

fun AnkoLogger.warn(message: Any?, thr: Throwable) {
    android.util.Log.w(loggerTag, message.toString(), thr)
}

fun AnkoLogger.error(message: Any?) {
    android.util.Log.e(loggerTag, message.toString())
}

fun AnkoLogger.error(message: Any?, thr: Throwable) {
    android.util.Log.e(loggerTag, message.toString(), thr)
}

fun AnkoLogger.wtf(message: Any?) {
    android.util.Log.wtf(loggerTag, message.toString())
}

fun AnkoLogger.wtf(message: Any?, thr: Throwable) {
    android.util.Log.wtf(loggerTag, message.toString(), thr)
}

inline fun <reified T : Any> Context.intentFor(vararg params: Pair<String, Any?>): Intent {
    val intent = Intent(this, T::class.java)
    if (params.isNotEmpty()) {
        fillIntentExtras(intent, params)
    }
    return intent
}


// Helper function to avoid code duplication
fun fillIntentExtras(intent: Intent, params: Array<out Pair<String, Any?>>) {
    params.forEach { (key, value) ->
        when (value) {
            null -> intent.putExtra(key, null as String?)
            is Int -> intent.putExtra(key, value)
            is Long -> intent.putExtra(key, value)
            is CharSequence -> intent.putExtra(key, value)
            is String -> intent.putExtra(key, value)
            is Float -> intent.putExtra(key, value)
            is Double -> intent.putExtra(key, value)
            is Char -> intent.putExtra(key, value)
            is Short -> intent.putExtra(key, value)
            is Boolean -> intent.putExtra(key, value)
            is Serializable -> intent.putExtra(key, value)
            is Bundle -> intent.putExtra(key, value)
            is Parcelable -> intent.putExtra(key, value)
            is Array<*> -> when {
                value.isArrayOf<CharSequence>() -> intent.putExtra(key, value as Array<CharSequence>)
                value.isArrayOf<String>() -> intent.putExtra(key, value as Array<String>)
                value.isArrayOf<Parcelable>() -> intent.putExtra(key, value as Array<Parcelable>)
                else -> throw IllegalArgumentException("Intent extra $key has wrong type ${value.javaClass.name}")
            }
            is IntArray -> intent.putExtra(key, value)
            is LongArray -> intent.putExtra(key, value)
            is FloatArray -> intent.putExtra(key, value)
            is DoubleArray -> intent.putExtra(key, value)
            is CharArray -> intent.putExtra(key, value)
            is ShortArray -> intent.putExtra(key, value)
            is BooleanArray -> intent.putExtra(key, value)
            else -> throw IllegalArgumentException("Intent extra $key has wrong type ${value.javaClass.name}")
        }
    }
}

inline fun <reified T : Service> Context.startService(vararg params: Pair<String, Any?>): ComponentName? {
    return startService(intentFor<T>(*params))
}

fun Context.browse(url: String, newTask: Boolean = false): Boolean {
    return try {
        val webpage = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, webpage).apply {
            // Add FLAG_ACTIVITY_NEW_TASK to avoid the warning when called from non-Activity context
            if (newTask || this@browse !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        // Check if there's an app that can handle this intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Intent.newTask(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return this
}

fun Intent.clearTask(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return this
}

fun Intent.clearTop(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    return this
}

fun Intent.excludeFromRecents(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    return this
}

fun Intent.multipleTask(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    return this
}

fun Intent.singleTop(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    return this
}

fun Intent.noAnimation(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    return this
}

fun Intent.noHistory(): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    return this
}

inline fun <reified T : Any> Context.startActivity(vararg params: Pair<String, Any?>) {
    startActivity(intentFor<T>(*params))
}