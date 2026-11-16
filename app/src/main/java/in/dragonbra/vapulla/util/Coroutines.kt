package `in`.dragonbra.vapulla.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * A request got no answer from Steam in time. Unlike JavaSteam's `AsyncJobTimeoutException`
 * this is not a `CancellationException`, so it can be carried in a [Result] and thrown again
 * without ending the coroutine that does so.
 */
class SteamTimeoutException : Exception("Steam did not answer")

/**
 * [runCatching] for suspending code. A plain `runCatching` also catches the
 * `CancellationException` of a cancelled coroutine, which then carries on as if the call had
 * merely failed and may overwrite newer state. Here a cancellation of the caller is rethrown.
 *
 * JavaSteam cancels a job that timed out, with an `AsyncJobTimeoutException`, so awaiting it
 * throws a `CancellationException` although the caller is still active. That one, and any
 * other cancellation that is not the caller's own, is returned as a [SteamTimeoutException].
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    Result.failure(SteamTimeoutException())
} catch (e: Exception) {
    Result.failure(e)
}
