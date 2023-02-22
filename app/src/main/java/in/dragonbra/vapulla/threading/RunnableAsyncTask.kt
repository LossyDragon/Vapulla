package `in`.dragonbra.vapulla.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Async replacement using coroutines
 */

fun <R> CoroutineScope.executeAsyncTask(task: () -> R) {
    executeAsyncTask(doInBackground = { task() })
}

fun <R> CoroutineScope.executeAsyncTask(
    onPreExecute: (() -> Unit?)? = null,
    onPostExecute: ((R) -> Unit?)? = null,
    doInBackground: () -> R
) = launch {
    onPreExecute?.invoke()
    val result = withContext(Dispatchers.IO) { doInBackground() }
    onPostExecute?.invoke(result)
}
