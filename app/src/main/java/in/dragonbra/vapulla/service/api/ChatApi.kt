package `in`.dragonbra.vapulla.service.api

import android.content.Context
import android.os.SystemClock
import androidx.room.withTransaction
import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
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
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    suspend fun send(friendId: Long, text: String): Boolean = withContext(Dispatchers.IO) {
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

        val response = runCatching { friendMessages.sendMessage(request).await() }
            .onFailure { Timber.w(it, "SendMessage failed") }
            .getOrNull()

        if (response?.result != EResult.OK) {
            Timber.w("SendMessage to $friendId answered ${response?.result}")
            dao.delete(localId)
            return@withContext false
        }

        val sentMessage = response.body.modifiedMessage.ifEmpty { message }
        val timestamp = response.body.serverTimestamp * 1000L

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

        runCatching { friendMessages.sendMessage(request).await() }
            .onFailure { Timber.w(it, "Typing notice failed") }
    }

    /** Marks a friend as typing until [TYPING_SHOWN_MS] pass without another notice. */
    private fun markTyping(friendId: Long) {
        _typing.update { it + friendId }

        typingTimeouts.put(
            friendId,
            scope.launch {
                delay(TYPING_SHOWN_MS)
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

            val response = runCatching { friendMessages.getRecentMessages(request).await() }
                .onFailure { Timber.w(it, "GetRecentMessages failed") }
                .getOrNull() ?: return@withContext

            if (response.result != EResult.OK) {
                Timber.w("GetRecentMessages for $friendId answered ${response.result}")
                return@withContext
            }

            storeConfirmed(
                response.body.messagesList.map { message ->
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

    /** Marks the chat with a friend as read, here and on Steam, and clears its notification. */
    suspend fun ack(friendId: Long) = withContext(Dispatchers.IO) {
        db.chatMessageDao().markMessagesAsRead(friendId)
        NotificationHelper.cancelFriendMessageNotification(context, friendId)

        if (!SteamState.isLoggedIn.value) {
            return@withContext
        }

        val request = CFriendMessages_AckMessage_Notification.newBuilder()
            .setSteamidPartner(friendId)
            .setTimestamp((System.currentTimeMillis() / 1000).toInt())
            .build()

        friendMessages.ackMessage(request)
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
                        dao.update(
                            unconfirmed.copy(
                                timestamp = incoming.timestamp,
                                timestampConfirmed = true,
                            ),
                        )
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
     * Fetches the messages that arrived while signed out. Steam lists the conversations
     * with unread messages and when each was last viewed; their recent history is then
     * loaded with everything after that point marked unread.
     */
    private suspend fun syncUnread() {
        val request = CFriendsMessages_GetActiveMessageSessions_Request.newBuilder()
            .setOnlySessionsWithMessages(true)
            .build()

        val response = runCatching { friendMessages.getActiveMessageSessions(request).await() }
            .onFailure { Timber.w(it, "GetActiveMessageSessions failed") }
            .getOrNull() ?: return

        if (response.result != EResult.OK) {
            Timber.w("GetActiveMessageSessions answered ${response.result}")
            return
        }

        val unread = response.body.messageSessionsList.filter { it.unreadMessageCount > 0 }
        Timber.d("syncUnread: ${unread.size} conversation(s) with unread messages")

        unread.forEach { session ->
            val friendId = SteamID(
                session.accountidFriend.toUInt().toLong(),
                EUniverse.Public,
                EAccountType.Individual,
            ).convertToUInt64()

            loadRecent(friendId, unreadAfter = session.lastView * 1000L)
        }
    }

    private companion object {
        /** Messages fetched from Steam when a chat is opened. */
        const val RECENT_MESSAGE_COUNT = 50

        /** Shortest gap between two typing notices to the same friend. */
        const val TYPING_SEND_INTERVAL_MS = 8_000L

        /** How long a friend counts as typing after their last notice. */
        val TYPING_SHOWN_MS = 15.seconds
    }
}
