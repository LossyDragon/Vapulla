package `in`.dragonbra.vapulla.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.drawable.IconCompat
import coil.Coil
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.broadcastreceiver.AcceptRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.BlockRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.IgnoreRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.home.HomeActivity
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import org.spongycastle.util.encoders.Hex

private var remoteInput: RemoteInput =
    RemoteInput.Builder(KEY_TEXT_REPLY)
        .setLabel("Reply")
        .build()

private val flagUpdateCurrent =
    if (Constants.isAtLeastS) {
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

/**
 * Time to back off when we receive a new message to prevent spam
 */
private const val NEW_MESSAGE_BACKOFF = DateUtils.MINUTE_IN_MILLIS

private fun Context.getMessageReplyIntent(id: Long): Intent {
    val intent = Intent(this, ReplyReceiver::class.java).apply {
        putExtra(ReplyReceiver.EXTRA_ID, id)
    }

    return intent
}

fun Context.serviceNotification(
    text: String,
    block: (builder: NotificationCompat.Builder) -> Unit
) {
    fun logoutIntent(): PendingIntent? {
        val intent = Intent(this, SteamService::class.java).apply {
            putExtra(SteamService.EXTRA_ACTION, "stop")
        }
        return PendingIntent.getService(
            /* context = */ this,
            /* requestCode = */ 0,
            /* intent = */ intent,
            /* flags = */ 0 or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun homeIntent(): PendingIntent? {
        val intent = Intent(this, HomeActivity::class.java)
        return PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ 0,
            /* intent = */ intent,
            /* flags = */ 0 or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Note: DI now
    val builder = NotificationCompat.Builder(this, "vapulla-service")
        .setDefaults(0)
        .setShowWhen(false)
        .setContentTitle("Vapulla")
        .setContentText(text)
        .setContentIntent(homeIntent())
        .setSmallIcon(R.drawable.ic_vapulla)
        .setVibrate(longArrayOf(-1L))
        .setSound(null)
        .addAction(
            R.drawable.ic_exit_to_app,
            getString(R.string.notificationActionLogOut),
            logoutIntent()
        )

    builder.priority = NotificationManager.IMPORTANCE_LOW

    block(builder)
}

suspend fun Context.serviceMessageNotification(
    friendId: SteamID,
    friend: SteamFriend,
    message: String,
    messages: MutableList<NotificationCompat.MessagingStyle.Message>,
    block: (builder: NotificationCompat.Builder) -> Unit
) {
    val currentTs = System.currentTimeMillis()
    val backoff = messages.isNotEmpty() &&
        currentTs < messages[messages.size - 1].timestamp + NEW_MESSAGE_BACKOFF

    var bitmap: Bitmap? = null
    val imageLoader = Coil.imageLoader(this)
    val request = ImageRequest.Builder(this)
        .data(getAvatarUrl(friend.avatar))
        .target { drawable ->
            bitmap = (drawable as BitmapDrawable).bitmap
        }
        .fallback(R.drawable.vapulla)
        .error(R.drawable.vapulla)
        .transformations(CircleCropTransformation())
        .build()
    imageLoader.execute(request)

    val iconBitmap = IconCompat.createWithBitmap(bitmap!!)
    val steamUser = Person
        .Builder()
        .setName(friend.name ?: "")
        .setIcon(iconBitmap)
        .build()

    val newMessage = NotificationCompat.MessagingStyle.Message(message, currentTs, steamUser)
    messages.add(newMessage)

    val style = NotificationCompat.MessagingStyle(steamUser)

    messages.forEach {
        style.addMessage(it)
    }

    val replyPendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        friendId.convertToUInt64().toInt(),
        getMessageReplyIntent(friendId.convertToUInt64()),
        flagUpdateCurrent
    )

    val replyAction = NotificationCompat.Action.Builder(
        R.drawable.ic_send,
        getString(R.string.notificationActionReply),
        replyPendingIntent
    ).addRemoteInput(remoteInput).build()

    val intent = Intent(this, ChatActivity::class.java).apply {
        putExtra(ChatActivity.INTENT_STEAM_ID, friendId.convertToUInt64())
    }

    val pendingIntent = TaskStackBuilder.create(this)
        .addNextIntentWithParentStack(intent)
        .getPendingIntent(
            friendId.convertToUInt64().toInt(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // Note: DI now
    val notification = NotificationCompat.Builder(this, "vapulla-message")
        .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        .setStyle(style)
        .setSmallIcon(R.drawable.ic_message)
        .setLargeIcon(bitmap)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOnlyAlertOnce(backoff)
        .addAction(replyAction)

    block(notification)
}

suspend fun Context.serviceRequestNotification(
    state: PersonaState,
    block: (builder: NotificationCompat.Builder) -> Unit
) {
    val steamId = state.friendID.convertToUInt64().toInt()

    var bitmap: Bitmap? = null
    val imageLoader = Coil.imageLoader(this)
    val request = ImageRequest.Builder(this)
        .data(getAvatarUrl(Hex.toHexString(state.avatarHash)))
        .target { drawable ->
            bitmap = (drawable as BitmapDrawable).bitmap
        }
        .fallback(R.drawable.vapulla)
        .error(R.drawable.vapulla)
        .transformations(CircleCropTransformation())
        .build()
    imageLoader.execute(request)

    val acceptReceiver = Intent(this, AcceptRequestReceiver::class.java).apply {
        putExtra(AcceptRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
    }
    val acceptPendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        steamId,
        acceptReceiver,
        flagUpdateCurrent
    )

    val ignoreReceiver = Intent(this, IgnoreRequestReceiver::class.java).apply {
        putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
    }
    val ignorePendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        steamId,
        ignoreReceiver,
        flagUpdateCurrent
    )

    val blockPendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        steamId,
        Intent(this, BlockRequestReceiver::class.java).apply {
            putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
        },
        flagUpdateCurrent
    )

    // NOTE: DI now
    val notification = NotificationCompat.Builder(this, "vapulla-friend-request")
        .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        .setSmallIcon(R.drawable.ic_add_friend)
        .setLargeIcon(bitmap)
        .setContentText(getString(R.string.notificationMessageFriendRequest, state.name))
        .setContentTitle(getString(R.string.notificationTitleFriendRequest))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, HomeActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            R.drawable.ic_check,
            getString(R.string.notificationActionAccept),
            acceptPendingIntent
        )
        .addAction(
            R.drawable.ic_close,
            getString(R.string.notificationActionIgnore),
            ignorePendingIntent
        )
        .addAction(
            R.drawable.ic_block,
            getString(R.string.notificationActionBlock),
            blockPendingIntent
        )

    block(notification)
}
