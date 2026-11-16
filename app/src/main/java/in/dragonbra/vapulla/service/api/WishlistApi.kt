package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.webui.ServiceWishlist.CWishlist_AddToWishlist_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceWishlist.CWishlist_GetWishlist_Request
import `in`.dragonbra.javasteam.protobufs.webui.ServiceWishlist.CWishlist_RemoveFromWishlist_Request
import `in`.dragonbra.javasteam.rpc.service.webui.Wishlist
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.service.SessionComponent
import java.io.Closeable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One app on the wishlist.
 *
 * @property priority its rank, 1 for the most wanted; 0 when it was never ranked.
 * @property dateAdded seconds since 1970.
 */
@Immutable
data class WishlistEntry(val appId: Int, val priority: Int, val dateAdded: Long)

/**
 * The account's wishlist, most wanted first, with the apps never ranked after the ranked
 * ones, newest first. Held in memory only: it is read from Steam at every sign-in.
 */
object SteamWishlist {

    private val _entries = MutableStateFlow<ImmutableList<WishlistEntry>?>(null)

    /** Null until Steam has sent the list, and while signed out. */
    val entries: StateFlow<ImmutableList<WishlistEntry>?> = _entries.asStateFlow()

    internal fun set(entries: List<WishlistEntry>?) {
        _entries.value = entries?.sorted()
    }

    /** Changes the list, if there is one. */
    internal fun update(change: (List<WishlistEntry>) -> List<WishlistEntry>) {
        _entries.update { current -> current?.let(change)?.sorted() }
    }

    private fun List<WishlistEntry>.sorted(): ImmutableList<WishlistEntry> =
        sortedWith(
            compareBy<WishlistEntry> { it.priority == 0 }
                .thenBy { it.priority }
                .thenByDescending { it.dateAdded },
        ).toImmutableList()
}

/**
 * The wishlist via the `Wishlist` service, which comes from JavaSteam's
 * `javasteam-protobufs-webui` artifact. It is read at sign-in and kept in [SteamWishlist];
 * adding and removing an app change the copy there once Steam has agreed.
 */
class WishlistApi(private val wishlist: Wishlist, private val localSteamId: () -> SteamID?) :
    SessionComponent {

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = emptyList()

    override fun start(session: CoroutineScope) {
        session.launch { refresh() }
    }

    /**
     * Reads the list again.
     *
     * @return false when Steam does not answer; the list is then left as it was.
     */
    suspend fun refresh(): Boolean {
        val steamId = localSteamId() ?: return false
        val request = CWishlist_GetWishlist_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .build()

        val body = wishlist.getWishlist(request).bodyOrNull("Wishlist.GetWishlist") ?: return false

        SteamWishlist.set(
            body.itemsList.map {
                WishlistEntry(
                    appId = it.appid,
                    priority = it.priority,
                    dateAdded = it.dateAdded.toUInt().toLong(),
                )
            },
        )
        return true
    }

    /** Puts [appId] on the wishlist, unranked. The list changes only on `OK`. */
    suspend fun add(appId: Int): EResult {
        val request = CWishlist_AddToWishlist_Request.newBuilder().setAppid(appId).build()
        val result = wishlist.addToWishlist(request).resultOf("Wishlist.AddToWishlist")

        if (result == EResult.OK) {
            SteamWishlist.update { entries ->
                if (entries.any { it.appId == appId }) {
                    entries
                } else {
                    entries + WishlistEntry(appId, priority = 0, dateAdded = steamTimeNow().toLong())
                }
            }
        }
        return result
    }

    /** Takes [appId] off the wishlist. The list changes only on `OK`. */
    suspend fun remove(appId: Int): EResult {
        val request = CWishlist_RemoveFromWishlist_Request.newBuilder().setAppid(appId).build()
        val result = wishlist.removeFromWishlist(request).resultOf("Wishlist.RemoveFromWishlist")

        if (result == EResult.OK) {
            SteamWishlist.update { entries -> entries.filterNot { it.appId == appId } }
        }
        return result
    }
}
