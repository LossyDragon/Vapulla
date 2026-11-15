package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetMutualFriendsForIncomingInvites_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.ProfileItems
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The things the user does to a friend or to their own presence, via the [SteamFriends]
 * handler. Keeping the friends table in step with what Steam pushes is
 * [in.dragonbra.vapulla.service.sync.FriendsSync]'s job.
 */
class FriendsApi(
    private val db: VapullaDatabase,
    private val steamFriends: SteamFriends,
    private val player: Player,
) {

    fun setPersonaState(state: EPersonaState) {
        steamFriends.setPersonaState(state = state)
    }

    /**
     * Asks Steam to change the account's own profile name. Its answer is not tied to this
     * call: it arrives as a `PersonaChangeCallback`, which
     * [in.dragonbra.vapulla.service.SteamConnection] passes on.
     */
    fun setPersonaName(name: String) {
        steamFriends.setPersonaName(name)
    }

    /**
     * The friends each sender of an incoming friend request has in common with the account:
     * the sender's Steam ID to the Steam IDs of those friends. Senders with none in common
     * may be left out. Empty when Steam does not answer.
     */
    suspend fun mutualFriendsOfRequests(): Map<Long, List<Long>> {
        val request = CPlayer_GetMutualFriendsForIncomingInvites_Request.newBuilder().build()

        val response = player.getMutualFriendsForIncomingInvites(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetMutualFriendsForIncomingInvites failed: ${response.result}")
            return emptyMap()
        }

        return response.body.incomingInviteMutualFriendsListsList.associate { invite ->
            // The friends come as account IDs, the low half of a Steam ID.
            invite.steamid to invite.mutualFriendAccountIdsList.map { accountId ->
                SteamID(accountId.toUInt().toLong(), EUniverse.Public, EAccountType.Individual)
                    .convertToUInt64()
            }
        }
    }

    /** Accepts the friend request from [friendId]. */
    fun acceptRequest(friendId: Long) {
        steamFriends.addFriend(friendId.toSteamID())
    }

    /** Declines the friend request from [friendId]. */
    fun declineRequest(friendId: Long) {
        steamFriends.removeFriend(friendId.toSteamID())
    }

    /** Declines the friend request from [friendId] and blocks them. */
    fun blockRequest(friendId: Long) {
        steamFriends.ignoreFriend(friendId.toSteamID(), setIgnore = true)
    }

    suspend fun getProfileItems(friendId: Long) = withContext(Dispatchers.IO) {
        val request = SteammessagesPlayerSteamclient.CPlayer_GetProfileItemsEquipped_Request
            .newBuilder().apply {
                this.steamid = friendId
            }
            .build()

        val response = player.getProfileItemsEquipped(request).await()

        if (response.result != EResult.OK) {
            Timber.e("Failed to get profile items for $friendId, result: ${response.result}")
            return@withContext
        }

        val body = response.body
        var dbFriend = db.steamFriendDao().find(friendId)

        if (dbFriend == null) {
            Timber.i("No friend found in db to update profile items")
            return@withContext
        }

        dbFriend = dbFriend.copy(
            profileBackground = ProfileItems.from(body.profileBackground),
            profileMiniBackground = ProfileItems.from(body.miniProfileBackground),
            profileAvatarFrame = ProfileItems.from(body.avatarFrame),
            profileAnimatedAvatar = ProfileItems.from(body.animatedAvatar),
            profileProfileModifier = ProfileItems.from(body.profileModifier),
        )

        db.steamFriendDao().update(dbFriend)
    }

    /**
     * Blocks [friendId], or unblocks them when they already are. Nothing is stored here:
     * Steam follows its answer with a friends list update carrying the new relationship,
     * which [in.dragonbra.vapulla.service.sync.FriendsSync] writes to the database.
     */
    suspend fun ignoreFriend(friendId: Long) = withContext(Dispatchers.IO) {
        val dbFriend = db.steamFriendDao().find(friendId) ?: return@withContext
        val block = !dbFriend.isBlocked
        Timber.d("ignoreFriend() id=%d relation=%s block=%b", friendId, dbFriend.relation, block)

        val result = steamFriends.ignoreFriend(friendId.toSteamID(), setIgnore = block).await()
        Timber.d("ignoreFriend() id=%d result=%s", friendId, result.result)
    }

    suspend fun removeFriend(friendId: Long) = withContext(Dispatchers.IO) {
        val friend = friendId.toSteamID()
        steamFriends.removeFriend(friend)
        db.steamFriendDao().remove(friendId)
    }

    /**
     * Sets the nickname of [friendId]; an empty [value] removes it.
     *
     * The nickname is stored here as well as sent, so the change shows at once. Steam also
     * announces a changed nickname to every signed-in device, which
     * [in.dragonbra.vapulla.service.sync.FriendsSync] writes; if that reaches this device
     * too it finds the nickname already in place. Steam's answer cannot be awaited with this
     * call, so should it refuse the change, the list at the next sign-in puts the stored
     * one right again.
     */
    suspend fun setNickname(friendId: Long, value: String) = withContext(Dispatchers.IO) {
        val nickname = value.trim()
        steamFriends.setFriendNickname(friendId.toSteamID(), nickname)

        with(db.steamFriendDao()) {
            find(friendId)?.copy(nickname = nickname)?.also { update(it) }
        }
    }

    suspend fun requestAliasHistory(friendId: Long) = withContext(Dispatchers.IO) {
        val friend = friendId.toSteamID()
        val aliasList = steamFriends.requestAliasHistory(friend).await()
            .responses
            .flatMap { response -> response.names }
            .map { name -> name.name }
            .toPersistentList()

        with(db.steamFriendDao()) {
            find(friendId)?.copy(aliases = aliasList)?.also {
                update(it)
            }
        }
    }

    suspend fun getProfileInfo(friendID: SteamID): ProfileInfoCallback =
        withContext(Dispatchers.IO) {
            steamFriends.requestProfileInfo(friendID).await()
        }
}
