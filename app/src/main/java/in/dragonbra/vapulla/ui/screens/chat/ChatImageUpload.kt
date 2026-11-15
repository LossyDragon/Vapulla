package `in`.dragonbra.vapulla.ui.screens.chat

import android.content.Context
import android.net.Uri
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.ChatMediaApi
import `in`.dragonbra.vapulla.service.requestMessage
import `in`.dragonbra.vapulla.util.ChatMedia
import `in`.dragonbra.vapulla.util.readChatMedia

/**
 * Reads the image picked for a chat and uploads it with [send]. Friend and group chats share
 * this; they differ only in where the image goes.
 *
 * @return why the image was not sent, or null when it was.
 */
internal suspend fun SteamServiceConnection.sendChatImage(
    context: Context,
    uri: Uri,
    send: suspend (ChatMediaApi, ChatMedia) -> Unit,
): String? = request("Image upload") { apis -> send(apis.media, context.readChatMedia(uri)) }
    .exceptionOrNull()
    ?.let { "Image not sent: ${it.requestMessage}" }
