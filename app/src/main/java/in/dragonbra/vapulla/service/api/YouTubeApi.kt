package `in`.dragonbra.vapulla.service.api

import android.util.LruCache
import `in`.dragonbra.vapulla.util.LenientJson
import `in`.dragonbra.vapulla.util.httpGetText
import java.io.IOException
import java.net.URLEncoder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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
    private const val CACHE_SIZE = 128

    private val cache = LruCache<String, YouTubeVideo>(CACHE_SIZE)

    /** The answer for [videoId] if it was asked for before. */
    fun cached(videoId: String): YouTubeVideo? = cache[videoId]

    /**
     * Asks YouTube about [videoId]. Null when it will not say (a private or removed video) or
     * cannot be reached; that is not kept, so the next showing of the video asks again.
     */
    suspend fun load(videoId: String): YouTubeVideo? = try {
        val text = httpGetText(OEMBED_URL + URLEncoder.encode(WATCH_URL + videoId, "UTF-8"))
        LenientJson.decodeFromString<YouTubeVideo>(text).also { cache.put(videoId, it) }
    } catch (e: IOException) {
        Timber.d("No YouTube details for $videoId: ${e.message}")
        null
    } catch (e: SerializationException) {
        Timber.d("Unreadable YouTube details for $videoId: ${e.message}")
        null
    }
}
