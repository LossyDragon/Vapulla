package `in`.dragonbra.vapulla.service.api

import android.content.Context
import android.os.SystemClock
import androidx.room.withTransaction
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_AckMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendsMessages_GetActiveMessageSessions_Request
import `in`.dragonbra.javasteam.rpc.service.FriendMessages
import `in`.dragonbra.javasteam.rpc.service.FriendMessagesClient
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodNotification
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.service.SteamState
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.helpers.accountIdToSteamID
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * One-to-one friend chat via the `FriendMessages` unified service: sending, history, read
 * state, and the messages Steam pushes. Everything is mirrored into the `chat_message` table,
 * which the chat screen and the friends list observe.
 *
 * @param scope outlives a single sign-in; used for work started by a pushed message.
 */
class ChatApi(
    private val context: Context,
    private val scope: CoroutineScope,
    private val db: VapullaDatabase,
    private val friendMessages: FriendMessages,
    private val localSteamId: () -> SteamID?,
) : SessionComponent {

    /** The friend whose chat is on screen, so their messages are not notified or left unread. */
    @Volatile
    private var activeChatFriendId: Long? = null

    private val chatMutex = Mutex()

    private val _typing = MutableStateFlow<Set<Long>>(emptySet())

    /** The friends who are typing to us right now. */
    val typing: StateFlow<Set<Long>> = _typing.asStateFlow()

    /** Per friend, the job that ends their typing state when no further notice arrives. */
    private val typingTimeouts = ConcurrentHashMap<Long, Job>()

    /** Per friend, when we last told them we are typing (elapsed realtime). */
    private val typingSent = ConcurrentHashMap<Long, Long>()

    // Chat goes through the FriendMessages service only. The legacy callbacks (FriendMsg,
    // FriendMsgHistory, offline messages) describe the same messages as plain text, which
    // cannot be matched against the BB Code copies and showed every message twice.
    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            FriendMessagesClient,
            CFriendMessages_IncomingMessage_Notification.Builder,
            >(onIncomingMessage),
        callbacks.subscribeServiceNotification<
            FriendMessagesClient,
            CFriendMessages_AckMessage_Notification.Builder,
            >(onReadElsewhere),
    )

    override fun start(session: CoroutineScope) {
        session.launch { syncUnread() }
    }

    /** Tells the service which chat is on screen; null when none is. */
    fun setActiveChat(friendId: Long?) {
        activeChatFriendId = friendId
    }

    /**
     * Sends [text] to a friend. The message is stored unconfirmed at once so it shows
     * immediately, then confirmed with Steam's timestamp.
     *
     * @return false if it could not be sent; the local copy is removed again.
     */
    suspend fun send(friendId: Long, text: String): Boolean = withContext(SEND_CONTEXT) {
        val message = text.trim()

        if (message.isEmpty() || !SteamState.isLoggedIn.value) {
            return@withContext false
        }

        val dao = db.chatMessageDao()
        val localId = dao.insertMessage(
            ChatMessage(
                message = message,
                timestamp = System.currentTimeMillis(),
                friendId = friendId,
                fromLocal = true,
                unread = false,
                timestampConfirmed = false,
            ),
        )

        val request = CFriendMessages_SendMessage_Request.newBuilder()
            .setSteamid(friendId)
            .setChatEntryType(EChatEntryType.ChatMsg.code())
            .setMessage(message)
            .setContainsBbcode(true)
            .build()

        val body = friendMessages.sendMessage(request).bodyOrNull("SendMessage")
        if (body == null) {
            dao.delete(localId)
            return@withContext false
        }

        val sentMessage = body.modifiedMessage.ifEmpty { message }
        val timestamp = body.serverTimestamp * 1000L

        chatMutex.withLock {
            // An echo of this message may have been stored while the reply was in flight.
            if (dao.find(sentMessage, timestamp, friendId, true, true) != null) {
                dao.delete(localId)
            } else {
                dao.confirm(localId, sentMessage, timestamp)
            }
        }

        // The message ends our typing state on the friend's side, so the next keystroke
        // should announce it again without waiting out the interval.
        typingSent.remove(friendId)

        true
    }

    /**
     * Tells a friend we are typing, as a `SendMessage` of entry type `Typing`. Meant to be
     * called on every edit of the message field: Steam shows the state for a while after each
     * notice, so at most one is sent per [TYPING_SEND_INTERVAL_MS].
     */
    suspend fun sendTyping(friendId: Long) {
        if (!SteamState.isLoggedIn.value) {
            return
        }

        val now = SystemClock.elapsedRealtime()
        val last = typingSent[friendId]
        if (last != null && now - last < TYPING_SEND_INTERVAL_MS) {
            return
        }
        typingSent[friendId] = now

        val request = CFriendMessages_SendMessage_Request.newBuilder()
            .setSteamid(friendId)
            .setChatEntryType(EChatEntryType.Typing.code())
            .build()

        friendMessages.sendMessage(request).awaitOrNull("Typing notice")
    }

    /** Marks a friend as typing until [TYPING_SHOWN] passes without another notice. */
    private fun markTyping(friendId: Long) {
        _typing.update { it + friendId }

        typingTimeouts.put(
            friendId,
            scope.launch {
                delay(TYPING_SHOWN)
                _typing.update { it - friendId }
            },
        )?.cancel()
    }

    private fun clearTyping(friendId: Long) {
        typingTimeouts.remove(friendId)?.cancel()
        _typing.update { it - friendId }
    }

    /**
     * Fetches the latest messages with a friend, so a chat shows history when opened.
     *
     * @param unreadAfter when given, the friend's messages newer than this (epoch
     *  milliseconds) are stored as unread; used for what arrived while signed out.
     */
    suspend fun loadRecent(friendId: Long, unreadAfter: Long? = null) =
        withContext(Dispatchers.IO) {
            val localId = localSteamId() ?: return@withContext

            val request = CFriendMessages_GetRecentMessages_Request.newBuilder()
                .setSteamid1(localId.convertToUInt64())
                .setSteamid2(friendId)
                .setCount(RECENT_MESSAGE_COUNT)
                .setBbcodeFormat(true)
                .setTimeLast(Int.MAX_VALUE)
                .build()

            val body = friendMessages.getRecentMessages(request).bodyOrNull("GetRecentMessages")
                ?: return@withContext

            storeConfirmed(
                body.messagesList.map { message ->
                    val timestamp = message.timestamp * 1000L
                    val fromLocal = message.accountid.toUInt().toLong() == localId.accountID

                    ChatMessage(
                        message = message.message,
                        timestamp = timestamp,
                        friendId = friendId,
                        fromLocal = fromLocal,
                        unread = !fromLocal &&
                            unreadAfter != null &&
                            timestamp > unreadAfter &&
                            activeChatFriendId != friendId,
                        timestampConfirmed = true,
                    )
                },
            )
        }

    /**
     * The chat with a friend was read on another device of the account: Steam echoes that
     * device's `AckMessage` to the others as `NotifyAckMessageEcho`. What was unread here up
     * to that moment is read now, and its notification goes.
     */
    private val onReadElsewhere:
        (ServiceMethodNotification<CFriendMessages_AckMessage_Notification.Builder>) -> Unit =
        { notification ->
            val friendId = notification.body.steamidPartner
            // Steam's time is in seconds; messages are stored in milliseconds.
            val until = notification.body.timestamp * 1000L
            Timber.d("onReadElsewhere() the chat was read on another device")

            scope.launch {
                db.chatMessageDao().markMessagesAsReadUntil(friendId, until)
                NotificationHelper.cancelFriendMessageNotification(context, friendId)
            }
        }

    /** Marks the chat with a friend as read, here and on Steam, and clears its notification. */
    suspend fun ack(friendId: Long) = withContext(Dispatchers.IO) {
        db.chatMessageDao().markMessagesAsRead(friendId)
        NotificationHelper.cancelFriendMessageNotification(context, friendId)

        if (!SteamState.isLoggedIn.value) {
            return@withContext
        }

        val request = CFriendMessages_AckMessage_Notification.newBuilder()
            .setSteamidPartner(friendId)
            .setTimestamp(steamTimeNow())
            .build()

        try {
            friendMessages.ackMessage(request)
        } catch (e: Exception) {
            Timber.w(e, "AckMessage failed")
        }
    }

    /**
     * Stores messages Steam has confirmed, skipping those already there. A matching message
     * that was sent from here and is still unconfirmed is confirmed instead of duplicated.
     *
     * [chatMutex] is always taken before the database, never inside a transaction, so chat
     * writers cannot deadlock each other. The single transaction means the paged chat list
     * is invalidated once per batch rather than once per message.
     *
     * @return the messages that were new.
     */
    private suspend fun storeConfirmed(messages: List<ChatMessage>): List<ChatMessage> {
        if (messages.isEmpty()) {
            return emptyList()
        }

        val dao = db.chatMessageDao()

        return chatMutex.withLock {
            db.withTransaction {
                messages.filter { incoming ->
                    val known = dao.find(
                        message = incoming.message,
                        timestamp = incoming.timestamp,
                        friendId = incoming.friendId,
                        fromLocal = incoming.fromLocal,
                        confirmed = true,
                    )

                    if (known != null) {
                        return@filter false
                    }

                    val unconfirmed = dao
                        .find(incoming.message, incoming.friendId, incoming.fromLocal, false)
                        .minByOrNull { abs(incoming.timestamp - it.timestamp) }

                    if (unconfirmed != null) {
                        dao.confirm(unconfirmed.id, unconfirmed.message, incoming.timestamp)
                        false
                    } else {
                        dao.insertMessage(incoming)
                        true
                    }
                }
            }
        }
    }

    /**
     * A chat message pushed by Steam: either a friend wrote to us, or we wrote from another
     * device and this session gets the echo. Typing notices and the like are ignored.
     */
    private val onIncomingMessage:
        (ServiceMethodNotification<CFriendMessages_IncomingMessage_Notification.Builder>) -> Unit =
        { notification ->
            val body = notification.body
            val friendId = body.steamidFriend
            val fromLocal = body.localEcho

            Timber.d("onIncomingMessage() type=${body.chatEntryType} echo=$fromLocal")

            val isMessage =
                body.chatEntryType == EChatEntryType.ChatMsg.code() && body.message.isNotEmpty()

            // Typing state: a notice starts it, and a message or the friend closing the chat
            // ends it. Echoes are our own activity on another device, not the friend's.
            if (!fromLocal) {
                when {
                    body.chatEntryType == EChatEntryType.Typing.code() -> markTyping(friendId)

                    isMessage || body.chatEntryType == EChatEntryType.LeftConversation.code() ->
                        clearTyping(friendId)
                }
            }

            if (isMessage) {
                scope.launch {
                    val chatIsOpen = activeChatFriendId == friendId

                    val isNew = storeConfirmed(
                        listOf(
                            ChatMessage(
                                message = body.message,
                                timestamp = body.rtime32ServerTimestamp * 1000L,
                                friendId = friendId,
                                fromLocal = fromLocal,
                                unread = !fromLocal && !chatIsOpen,
                                timestampConfirmed = true,
                            ),
                        ),
                    ).isNotEmpty()

                    when {
                        !isNew || fromLocal -> Unit

                        chatIsOpen -> ack(friendId)

                        else -> {
                            val friend = db.steamFriendDao().find(friendId)
                            NotificationHelper.sendFriendMessageNotification(
                                context = context,
                                friendId = friendId,
                                friendName = friend?.nameOrNickname.orEmpty().ifEmpty { "Steam" },
                                message = body.messageNoBbcode.ifEmpty { body.message },
                                avatarUrl = friend?.avatar?.let(Utils::getAvatarURL),
                            )
                        }
                    }
                }
            }
        }

    /**
     * Brings the unread marks in line with Steam's. Steam lists the account's conversations
     * with when each was last looked at, on any device, and how much is unread:
     *
     * - what is stored as unread from before that moment has been read elsewhere, on the PC
     *   say, and is marked read here;
     * - a conversation Steam counts unread messages in gets its recent history loaded, with
     *   everything after that moment marked unread. That is how messages that arrived
     *   while signed out come in.
     *
     * Steam has a notification for a chat read elsewhere as well (`NotifyAckMessageEcho`),
     * see [onReadElsewhere], but that one was seen to arrive only some of the time, so the
     * friends list
     * asks with this as well while it is on screen.
     */
    suspend fun syncUnread() {
        val request = CFriendsMessages_GetActiveMessageSessions_Request.newBuilder()
            .setOnlySessionsWithMessages(true)
            .build()

        val body = friendMessages.getActiveMessageSessions(request)
            .bodyOrNull("GetActiveMessageSessions")
            ?: return

        val unread = body.messageSessionsList.count { it.unreadMessageCount > 0 }
        Timber.d("syncUnread: $unread conversation(s) with unread messages")

        body.messageSessionsList.forEach { session ->
            val friendId = session.accountidFriend.accountIdToSteamID().convertToUInt64()
            // Steam's time is in seconds; messages are stored in milliseconds.
            val lastView = session.lastView * 1000L

            db.chatMessageDao().markMessagesAsReadUntil(friendId, lastView)
            if (session.unreadMessageCount > 0) {
                loadRecent(friendId, unreadAfter = lastView)
            } else {
                NotificationHelper.cancelFriendMessageNotification(context, friendId)
            }
        }
    }

    private companion object {
        /** Messages fetched from Steam when a chat is opened. */
        const val RECENT_MESSAGE_COUNT = 50

        /** Shortest gap between two typing notices to the same friend. */
        const val TYPING_SEND_INTERVAL_MS = 8_000L

        /** How long a friend counts as typing after their last notice. */
        val TYPING_SHOWN = 15.seconds

        /**
         * A send is seen through even when the chat screen is left meanwhile. Cut short, the
         * message could reach the friend while its local copy stays unconfirmed.
         */
        val SEND_CONTEXT = Dispatchers.IO + NonCancellable
    }
}
