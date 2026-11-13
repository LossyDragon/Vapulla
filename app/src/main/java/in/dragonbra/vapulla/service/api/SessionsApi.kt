package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.CAuthentication_RefreshToken_Enumerate_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.CAuthentication_RefreshToken_Revoke_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.EAuthTokenPlatformType
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.EAuthTokenRevokeAction
import `in`.dragonbra.javasteam.rpc.service.Authentication
import `in`.dragonbra.javasteam.types.SteamID
import timber.log.Timber

/** Signed-in devices via the `Authentication` unified service. */
class SessionsApi(
    private val authentication: Authentication,
    private val localSteamId: () -> SteamID?,
) {
    /** Every refresh token the account currently holds, newest activity first. */
    suspend fun sessions(): List<AuthSession> {
        val request = CAuthentication_RefreshToken_Enumerate_Request.newBuilder()
            .setIncludeRevoked(false)
            .build()

        val response = authentication.enumerateTokens(request).await()
        if (response.result != EResult.OK) {
            Timber.w("EnumerateTokens failed: ${response.result}")
            return emptyList()
        }

        val thisToken = response.body.requestingToken
        return response.body.refreshTokensList
            .map { token ->
                AuthSession(
                    tokenId = token.tokenId,
                    description = token.tokenDescription,
                    platform = token.platformType.displayName(),
                    loggedIn = token.loggedIn,
                    isThisDevice = token.tokenId == thisToken,
                    firstSeen = token.firstSeen.time.toLong(),
                    lastSeen = token.lastSeen.time.toLong(),
                    lastSeenCity = token.lastSeen.city,
                    lastSeenCountry = token.lastSeen.country,
                )
            }
            .sortedByDescending { it.lastSeen }
    }

    /** Signs that device out for good. */
    suspend fun revoke(tokenId: Long): EResult {
        val request = CAuthentication_RefreshToken_Revoke_Request.newBuilder()
            .setTokenId(tokenId)
            .setSteamid(localSteamId()?.convertToUInt64() ?: 0L)
            .setRevokeAction(EAuthTokenRevokeAction.k_EAuthTokenRevokePermanent)
            .build()
        return authentication.revokeRefreshToken(request).await().result
    }

    private fun EAuthTokenPlatformType.displayName(): String = when (this) {
        EAuthTokenPlatformType.k_EAuthTokenPlatformType_SteamClient -> "Steam client"
        EAuthTokenPlatformType.k_EAuthTokenPlatformType_WebBrowser -> "Web browser"
        EAuthTokenPlatformType.k_EAuthTokenPlatformType_MobileApp -> "Mobile app"
        else -> "Unknown"
    }
}
