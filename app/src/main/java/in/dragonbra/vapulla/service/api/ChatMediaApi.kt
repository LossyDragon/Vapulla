package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.steam.authentication.SteamAuthentication
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.manager.AccountManager
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Images and videos in chat, via [SteamCloud]'s chat media upload. Steam posts the media to
 * the chat itself once it is uploaded, so nothing else has to be sent; the message then
 * arrives like any other.
 *
 * The upload goes through the community website rather than the CM connection, which wants
 * an access token. One is generated from the saved refresh token with
 * [SteamAuthentication.generateAccessTokenForApp] and kept for later uploads.
 */
class ChatMediaApi(
    private val steamCloud: SteamCloud,
    private val authentication: SteamAuthentication,
    private val account: AccountManager,
    private val localSteamId: () -> SteamID?,
) {

    private val tokenMutex = Mutex()
    private var accessToken: String? = null

    /**
     * Uploads [media] to the chat with a friend.
     *
     * @param media at most 10 MiB: a PNG, JPEG or GIF image, or a WebM, MPEG or OGV video.
     * @throws IllegalArgumentException if the media is too large or not a supported format.
     * @throws IOException if a request fails or Steam turns it down.
     */
    suspend fun sendToFriend(friendId: Long, media: ByteArray, fileName: String? = null) {
        upload { token ->
            steamCloud.uploadChatMediaToUser(
                accessToken = token,
                friendSteamId = SteamID(friendId),
                media = media,
                fileName = fileName,
            )
        }
    }

    /** Uploads [media] to a channel of a chat room group; see [sendToFriend] for the limits. */
    suspend fun sendToChatRoom(
        groupId: Long,
        chatId: Long,
        media: ByteArray,
        fileName: String? = null,
    ) {
        upload { token ->
            steamCloud.uploadChatMediaToChatRoom(
                accessToken = token,
                chatGroupId = groupId,
                chatId = chatId,
                media = media,
                fileName = fileName,
            )
        }
    }

    /**
     * Runs [request] with an access token. A kept token may have expired, which the website
     * reports like any other refusal, so a failure with one is retried once with a new token.
     */
    private suspend fun <T> upload(request: suspend (String) -> T): T =
        withContext(Dispatchers.IO) {
            val cached = accessToken

            if (cached != null) {
                try {
                    return@withContext request(cached)
                } catch (e: IOException) {
                    Timber.w(e, "Upload with the kept access token failed, trying a new one")
                    accessToken = null
                }
            }

            val result = request(newAccessToken())
            Timber.d("Chat media uploaded: $result")
            result
        }

    private suspend fun newAccessToken(): String = tokenMutex.withLock {
        accessToken ?: run {
            val steamId = localSteamId() ?: throw IOException("Not signed in")
            val refreshToken = account.refreshToken.first()
                ?.takeIf { it.isNotBlank() }
                ?: throw IOException("No saved sign-in to upload with")

            val token = try {
                authentication.generateAccessTokenForApp(steamId, refreshToken).await().accessToken
            } catch (e: IllegalArgumentException) {
                throw IOException("Steam did not issue an access token", e)
            }

            token.also { accessToken = it }
        }
    }
}
