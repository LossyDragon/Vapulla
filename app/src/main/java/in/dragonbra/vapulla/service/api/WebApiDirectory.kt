package `in`.dragonbra.vapulla.service.api

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One parameter of a Web API method. */
@Serializable
data class WebApiParameter(
    val name: String,
    val type: String = "",
    val optional: Boolean = false,
    val description: String = "",
)

/** One method of a Web API interface, e.g. `GetNewsForApp` version 2. */
@Serializable
data class WebApiMethod(
    val name: String,
    val version: Int = 1,
    @SerialName("httpmethod") val httpMethod: String = "GET",
    val parameters: List<WebApiParameter> = emptyList(),
)

/** A Web API interface such as `ISteamNews`, with its methods. */
@Serializable
data class WebApiInterface(val name: String, val methods: List<WebApiMethod> = emptyList())

/**
 * Steam's own list of what its Web API (`api.steampowered.com`) offers, from
 * `ISteamWebAPIUtil/GetSupportedAPIList`. Asked without a key, as here, it lists the methods
 * anyone may call; a publisher key would add the restricted ones.
 */
object WebApiDirectory {

    private const val LIST_URL =
        "https://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v1/"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class ApiList(val interfaces: List<WebApiInterface> = emptyList())

    @Serializable
    private class Answer(@SerialName("apilist") val apiList: ApiList = ApiList())

    /**
     * Every interface, sorted by name.
     *
     * @throws java.io.IOException if the Web API cannot be reached.
     */
    suspend fun load(): List<WebApiInterface> = withContext(Dispatchers.IO) {
        val connection = URL(LIST_URL).openConnection() as HttpURLConnection
        val text = try {
            connection.inputStream.use { it.readBytes().decodeToString() }
        } finally {
            connection.disconnect()
        }

        json.decodeFromString<Answer>(text).apiList.interfaces.sortedBy { it.name }
    }
}
