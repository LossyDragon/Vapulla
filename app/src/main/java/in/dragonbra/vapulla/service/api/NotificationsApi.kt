package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.CommentNotificationsCallback
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.ItemAnnouncementsCallback
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.service.SteamAccount
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber

/**
 * How much is waiting for the account on Steam. Each count arrives in a message of its own
 * and is null until that message has come.
 *
 * @property comments new comments, of every kind.
 * @property commentsOnOwnContent those on what the account owns, such as its profile.
 * @property commentsOnSubscriptions those on discussions the account follows.
 * @property newItems items added to the inventory and not looked at yet.
 * @property offlineMessages chat messages sent while the account was offline.
 * @property offlineMessageFriends how many friends those messages are from.
 */
@Immutable
data class NotificationCounts(
    val comments: Int? = null,
    val commentsOnOwnContent: Int = 0,
    val commentsOnSubscriptions: Int = 0,
    val newItems: Int? = null,
    val offlineMessages: Int? = null,
    val offlineMessageFriends: Int = 0,
) {
    /** True once Steam has sent at least one of the counts. */
    val hasAny: Boolean get() = comments != null || newItems != null || offlineMessages != null
}

/**
 * Reads [NotificationCounts] via JavaSteam's `SteamNotifications`
 * handler: new comments, new inventory items and messages sent while it was offline. They
 * are asked for at sign-in and with [refresh], and Steam may send one again when it
 * changes. They are kept in [SteamAccount] for the Account screen.
 *
 * The offline messages themselves are fetched by [ChatApi], not from here.
 */
class NotificationsApi(private val notifications: SteamNotifications) : SessionComponent {

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribe(onComments),
        callbacks.subscribe(onItems),
        callbacks.subscribe(onOfflineMessages),
    )

    override fun start(session: CoroutineScope) = refresh()

    /** Asks Steam for all three counts; each answer arrives in a callback of its own. */
    fun refresh() {
        notifications.requestCommentNotifications()
        notifications.requestItemAnnouncements()
        notifications.requestOfflineMessageCount()
    }

    private val onComments: Consumer<CommentNotificationsCallback> = Consumer { comments ->
        Timber.d("onComments() count=%d", comments.commentCount)

        update {
            it.copy(
                comments = comments.commentCount,
                commentsOnOwnContent = comments.commentOwnerCount,
                commentsOnSubscriptions = comments.commentSubscriptionsCount,
            )
        }
    }

    private val onItems: Consumer<ItemAnnouncementsCallback> = Consumer { items ->
        Timber.d("onItems() count=%d", items.count)

        update { it.copy(newItems = items.count) }
    }

    private val onOfflineMessages: Consumer<OfflineMessageNotificationCallback> =
        Consumer { messages ->
            Timber.d("onOfflineMessages() count=%d", messages.messageCount)

            update {
                it.copy(
                    offlineMessages = messages.messageCount,
                    offlineMessageFriends = messages.friendsWithOfflineMessages.size,
                )
            }
        }

    private fun update(change: (NotificationCounts) -> NotificationCounts) {
        SteamAccount.update { it.copy(notifications = change(it.notifications)) }
    }
}
