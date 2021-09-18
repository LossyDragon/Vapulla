package `in`.dragonbra.vapulla.util

import android.util.Log

inline fun <reified T : Any> T.debug(message: String) =
    Log.d("Vapulla", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.info(message: String) =
    Log.i("Vapulla", "[${this::class.java.simpleName}] $message")

inline fun <reified T : Any> T.warn(message: String) =
    Log.w("Vapulla", "[${this::class.java.simpleName}] $message")

// inline fun <reified T : Any> T.error(message: String) =
//     Log.e("Vapulla", "[${this::class.java.simpleName}] $message")
