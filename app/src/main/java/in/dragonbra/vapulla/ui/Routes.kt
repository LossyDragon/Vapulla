package `in`.dragonbra.vapulla.ui

import java.io.Serializable

/**
 * Navigation 3 destinations: an object for a screen that needs nothing, a data class for one
 * that is opened on something, such as a friend or an app. They are [Serializable] so the
 * back stack can be saved as it is. Each object has a `readResolve`, so a restored back stack
 * holds the object itself and not a second instance of it.
 */
sealed class Routes : Serializable {
    data object Downloads : Routes() {
        private fun readResolve(): Any = Downloads
    }

    data object Games : Routes() {
        private fun readResolve(): Any = Games
    }

    data object Home : Routes() {
        private fun readResolve(): Any = Home
    }

    data object Login : Routes() {
        private fun readResolve(): Any = Login
    }

    data object Settings : Routes() {
        private fun readResolve(): Any = Settings
    }

    data object ChatRooms : Routes() {
        private fun readResolve(): Any = ChatRooms
    }

    data object ServerBrowser : Routes() {
        private fun readResolve(): Any = ServerBrowser
    }

    data object Sessions : Routes() {
        private fun readResolve(): Any = Sessions
    }

    data object RemoteClients : Routes() {
        private fun readResolve(): Any = RemoteClients
    }

    data object Account : Routes() {
        private fun readResolve(): Any = Account
    }

    data object Family : Routes() {
        private fun readResolve(): Any = Family
    }

    data object Store : Routes() {
        private fun readResolve(): Any = Store
    }

    data object Screenshots : Routes() {
        private fun readResolve(): Any = Screenshots
    }

    data object NetworkLog : Routes() {
        private fun readResolve(): Any = NetworkLog
    }

    data object UnderTheHood : Routes() {
        private fun readResolve(): Any = UnderTheHood
    }

    data object ConnectionInfo : Routes() {
        private fun readResolve(): Any = ConnectionInfo
    }

    data object CmServers : Routes() {
        private fun readResolve(): Any = CmServers
    }

    data object Licenses : Routes() {
        private fun readResolve(): Any = Licenses
    }

    data object IdDecoder : Routes() {
        private fun readResolve(): Any = IdDecoder
    }

    data object AuthTicket : Routes() {
        private fun readResolve(): Any = AuthTicket
    }

    data object Diagnostics : Routes() {
        private fun readResolve(): Any = Diagnostics
    }

    data object PicsFeed : Routes() {
        private fun readResolve(): Any = PicsFeed
    }

    data object WebApi : Routes() {
        private fun readResolve(): Any = WebApi
    }

    data object DownloadInternals : Routes() {
        private fun readResolve(): Any = DownloadInternals
    }

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
