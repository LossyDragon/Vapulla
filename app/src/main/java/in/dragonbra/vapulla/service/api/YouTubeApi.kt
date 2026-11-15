package `in`.dragonbra.vapulla.service.api

import android.util.LruCache
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber

/** A video's name and the channel it is on. */
@Serializable
data class YouTubeVideo(val title: String = "", @SerialName("author_name") val channel: String = "")

/**
 * What YouTube tells anyone about a video, from its oEmbed address, which needs no key.
 * Answers are kept while the app runs, so a chat scrolled back and forth asks once per video.
 */
object YouTubeApi {

    private const val OEMBED_URL = "https://www.youtube.com/oembed?format=json&url="
    private const val WATCH_URL = "https://www.youtube.com/watch?v="
    private const val TIMEOUT_MS = 10_000
    private const val CACHE_SIZE = 128

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = LruCache<String, YouTubeVideo>(CACHE_SIZE)

    /** The answer for [videoId] if it was asked for before. */
    fun cached(videoId: String): YouTubeVideo? = cache[videoId]

    /**
     * Asks YouTube about [videoId]. Null when it will not say (a private or removed video) or
     * cannot be reached; that is not kept, so the next showing of the video asks again.
     */
    suspend fun load(videoId: String): YouTubeVideo? = withContext(Dispatchers.IO) {
        val address = OEMBED_URL + URLEncoder.encode(WATCH_URL + videoId, "UTF-8")
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        try {
            val text = connection.inputStream.use { it.readBytes().decodeToString() }
            json.decodeFromString<YouTubeVideo>(text).also { cache.put(videoId, it) }
        } catch (e: IOException) {
            Timber.d("No YouTube details for $videoId: ${e.message}")
            null
        } catch (e: SerializationException) {
            Timber.d("Unreadable YouTube details for $videoId: ${e.message}")
            null
        } finally {
            connection.disconnect()
        }
    }
}
