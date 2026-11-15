package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * One news post of a game.
 *
 * @property summary the start of the post as plain text, cut short by Steam.
 * @property source where it was posted, e.g. "Community Announcements" or a news site.
 * @property date when it was posted, in seconds since 1970.
 */
@Immutable
data class NewsItem(
    val id: String,
    val title: String,
    val url: String,
    val summary: String,
    val source: String,
    val date: Long,
)

/**
 * A game's latest news, from the Web API's `ISteamNews/GetNewsForApp`. It needs no key and
 * no Steam session, so it is a plain web request like [WebApiDirectory].
 */
object SteamNews {

    private const val NEWS_URL = "https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/"

    /** Steam cuts each post down to about this many characters and drops its formatting. */
    private const val SUMMARY_LENGTH = 300
    private const val TIMEOUT_MS = 10_000

    private val json = Json { ignoreUnknownKeys = true }

    /** What is left of a link or other markup in a summary. */
    private val HTML_TAG = Regex("<[^>]+>")

    @Serializable
    private class Post(
        val gid: String = "",
        val title: String = "",
        val url: String = "",
        val contents: String = "",
        @SerialName("feedlabel") val feedLabel: String = "",
        val date: Long = 0,
    )

    @Serializable
    private class AppNews(@SerialName("newsitems") val posts: List<Post> = emptyList())

    @Serializable
    private class Answer(@SerialName("appnews") val appNews: AppNews = AppNews())

    /**
     * The [count] newest posts of [appId], newest first.
     *
     * @throws java.io.IOException if the Web API cannot be reached.
     */
    suspend fun load(appId: Int, count: Int = 5): List<NewsItem> = withContext(Dispatchers.IO) {
        val address = "$NEWS_URL?appid=$appId&count=$count&maxlength=$SUMMARY_LENGTH&format=json"
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS

        val text = try {
            connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }

        json.decodeFromString<Answer>(text).appNews.posts.map { post ->
            NewsItem(
                id = post.gid,
                title = post.title.decodeHtml(),
                url = post.url,
                summary = post.contents.replace(HTML_TAG, "").decodeHtml().trim(),
                source = post.feedLabel,
                date = post.date,
            )
        }
    }
}
