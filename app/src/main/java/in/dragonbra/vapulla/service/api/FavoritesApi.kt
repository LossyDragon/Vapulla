package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.webui.ServiceFriendslist.CFriendsListFavoriteEntry
import `in`.dragonbra.javasteam.protobufs.webui.ServiceFriendslist.CFriendsList_FavoritesChanged_Notification
import `in`.dragonbra.javasteam.protobufs.webui.ServiceFriendslist.CFriendsList_GetFavorites_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceFriendslist.CFriendsList_SetFavorites_Request
import `in`.dragonbra.javasteam.rpc.service.webui.FriendsList
import `in`.dragonbra.javasteam.rpc.service.webui.FriendsListClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.util.helpers.accountIdToSteamID
import `in`.dragonbra.vapulla.util.helpers.toLong
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import java.io.Closeable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The account's favourite friends, the ones Steam's own friends list keeps at its top, in
 * the order Steam keeps them. Held in memory only: Steam sends the list at every sign-in.
 */
object SteamFavorites {

    private val _friendIds = MutableStateFlow<ImmutableList<Long>>(persistentListOf())

    /** 64-bit Steam IDs of the favourite friends; empty while signed out. */
    val friendIds: StateFlow<ImmutableList<Long>> = _friendIds.asStateFlow()

    internal fun set(friendIds: List<Long>) {
        _friendIds.value = friendIds.toImmutableList()
    }
}

/**
 * Favourites via the `FriendsList` service, which comes from JavaSteam's
 * `javasteam-protobufs-webui` artifact: the list is read at sign-in, kept up to date from
 * the `FriendsListClient.FavoritesChanged` notification, which Steam sends when a favourite
 * is changed on any device, and changed with `SetFavorites`.
 */
class FavoritesApi(private val friendsList: FriendsList) : SessionComponent {

    /**
     * The list as Steam sent it. A favourite can be a group or a group chat as well as a
     * friend; those are not shown, but are kept so that saving the list does not drop them.
     */
    @Volatile
    private var entries: List<CFriendsListFavoriteEntry> = emptyList()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            FriendsListClient,
            CFriendsList_FavoritesChanged_Notification.Builder,
            > { notification ->
            Timber.d("Favourites changed on Steam")
            apply(notification.body.favoritesList)
        },
    )

    override fun start(session: CoroutineScope) {
        session.launch {
            val request = CFriendsList_GetFavorites_Request.newBuilder().build()
            friendsList.getFavorites(request).bodyOrNull("GetFavorites")
                ?.let { apply(it.favoritesList) }
        }
    }

    /**
     * Makes [friendId] a favourite, or no longer one.
     *
     * @return Steam's answer; `Timeout` when there is none. The list changes only on `OK`.
     */
    suspend fun setFavorite(friendId: Long, favorite: Boolean): EResult {
        val accountId = friendId.toSteamID().accountID.toInt()
        val others = entries.filterNot { it.accountid == accountId }
        val changed = if (favorite) {
            others + CFriendsListFavoriteEntry.newBuilder().setAccountid(accountId).build()
        } else {
            others
        }

        val request = CFriendsList_SetFavorites_Request.newBuilder().addAllFavorites(
            changed,
        ).build()
        val result = friendsList.setFavorites(request).resultOf("SetFavorites")
        if (result == EResult.OK) apply(changed)

        return result
    }

    private fun apply(favorites: List<CFriendsListFavoriteEntry>) {
        entries = favorites
        SteamFavorites.set(
            // The friends come as account IDs, the low half of a Steam ID.
            favorites.filter {
                it.accountid != 0
            }.map { it.accountid.accountIdToSteamID().toLong() },
        )
    }
}
