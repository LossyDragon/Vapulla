package `in`.dragonbra.vapulla.util

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

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