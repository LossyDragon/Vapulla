package `in`.dragonbra.vapulla.util

import timber.log.Timber

inline fun <reified T : Any> T.debug(message: String) =
    Timber.tag("Vapulla").d("[${this::class.java.simpleName}]  $message")

inline fun <reified T : Any> T.info(message: String) =
    Timber.tag("Vapulla").i("[${this::class.java.simpleName}]  $message")

inline fun <reified T : Any> T.warn(message: String) =
    Timber.tag("Vapulla").w("[${this::class.java.simpleName}]  $message")

// inline fun <reified T : Any> T.error(message: String) =
//      Timber.tag("Vapulla").e("[${this::class.java.simpleName}]  $message")
