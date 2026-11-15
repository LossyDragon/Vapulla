package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_CreateFriendInviteToken_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_CreateFriendInviteToken_ResponseOrBuilder
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_GetFriendInviteTokens_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_RevokeFriendInviteToken_Request
import `in`.dragonbra.javasteam.rpc.service.UserAccount
import `in`.dragonbra.javasteam.types.SteamID

/**
 * Friend invite links via the `UserAccount` unified service. A link adds whoever opens it as
 * a friend without a request having to be accepted, so each one is made for a single use
 * and can be revoked.
 */
class InvitesApi(private val userAccount: UserAccount, private val localSteamId: () -> SteamID?) {

    /**
     * The account's invite links that can still be used, newest first. Empty when Steam does
     * not answer.
     */
    suspend fun invites(): List<FriendInvite> {
        val accountId = localSteamId()?.accountID ?: return emptyList()
        val request = CUserAccount_GetFriendInviteTokens_Request.newBuilder().build()

        val body = userAccount.getFriendInviteTokens(request).bodyOrNull("GetFriendInviteTokens")
            ?: return emptyList()

        return body.tokensList
            .filter { it.valid }
            .map { it.toInvite(accountId) }
            .sortedByDescending { it.createdAt }
    }

    /** Makes a new single-use link, good for [INVITE_DAYS] days. Null when Steam refuses. */
    suspend fun create(): FriendInvite? {
        val accountId = localSteamId()?.accountID ?: return null
        val request = CUserAccount_CreateFriendInviteToken_Request.newBuilder()
            .setInviteLimit(1)
            .setInviteDuration(INVITE_DAYS * SECONDS_PER_DAY)
            .build()

        return userAccount.createFriendInviteToken(request)
            .bodyOrNull("CreateFriendInviteToken")
            ?.toInvite(accountId)
    }

    /** Makes the link with [token] stop working. */
    suspend fun revoke(token: String): EResult {
        val request = CUserAccount_RevokeFriendInviteToken_Request.newBuilder()
            .setInviteToken(token)
            .build()
        return userAccount.revokeFriendInviteToken(request).resultOf("RevokeFriendInviteToken")
    }

    private fun CUserAccount_CreateFriendInviteToken_ResponseOrBuilder.toInvite(accountId: Long) =
        FriendInvite(
            token = inviteToken,
            link = "$LINK_URL${friendCode(accountId)}/$inviteToken",
            uses = inviteLimit.toInt(),
            durationSeconds = inviteDuration,
            createdAt = timeCreated.toUInt().toLong(),
        )

    /**
     * The account ID the way invite links write it: in hexadecimal, each digit swapped for a
     * consonant so that it cannot spell a word, with a dash in the middle.
     */
    private fun friendCode(accountId: Long): String {
        val code = accountId.toString(HEX).map { CODE_LETTERS[it.digitToInt(HEX)] }.joinToString("")
        val middle = code.length / 2

        return if (code.length > 3) "${code.take(middle)}-${code.drop(middle)}" else code
    }

    private companion object {
        const val LINK_URL = "https://s.team/p/"
        const val CODE_LETTERS = "bcdfghjkmnpqrtvw"
        const val HEX = 16
        const val INVITE_DAYS = 30
        const val SECONDS_PER_DAY = 24 * 60 * 60
    }
}
