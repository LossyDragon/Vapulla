package `in`.dragonbra.vapulla.service.api

import android.net.Uri
import `in`.dragonbra.javasteam.steam.authentication.SteamAuthentication
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.util.runSuspendCatching
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The account's access to Steam's websites. They do not take the CM connection's session
 * but an access token, generated from the saved refresh token with
 * [SteamAuthentication.generateAccessTokenForApp]. Chat media uploads send it along with the
 * upload; a web page takes it as its sign-in cookie.
 */
class WebSessionApi(
    private val authentication: SteamAuthentication,
    private val account: AccountManager,
    private val localSteamId: () -> SteamID?,
) {

    private val tokenMutex = Mutex()
    private var accessToken: String? = null

    /**
     * An access token: the one kept from last time, or a new one when there is none or
     * [fresh] is asked for. A kept token may have expired, which only shows when it is used.
     *
     * @throws IOException if not signed in, or Steam does not issue a token.
     */
    suspend fun accessToken(fresh: Boolean = false): String = tokenMutex.withLock {
        accessToken?.takeUnless { fresh } ?: run {
            val steamId = localSteamId() ?: throw IOException("Not signed in")
            val refreshToken = account.refreshToken.first()
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("No saved sign-in to make an access token from")

            // Whatever goes wrong, a refusal or no answer at all, is an IOException to callers.
            runSuspendCatching {
                authentication.generateAccessTokenForApp(steamId, refreshToken).await().accessToken
            }
                .getOrElse { throw IOException("Steam did not issue an access token", it) }
                .also { accessToken = it }
        }
    }

    /**
     * The value of the `steamLoginSecure` cookie that signs a Steam web page in: the Steam
     * ID and a new access token.
     *
     * @throws IOException if not signed in, or Steam does not issue a token.
     */
    suspend fun signInCookie(): String {
        val token = accessToken(fresh = true)
        val steamId = localSteamId()?.convertToUInt64() ?: throw IOException("Not signed in")

        return Uri.encode("$steamId||$token")
    }
}
