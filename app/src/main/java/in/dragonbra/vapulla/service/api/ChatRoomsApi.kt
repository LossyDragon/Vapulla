package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoomMember
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_AckChatMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_GetMessageHistory_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_GetMyChatRoomGroups_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_IncomingChatMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_MessageReaction_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_SendChatMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_SetSessionActiveChatRoomGroups_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_UpdateMessageReaction_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.EChatRoomGroupRank
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.EChatRoomJoinState
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.EChatRoomMessageReactionType
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
 * Group chats via the `ChatRoom` unified service: the groups the account is in, their
 * members, and reading, writing and reacting to the messages of one of their channels.
 * Nothing is stored; a channel's history is fetched when it is opened, and new messages and
 * reactions are handed out through [incoming] and [reactionChanges].
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

    /** Group ID to its members as Steam last listed them, for the groups held. */
    private val groupMembers = ConcurrentHashMap<Long, List<CChatRoomMember>>()

    private val _groups = MutableStateFlow<List<ChatGroup>>(emptyList())

    /** The groups as last fetched by [myGroups], for screens that need a group's channels. */
    val groups: StateFlow<List<ChatGroup>> = _groups.asStateFlow()

    private val _reactionChanges = MutableSharedFlow<GroupReactionChange>(extraBufferCapacity = 64)

    /** Reactions given and taken back in the groups held with [holdGroup], as they happen. */
    val reactionChanges: SharedFlow<GroupReactionChange> = _reactionChanges.asSharedFlow()

    override fun subscribe(callbacks: CallbackManager): List<Closeable> = listOf(
        callbacks.subscribeServiceNotification<
            ChatRoomClient,
            CChatRoom_IncomingChatMessage_Notification.Builder,
            >(onIncomingMessage),
        callbacks.subscribeServiceNotification<
            ChatRoomClient,
            CChatRoom_MessageReaction_Notification.Builder,
            >(onReaction),
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
        scope.launch { tellActiveGroups() }
    }

    /**
     * Sends Steam the set of held groups. Its answer carries the state of each, members
     * included, which is kept in [groupMembers]: Steam turns down a request for one group's
     * state on its own (`GetChatRoomGroupState` answers `Fail`), so this is where the member
     * list comes from.
     *
     * @return false when Steam refused or did not answer.
     */
    private suspend fun tellActiveGroups(): Boolean = activeGroupsMutex.withLock {
        val groupIds = synchronized(heldGroups) { heldGroups.keys.toList() }

        val request = CChatRoom_SetSessionActiveChatRoomGroups_Request.newBuilder()
            .addAllChatGroupIds(groupIds)
            .addAllChatGroupsDataRequested(groupIds)
            .build()

        val body = chatRoom.setSessionActiveChatRoomGroups(request)
            .bodyOrNull("SetSessionActiveChatRoomGroups")
        Timber.d("Active chat groups $groupIds answered, ${body?.chatStatesCount} with state")

        body?.chatStatesList?.forEach { state ->
            groupMembers[state.headerState.chatGroupId] = state.membersList
        }
        // A group no longer held gets no member updates, so what is kept of it goes.
        groupMembers.keys.retainAll(groupIds.toSet())

        body != null
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
            )?.copy(
                reactions = entry.reactionsList.map {
                    MessageReaction(
                        reaction = it.reaction,
                        isSticker = it.reactionType == STICKER,
                        count = it.numReactors,
                        mine = it.hasUserReacted,
                    )
                }.toImmutableList(),
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

    /**
     * Gives a reaction to a message of a channel, or takes it back.
     *
     * Steam pushes the change to everyone else in the channel but not back to the session
     * that made it, so the caller has to show it itself.
     *
     * @param reaction an emoticon's name between colons, or a sticker's name.
     * @return false if Steam refused or did not answer.
     */
    suspend fun react(
        message: GroupChatMessage,
        reaction: String,
        isSticker: Boolean,
        add: Boolean,
    ): Boolean {
        val request = CChatRoom_UpdateMessageReaction_Request.newBuilder()
            .setChatGroupId(message.groupId)
            .setChatId(message.chatId)
            .setServerTimestamp((message.timestamp / 1000).toInt())
            .setOrdinal(message.ordinal)
            .setReactionType(if (isSticker) STICKER else EMOTICON)
            .setReaction(reaction)
            .setIsAdd(add)
            .build()

        return chatRoom.updateMessageReaction(request)
            .bodyOrNull("ChatRoom.UpdateMessageReaction") != null
    }

    /**
     * The members of a group held with [holdGroup], highest rank first and by name within a
     * rank. They are asked for anew each time, as people come and go; without an answer the
     * ones from last time are given. Steam leaves the list out for the largest groups, which
     * the Steam client pages through instead; those come out empty here. Names are looked up
     * for the first [MEMBER_NAME_LIMIT]; the rest are listed by their account ID.
     */
    suspend fun members(groupId: Long): List<ChatGroupMember> {
        tellActiveGroups()
        val members = groupMembers[groupId] ?: return emptyList()

        // Highest rank first here too, so that those are the ones whose names are asked for.
        val joined = members
            .filter { it.state == EChatRoomJoinState.k_EChatRoomJoinState_Joined }
            .sortedByDescending { it.rank.number }

        awaitPersonas(joined.take(MEMBER_NAME_LIMIT).map { it.accountid.accountIdToSteamID() }) {
            friends.getFriendPersonaName(it).isNullOrBlank()
        }

        // The sort is stable, so sorting by name first leaves each rank in name order.
        return joined
            .map { member ->
                val steamId = member.accountid.accountIdToSteamID()
                val listed = ChatGroupMember(
                    steamId = steamId.convertToUInt64(),
                    accountId = member.accountid,
                    name = personaName(steamId),
                    avatarUrl = avatarUrl(steamId),
                    rank = when (member.rank) {
                        EChatRoomGroupRank.k_EChatRoomGroupRank_Owner -> "Owner"
                        EChatRoomGroupRank.k_EChatRoomGroupRank_Officer -> "Officer"
                        EChatRoomGroupRank.k_EChatRoomGroupRank_Moderator -> "Moderator"
                        else -> ""
                    },
                )
                listed to member.rank.number
            }
            .sortedBy { (member, _) -> member.name.lowercase() }
            .sortedByDescending { (_, rank) -> rank }
            .map { (member, _) -> member }
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
                )?.let { message ->
                    // Steam has worked out who a pushed message calls on; no need to read it.
                    val self = localSteamId()?.accountID?.toInt()
                    val mentions = body.mentions
                    val mentionsMe = mentions.mentionAll ||
                        mentions.mentionHere ||
                        self in mentions.mentionAccountidsList

                    _incoming.emit(message.copy(mentionsMe = mentionsMe && !message.fromLocal))
                }
            }
        }

    private val onReaction:
        (ServiceMethodNotification<CChatRoom_MessageReaction_Notification.Builder>) -> Unit =
        { notification ->
            val body = notification.body
            Timber.d("onReaction() group=${body.chatGroupId} add=${body.isAdd}")

            _reactionChanges.tryEmit(
                GroupReactionChange(
                    groupId = body.chatGroupId,
                    chatId = body.chatId,
                    timestamp = body.serverTimestamp * 1000L,
                    ordinal = body.ordinal,
                    reaction = body.reaction,
                    isSticker = body.reactionType == STICKER,
                    isAdd = body.isAdd,
                    fromLocal = SteamID(body.reactor) == localSteamId(),
                ),
            )
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

        val fromLocal = sender == localSteamId()

        return GroupChatMessage(
            groupId = groupId,
            chatId = chatId,
            senderId = sender.convertToUInt64(),
            senderName = personaName(sender),
            senderAvatarUrl = avatarUrl(sender),
            message = body,
            timestamp = timestamp,
            ordinal = ordinal,
            fromLocal = fromLocal,
            isServerMessage = serverMessage != null,
            mentionsMe = serverMessage == null && !fromLocal && mentionsLocal(text),
        )
    }

    /**
     * Whether a message's text calls on the signed-in account. A mention of someone is a
     * `[mention=accountid]@Name[/mention]` tag; everyone is called with `@all`, and everyone
     * online with `@here`, which are looked for both as a tag and as a plain word.
     */
    private fun mentionsLocal(text: String): Boolean {
        if ('@' !in text) return false
        val called = setOf(localSteamId()?.accountID?.toString(), "all", "here")

        return MENTION_TAG.findAll(text).any { it.groupValues[1].lowercase() in called } ||
            MENTION_EVERYONE.containsMatchIn(text)
    }

    /** The full address of [steamId]'s avatar; empty when JavaSteam does not hold it. */
    private fun avatarUrl(steamId: SteamID): String = friends.getFriendAvatar(steamId)
        ?.takeIf { it.isNotEmpty() }
        ?.toHexString()
        ?.let(Utils::getAvatarURL)
        .orEmpty()

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

        /** Members of a group whose names are asked for; a persona request has its limits. */
        const val MEMBER_NAME_LIMIT = 200

        val EMOTICON = EChatRoomMessageReactionType.k_EChatRoomMessageReactionType_Emoticon
        val STICKER = EChatRoomMessageReactionType.k_EChatRoomMessageReactionType_Sticker

        val MENTION_TAG = Regex("""\[mention=(\d+|all|here)]""", RegexOption.IGNORE_CASE)
        val MENTION_EVERYONE = Regex("""(^|\s)@(all|here)\b""", RegexOption.IGNORE_CASE)

        /** Steam orders a channel by time, then by ordinal within the same second. */
        val NEWEST_FIRST: Comparator<GroupChatMessage> =
            compareByDescending<GroupChatMessage> { it.timestamp }.thenByDescending { it.ordinal }
    }
}
