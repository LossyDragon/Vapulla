package `in`.dragonbra.vapulla.threading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <R> CoroutineScope.executeAsyncTask(
    onPreExecute: (() -> Unit?)? = null,
    doInBackground: () -> R,
    onPostExecute: ((R) -> Unit?)? = null
) = launch {
    onPreExecute?.invoke()
    val result = withContext(Dispatchers.IO) { doInBackground() }
    onPostExecute?.invoke(result)
}
