package `in`.dragonbra.vapulla.ui

import java.io.Serializable

/**
 * Navigation 3 destinations: an object for a screen that needs nothing, a data class for one
 * that is opened on something, such as a friend or an app. They are [Serializable] so the
 * back stack can be saved as it is.
 */
sealed class Routes : Serializable {
    data object Downloads : Routes()
    data object Games : Routes()
    data object Home : Routes()
    data object Login : Routes()
    data object Settings : Routes()
    data object ChatRooms : Routes()
    data object ServerBrowser : Routes()
    data object Sessions : Routes()
    data object Account : Routes()
    data object Family : Routes()
    data object Store : Routes()
    data object Screenshots : Routes()
    data object NetworkLog : Routes()
    data object UnderTheHood : Routes()
    data object ConnectionInfo : Routes()
    data object CmServers : Routes()
    data object Licenses : Routes()
    data object IdDecoder : Routes()
    data object AuthTicket : Routes()
    data object Diagnostics : Routes()
    data object PicsFeed : Routes()
    data object WebApi : Routes()
    data object DownloadInternals : Routes()

    /** The chat with a friend; [id] is the friend's 64-bit Steam ID. */
    data class FriendChat(val id: Long) : Routes()

    /** The profile of a friend; [id] is the friend's 64-bit Steam ID. */
    data class FriendProfile(val id: Long) : Routes()
    data class GameDetail(val appId: Int) : Routes()
    data class CloudFiles(val appId: Int) : Routes()
    data class Workshop(val appId: Int) : Routes()
    data class Achievements(val appId: Int) : Routes()
    data class Leaderboards(val appId: Int) : Routes()

    /** One of Steam's web pages inside the app, signed in; see the setting for it. */
    data class WebPage(val url: String) : Routes()

    /** The store page of any app, owned or not. */
    data class StorePage(val appId: Int) : Routes()

    /** The depot browser; [appId] 0 opens it on its search box. */
    data class Depots(val appId: Int = 0) : Routes()

    /** The PICS explorer; [id] 0 opens it on its search box. */
    data class PicsExplorer(val id: Int = 0, val isApp: Boolean = true) : Routes()

    /** The files of one manifest of a depot. */
    data class Manifest(
        val appId: Int,
        val depotId: Int,
        val manifestId: Long,
        val branch: String,
    ) : Routes()

    /** One channel of a group chat, with the names of the group and the channel. */
    data class GroupChat(
        val groupId: Long,
        val chatId: Long,
        val groupName: String,
        val roomName: String,
    ) : Routes()
}
