package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.vapulla.util.LenientJson
import `in`.dragonbra.vapulla.util.httpGetText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

    @Serializable
    private class ApiList(val interfaces: List<WebApiInterface> = emptyList())

    @Serializable
    private class Answer(@SerialName("apilist") val apiList: ApiList = ApiList())

    /**
     * Every interface, sorted by name.
     *
     * @throws java.io.IOException if the Web API cannot be reached.
     */
    suspend fun load(): List<WebApiInterface> =
        LenientJson.decodeFromString<Answer>(httpGetText(LIST_URL))
            .apiList.interfaces.sortedBy { it.name }
}
