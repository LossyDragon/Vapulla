package `in`.dragonbra.vapulla.service.api

import com.google.protobuf.GeneratedMessage
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodResponse
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.types.AsyncJobMultiple
import `in`.dragonbra.javasteam.types.AsyncJobSingle
import `in`.dragonbra.vapulla.util.SteamTimeoutException
import `in`.dragonbra.vapulla.util.runSuspendCatching
import timber.log.Timber

/*
 * How the feature APIs report a call to Steam that did not work out. There is one rule:
 *
 * - A call that reads returns null or an empty list when Steam refuses, fails or does not
 *   answer, and logs why: [bodyOrNull], [awaitOrNull].
 * - A call that changes something returns Steam's EResult: [resultOf].
 * - A call whose failure a screen shows in words throws, and says so with `@throws`:
 *   [bodyOrThrow], [awaitOrThrow].
 *
 * All of them wait through `runSuspendCatching`, because JavaSteam cancels a job that timed
 * out: awaited directly, it would end the calling coroutine as if that had been cancelled.
 */

/**
 * Waits for a handler's callback.
 *
 * @throws SteamTimeoutException if Steam does not answer.
 */
suspend fun <T : CallbackMsg> AsyncJobSingle<T>.awaitOrThrow(): T =
    runSuspendCatching { await() }.getOrThrow()

/** [awaitOrThrow] for a job that Steam answers in several parts, such as PICS product info. */
suspend fun <T : CallbackMsg> AsyncJobMultiple<T>.awaitOrThrow(): AsyncJobMultiple.ResultSet<T> =
    runSuspendCatching { await() }.getOrThrow()

/** Waits for a handler's callback; null, with [what] logged, when Steam does not answer. */
suspend fun <T : CallbackMsg> AsyncJobSingle<T>.awaitOrNull(what: String): T? =
    runSuspendCatching { await() }
        .onFailure { Timber.w(it, "$what failed") }
        .getOrNull()

/** The body of a unified service answer; null, with [method] logged, unless it is `OK`. */
suspend fun <T : GeneratedMessage.Builder<T>> AsyncJobSingle<ServiceMethodResponse<T>>.bodyOrNull(
    method: String,
): T? {
    val response = awaitOrNull(method) ?: return null
    if (response.result != EResult.OK) {
        Timber.w("$method answered ${response.result}")
        return null
    }
    return response.body
}

/**
 * The body of a unified service answer.
 *
 * @throws IllegalStateException naming Steam's result when it is not `OK`.
 * @throws SteamTimeoutException if Steam does not answer.
 */
suspend fun <T : GeneratedMessage.Builder<T>> AsyncJobSingle<ServiceMethodResponse<T>>.bodyOrThrow(
    method: String,
): T {
    val response = awaitOrThrow()
    check(response.result == EResult.OK) { "$method answered ${response.result}" }
    return response.body
}

/**
 * The result of a unified service call that changes something. `Timeout` when Steam does not
 * answer and `RemoteCallFailed` when the call could not be made.
 */
suspend fun <T : GeneratedMessage.Builder<T>> AsyncJobSingle<ServiceMethodResponse<T>>.resultOf(
    method: String,
): EResult = runSuspendCatching { await().result }
    .onFailure { Timber.w(it, "$method failed") }
    .getOrElse { if (it is SteamTimeoutException) EResult.Timeout else EResult.RemoteCallFailed }
