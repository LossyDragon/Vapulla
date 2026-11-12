package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChatRoom_GetMyChatRoomGroups_Request
import `in`.dragonbra.javasteam.rpc.service.ChatRoom
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Group chats via the `ChatRoom` unified service. A group's own avatar comes in its summary
 * as a UGC URL or an avatar SHA. Groups backed by a Steam community group (a clan) carry
 * neither, so their avatar is fetched through [SteamFriends] persona state instead, the same
 * way the Steam client shows clan avatars.
 */
class ChatRoomsApi(
    private val chatRoom: ChatRoom,
    private val friends: SteamFriends,
    private val callbacks: CallbackManager,
) {

    /** The groups the account is a member of, each with its text channels. */
    suspend fun myGroups(): List<ChatGroup> {
        val request = CChatRoom_GetMyChatRoomGroups_Request.newBuilder().build()
        val response = chatRoom.getMyChatRoomGroups(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetMyChatRoomGroups failed: ${response.result}")
            return emptyList()
        }

        val summaries = response.body.chatRoomGroupsList.map { it.groupSummary }
        val clanAvatars = clanAvatars(summaries.map { it.clanid }.filter { it != 0 })

        return summaries.map { summary ->
            ChatGroup(
                groupId = summary.chatGroupId,
                name = summary.chatGroupName,
                tagline = summary.chatGroupTagline,
                // TODO: chat_group_avatar_sha is not served from the user avatar CDN (404 on every
                //  known host and suffix). Find the URL the Steam web chat uses for group avatars.
                avatarUrl = summary.avatarUgcUrl
                    .ifEmpty {
                        summary.chatGroupAvatarSha.toByteArray().takeIf {
                            it.isNotEmpty()
                        }?.toHexString()?.let(Utils::getAvatarURL).orEmpty()
                    }
                    .ifEmpty { clanAvatars[summary.clanid].orEmpty() },
                activeMembers = summary.activeMemberCount,
                clanId = summary.clanid,
                appId = summary.appid,
                rooms = summary.chatRoomsList
                    .sortedBy { it.sortOrder }
                    .map { room ->
                        ChatRoomInfo(
                            chatId = room.chatId,
                            name = room.chatName,
                            lastMessage = room.lastMessage,
                            timeLastMessage = room.timeLastMessage.toLong(),
                        )
                    }
                    .toImmutableList(),
            )
        }
    }

    /**
     * Avatar URLs keyed by clan ID. Clans already in JavaSteam's cache are answered at once;
     * the rest are requested with `ClientRequestFriendData` and awaited briefly.
     */
    private suspend fun clanAvatars(clanIds: List<Int>): Map<Int, String> {
        val steamIds = clanIds.associateWith {
            SteamID(it.toLong(), EUniverse.Public, EAccountType.Clan)
        }
        val missing = steamIds.filterValues {
            friends.getClanAvatar(it)?.isEmpty() ?: true
        }.values.toMutableSet()

        if (missing.isNotEmpty()) {
            val done = CompletableDeferred<Unit>()
            callbacks.subscribe<PersonaStateCallback> {
                if (missing.remove(it.friendId) && missing.isEmpty()) done.complete(Unit)
            }.use {
                friends.requestFriendInfo(missing.toList())
                withTimeoutOrNull(AVATAR_TIMEOUT_MS) { done.await() }
            }
        }

        return steamIds.mapValues { (_, id) ->
            friends.getClanAvatar(id)?.toHexString()?.let(Utils::getAvatarURL).orEmpty()
        }
    }

    private companion object {
        const val AVATAR_TIMEOUT_MS = 3_000L
    }
}
