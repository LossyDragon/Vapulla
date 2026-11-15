package `in`.dragonbra.vapulla.util

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val HTTP_TIMEOUT_MS = 10_000

/** For the web answers the app reads: fields it has no use for are skipped. */
val LenientJson = Json { ignoreUnknownKeys = true }

/**
 * The body of [url], fetched with a plain GET off the main thread.
 *
 * @throws IOException if the server cannot be reached or answers with an error status.
 */
suspend fun httpGet(url: String, headers: Map<String, String> = emptyMap()): ByteArray =
    withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = HTTP_TIMEOUT_MS
            connection.readTimeout = HTTP_TIMEOUT_MS
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("HTTP $status from ${connection.url.host}")
            }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

/** [httpGet] with the body read as UTF-8 text. */
suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): String =
    httpGet(url, headers).decodeToString()
