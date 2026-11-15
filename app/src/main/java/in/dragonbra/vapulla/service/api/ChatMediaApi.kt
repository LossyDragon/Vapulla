package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.types.SteamID
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Images and videos in chat, via [SteamCloud]'s chat media upload. Steam posts the media to
 * the chat itself once it is uploaded, so nothing else has to be sent; the message then
 * arrives like any other.
 *
 * The upload goes through the community website rather than the CM connection, which wants
 * an access token; [WebSessionApi] makes one and keeps it for later uploads.
 */
class ChatMediaApi(private val steamCloud: SteamCloud, private val webSession: WebSessionApi) {

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
            val result = try {
                request(webSession.accessToken())
            } catch (e: IOException) {
                Timber.w(e, "Upload failed, trying once more with a new access token")
                request(webSession.accessToken(fresh = true))
            }

            Timber.d("Chat media uploaded: $result")
            result
        }
}
