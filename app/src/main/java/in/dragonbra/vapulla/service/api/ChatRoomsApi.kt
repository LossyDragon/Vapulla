package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_AckChatMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_GetMessageHistory_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_GetMyChatRoomGroups_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_IncomingChatMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_SendChatMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_SetSessionActiveChatRoomGroups_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.EChatRoomServerMessage
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.ServerMessage
import `in`.dragonbra.javasteam.rpc.service.ChatRoom
import `in`.dragonbra.javasteam.rpc.service.ChatRoomClient
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodNotification
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.service.SessionComponent
import `in`.dragonbra.vapulla.util.SteamTimeoutException
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.helpers.accountIdToSteamID
import `in`.dragonbra.vapulla.util.toChatPreview
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Group chats via the `ChatRoom` unified service: the groups the account is in, and reading
 * and writing the messages of one of their channels. Nothing is stored; a channel's history
 * is fetched when it is opened and new messages are handed out through [incoming].
 *
 * Names and avatars are not part of a chat message, only the sender's ID, so they are looked
 * up through [SteamFriends] persona state, the same way the Steam client fills them in. That
 * also covers a group's avatar when it is backed by a Steam community group (a clan), whose
 * summary carries neither an avatar URL nor a usable SHA.
 *
 * @param scope outlives a single sign-in; used for work started by a pushed message.
 */
class ChatRoomsApi(
    private val chatRoom: ChatRoom,
    private val friends: SteamFriends,
    private val callbacks: CallbackManager,
    private val scope: CoroutineScope,
    private val localSteamId: () -> SteamID?,
) : SessionComponent {

    private val _incoming = MutableSharedFlow<GroupChatMessage>(extraBufferCapacity = 64)

    /** Messages Steam pushes for the groups held with [holdGroup]. */
    val incoming: SharedFlow<GroupChatMessage> = _incoming.asSharedFlow()

    /** Group ID to the number of [holdGroup] calls not yet released; guarded by itself. */
    private val heldGroups = HashMap<Long, Int>()
    private val activeGroupsMutex = Mutex()

    private val _groups = MutableStateFlow<List<ChatGroup>>(emptyList())

    /** The groups as last fetched by [myGroups], for screens that need a group's channels. */
    val groups: StateFlow<List<ChatGroup>> = _groups.asStateFlow()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            ChatRoomClient,
            CChatRoom_IncomingChatMessage_Notification.Builder,
            >(onIncomingMessage),
    )

    /**
     * The groups the account is a member of, each with its text channels. Empty when Steam
     * does not answer.
     */
    suspend fun myGroups(): List<ChatGroup> {
        val request = CChatRoom_GetMyChatRoomGroups_Request.newBuilder().build()
        val body = chatRoom.getMyChatRoomGroups(request).bodyOrNull("GetMyChatRoomGroups")
            ?: return emptyList()

        val summaries = body.chatRoomGroupsList.map { it.groupSummary }
        val clanAvatars = clanAvatars(summaries.map { it.clanid }.filter { it != 0 })

        return summaries.map { summary ->
            ChatGroup(
                groupId = summary.chatGroupId,
                name = summary.chatGroupName,
                tagline = summary.chatGroupTagline,
                // A group's own avatar first, then the avatar of the Steam group it belongs to.
                avatarUrl = summary.avatarUgcUrl
                    .ifEmpty {
                        val sha = summary.chatGroupAvatarSha.toByteArray().toHexString()
                        Utils.getGroupAvatarURL(sha).orEmpty()
                    }
                    .ifEmpty { clanAvatars[summary.clanid].orEmpty() },
                activeMembers = summary.activeMemberCount,
                rooms = summary.chatRoomsList
                    .sortedBy { it.sortOrder }
                    .map { room ->
                        ChatRoomInfo(
                            chatId = room.chatId,
                            name = room.chatName,
                            lastMessage = room.lastMessage.toChatPreview(),
                            timeLastMessage = room.timeLastMessage.toUInt().toLong(),
                        )
                    }
                    .toImmutableList(),
            )
        }.also { _groups.value = it }
    }

    /**
     * Asks for the messages of a group to be pushed to this session, until a matching
     * [releaseGroup]. Steam only pushes for the groups a session has marked active. Several
     * screens can want the same group at once (the group list with it expanded, and an open
     * channel), so holds are counted and Steam is told the combined set.
     */
    fun holdGroup(groupId: Long) {
        val isNew = synchronized(heldGroups) {
            val holds = heldGroups[groupId] ?: 0
            heldGroups[groupId] = holds + 1
            holds == 0
        }
        if (isNew) sendActiveGroups()
    }

    /** Gives up one [holdGroup] on a group; its messages stop once nothing holds it. */
    fun releaseGroup(groupId: Long) {
        val isGone = synchronized(heldGroups) {
            val holds = heldGroups[groupId] ?: return
            if (holds > 1) {
                heldGroups[groupId] = holds - 1
                false
            } else {
                heldGroups.remove(groupId)
                true
            }
        }
        if (isGone) sendActiveGroups()
    }

    /**
     * Tells Steam the groups currently held. Steam keeps one set per session and each call
     * replaces it, so the set is read when the request is about to go out: whichever call
     * runs last sends the latest state.
     */
    private fun sendActiveGroups() {
        scope.launch {
            activeGroupsMutex.withLock {
                val groupIds = synchronized(heldGroups) { heldGroups.keys.toList() }

                val request = CChatRoom_SetSessionActiveChatRoomGroups_Request.newBuilder()
                    .addAllChatGroupIds(groupIds)
                    .addAllChatGroupsDataRequested(groupIds)
                    .build()

                val result = chatRoom.setSessionActiveChatRoomGroups(request)
                    .resultOf("SetSessionActiveChatRoomGroups")
                Timber.d("Active chat groups $groupIds answered $result")
            }
        }
    }

    /**
     * One page of a channel's messages, newest first.
     *
     * @param before the oldest message already loaded, to continue from; null for the latest.
     * @throws IllegalStateException naming Steam's result when it refuses.
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun history(
        groupId: Long,
        chatId: Long,
        before: GroupChatMessage? = null,
    ): GroupChatPage {
        val request = CChatRoom_GetMessageHistory_Request.newBuilder()
            .setChatGroupId(groupId)
            .setChatId(chatId)
            .setMaxCount(HISTORY_PAGE_SIZE)
            .apply {
                if (before != null) {
                    lastTime = (before.timestamp / 1000).toInt()
                    lastOrdinal = before.ordinal
                }
            }
            .build()

        val body = chatRoom.getMessageHistory(request).bodyOrThrow("GetMessageHistory")
        val entries = body.messagesList.filter { !it.deleted }

        // One persona request for everyone who appears on the page.
        awaitPersonas(
            entries.flatMap { entry ->
                listOf(entry.sender, entry.serverMessage.accountidParam).filter { it != 0 }
            }.map { it.accountIdToSteamID() },
        ) { friends.getFriendPersonaName(it).isNullOrBlank() }

        val messages = entries.mapNotNull { entry ->
            buildMessage(
                groupId = groupId,
                chatId = chatId,
                sender = entry.sender.accountIdToSteamID(),
                text = entry.message,
                timestamp = entry.serverTimestamp * 1000L,
                ordinal = entry.ordinal,
                serverMessage = entry.serverMessage.takeIf { entry.hasServerMessage() },
            )
        }.sortedWith(NEWEST_FIRST)

        return GroupChatPage(
            messages = messages.toImmutableList(),
            moreAvailable = body.moreAvailable,
        )
    }

    /**
     * Sends [text] to a channel.
     *
     * @return the message as Steam stored it; null if it was turned down or got no answer.
     */
    suspend fun send(groupId: Long, chatId: Long, text: String): GroupChatMessage? {
        val sender = localSteamId() ?: return null

        val request = CChatRoom_SendChatMessage_Request.newBuilder()
            .setChatGroupId(groupId)
            .setChatId(chatId)
            .setMessage(text)
            // The reply already says how the message was stored, so no echo is needed.
            .setEchoToSender(false)
            .build()

        val body = chatRoom.sendChatMessage(request).bodyOrNull("SendChatMessage") ?: return null

        return buildMessage(
            groupId = groupId,
            chatId = chatId,
            sender = sender,
            text = body.modifiedMessage.ifEmpty { text },
            timestamp = body.serverTimestamp * 1000L,
            ordinal = body.ordinal,
            serverMessage = null,
        )
    }

    /** Marks a channel as read up to now, which clears its unread state on other devices. */
    fun ack(groupId: Long, chatId: Long) {
        val request = CChatRoom_AckChatMessage_Notification.newBuilder()
            .setChatGroupId(groupId)
            .setChatId(chatId)
            .setTimestamp(steamTimeNow())
            .build()

        runCatching { chatRoom.ackChatMessage(request) }
            .onFailure { Timber.w(it, "AckChatMessage failed") }
    }

    private val onIncomingMessage:
        (ServiceMethodNotification<CChatRoom_IncomingChatMessage_Notification.Builder>) -> Unit =
        { notification ->
            val body = notification.body
            Timber.d("onIncomingMessage() group=${body.chatGroupId} chat=${body.chatId}")

            scope.launch {
                val sender = SteamID(body.steamidSender)
                val subject = body.serverMessage.accountidParam.takeIf { it != 0 }
                awaitPersonas(listOfNotNull(sender, subject?.accountIdToSteamID())) {
                    friends.getFriendPersonaName(it).isNullOrBlank()
                }

                buildMessage(
                    groupId = body.chatGroupId,
                    chatId = body.chatId,
                    sender = sender,
                    text = body.message,
                    timestamp = body.timestamp * 1000L,
                    ordinal = body.ordinal,
                    serverMessage = body.serverMessage.takeIf { body.hasServerMessage() },
                )?.let { _incoming.emit(it) }
            }
        }

    /**
     * Turns the wire fields into a message for display. A server message (someone joined, the
     * chat was renamed) has no text of its own, so one is written for it here; the kinds
     * that are not worth a line give null.
     */
    private fun buildMessage(
        groupId: Long,
        chatId: Long,
        sender: SteamID,
        text: String,
        timestamp: Long,
        ordinal: Int,
        serverMessage: ServerMessage?,
    ): GroupChatMessage? {
        val body = if (serverMessage != null) {
            val subject = serverMessage.accountidParam.takeIf { it != 0 }
                ?.accountIdToSteamID()
                ?: sender
            describe(serverMessage, personaName(subject)) ?: return null
        } else {
            text.ifEmpty { return null }
        }

        return GroupChatMessage(
            groupId = groupId,
            chatId = chatId,
            senderId = sender.convertToUInt64(),
            senderName = personaName(sender),
            senderAvatarUrl = friends.getFriendAvatar(sender)
                ?.takeIf { it.isNotEmpty() }
                ?.toHexString()
                ?.let(Utils::getAvatarURL)
                .orEmpty(),
            message = body,
            timestamp = timestamp,
            ordinal = ordinal,
            fromLocal = sender == localSteamId(),
            isServerMessage = serverMessage != null,
        )
    }

    private fun describe(message: ServerMessage, name: String): String? = when (message.message) {
        EChatRoomServerMessage.k_EChatRoomServerMsg_Joined -> "$name joined"

        EChatRoomServerMessage.k_EChatRoomServerMsg_Parted -> "$name left"

        EChatRoomServerMessage.k_EChatRoomServerMsg_Kicked -> "$name was kicked"

        EChatRoomServerMessage.k_EChatRoomServerMsg_Invited -> "$name was invited"

        EChatRoomServerMessage.k_EChatRoomServerMsg_RenameChatRoom ->
            "The chat was renamed to ${message.stringParam}"

        else -> null
    }

    private fun personaName(steamId: SteamID): String =
        friends.getFriendPersonaName(steamId)?.takeIf { it.isNotBlank() }
            ?: "User ${steamId.accountID}"

    /**
     * Makes sure JavaSteam's persona cache knows each of [steamIds], people or clans. Those
     * not [missing] from it are skipped; the rest are requested with `ClientRequestFriendData`
     * and awaited briefly, since Steam does not answer for every account.
     *
     * @param missing true for an ID the cache does not hold what the caller needs of.
     */
    private suspend fun awaitPersonas(
        steamIds: Collection<SteamID>,
        missing: (SteamID) -> Boolean,
    ) {
        // Emptied by the callback thread while this coroutine reads it.
        val waitingFor = ConcurrentHashMap.newKeySet<SteamID>().apply {
            addAll(steamIds.filter(missing))
        }
        if (waitingFor.isEmpty()) {
            return
        }

        val done = CompletableDeferred<Unit>()
        callbacks.subscribe<PersonaStateCallback> {
            if (waitingFor.remove(it.friendId) && waitingFor.isEmpty()) done.complete(Unit)
        }.use {
            friends.requestFriendInfo(waitingFor.toList())
            withTimeoutOrNull(PERSONA_TIMEOUT) { done.await() }
        }
    }

    /**
     * Avatar URLs keyed by clan ID. Clans already in JavaSteam's cache are answered at once;
     * the rest are asked for first.
     */
    private suspend fun clanAvatars(clanIds: List<Int>): Map<Int, String> {
        val steamIds = clanIds.associateWith {
            SteamID(it.toLong(), EUniverse.Public, EAccountType.Clan)
        }
        awaitPersonas(steamIds.values) { friends.getClanAvatar(it)?.isEmpty() ?: true }

        return steamIds.mapValues { (_, id) ->
            friends.getClanAvatar(id)?.toHexString()?.let(Utils::getAvatarURL).orEmpty()
        }
    }

    private companion object {
        val PERSONA_TIMEOUT = 3.seconds

        /** Messages per page of channel history. */
        const val HISTORY_PAGE_SIZE = 50

        /** Steam orders a channel by time, then by ordinal within the same second. */
        val NEWEST_FIRST: Comparator<GroupChatMessage> =
            compareByDescending<GroupChatMessage> { it.timestamp }.thenByDescending { it.ordinal }
    }
}
