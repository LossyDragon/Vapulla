package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetMutualFriendsForIncomingInvites_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.util.SteamTimeoutException
import `in`.dragonbra.vapulla.util.helpers.accountIdToSteamID
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * The things the user does to a friend or to their own presence, via the [SteamFriends]
 * handler. Keeping the friends table in step with what Steam pushes is
 * [in.dragonbra.vapulla.service.sync.FriendsSync]'s job, so what is stored here is written
 * one column at a time and cannot undo one of its updates.
 */
class FriendsApi(
    private val db: VapullaDatabase,
    private val steamFriends: SteamFriends,
    private val player: Player,
) {

    /** Shows the account to its friends as [state]: online, away, invisible and so on. */
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

        val body = player.getMutualFriendsForIncomingInvites(request)
            .bodyOrNull("GetMutualFriendsForIncomingInvites")
            ?: return emptyMap()

        return body.incomingInviteMutualFriendsListsList.associate { invite ->
            invite.steamid to invite.mutualFriendAccountIdsList.map { accountId ->
                accountId.accountIdToSteamID().convertToUInt64()
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

    /**
     * Blocks [friendId], or unblocks them when they already are. Nothing is stored here:
     * Steam follows its answer with a friends list update carrying the new relationship,
     * which [in.dragonbra.vapulla.service.sync.FriendsSync] writes to the database.
     */
    suspend fun toggleBlocked(friendId: Long) {
        val friend = db.steamFriendDao().find(friendId) ?: return
        val block = !friend.isBlocked
        Timber.d("toggleBlocked() id=%d relation=%s block=%b", friendId, friend.relation, block)

        val callback = steamFriends.ignoreFriend(friendId.toSteamID(), setIgnore = block)
            .awaitOrNull("IgnoreFriend")
        Timber.d("toggleBlocked() id=%d result=%s", friendId, callback?.result)
    }

    /**
     * Ends the friendship with [friendId]. The row is removed at once rather than when Steam
     * announces the change, so the friend is gone from the list as soon as it is asked for.
     */
    suspend fun removeFriend(friendId: Long) {
        steamFriends.removeFriend(friendId.toSteamID())
        db.steamFriendDao().remove(friendId)
    }

    /**
     * Sets the nickname of [friendId]; an empty [value] removes it.
     *
     * The nickname is stored once Steam has accepted it, so the change shows without waiting
     * for the announcement Steam makes to every signed-in device, which
     * [in.dragonbra.vapulla.service.sync.FriendsSync] writes as well.
     *
     * @return Steam's answer; `Timeout` when there is none. Nothing is stored unless it is `OK`.
     */
    suspend fun setNickname(friendId: Long, value: String): EResult {
        val nickname = value.trim()
        val result = steamFriends.setFriendNickname(friendId.toSteamID(), nickname)
            .awaitOrNull("SetFriendNickname")
            ?.result
            ?: return EResult.Timeout

        if (result == EResult.OK) {
            db.steamFriendDao().setNickname(friendId, nickname)
        } else {
            Timber.w("SetFriendNickname answered $result")
        }
        return result
    }

    /**
     * Asks Steam for the profile names [friendId] went by before and stores them with the
     * friend. Nothing is stored when Steam does not answer.
     */
    suspend fun loadAliases(friendId: Long) {
        val callback = steamFriends.requestAliasHistory(friendId.toSteamID())
            .awaitOrNull("AliasHistory")
            ?: return

        val aliases = callback.responses
            .flatMap { response -> response.names }
            .map { name -> name.name }
            .toImmutableList()

        db.steamFriendDao().setAliases(friendId, aliases)
    }

    /**
     * The profile page details of anyone, friend or not: real name, country, summary.
     *
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun profileInfo(friendId: SteamID): ProfileInfoCallback =
        steamFriends.requestProfileInfo(friendId).awaitOrThrow()
}
