package `in`.dragonbra.vapulla.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.spongycastle.util.encoders.Hex
import java.util.concurrent.TimeUnit

private var remoteInput: RemoteInput =
    RemoteInput.Builder(KEY_TEXT_REPLY)
        .setLabel("Reply")
        .build()

private val flagUpdateCurrent =
    if (Utils.isGreaterThanM)
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    else
        PendingIntent.FLAG_UPDATE_CURRENT

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

inline fun Context.serviceNotification(
    text: String,
    block: (builder: NotificationCompat.Builder) -> Unit
) {
    val logOutIntent = Intent(this, LogOutReceiver::class.java)
    val pendingIntent = PendingIntent.getActivity(
        applicationContext,
        0,
        logOutIntent,
        if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val homeIntent = Intent(this, HomeActivity::class.java)
    val contentIntent = PendingIntent.getActivity(
        this,
        0,
        homeIntent,
        if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
    )

    // Note: DI now
    val builder = NotificationCompat.Builder(this, "vapulla-service")
        .setDefaults(0)
        .setShowWhen(false)
        .setContentTitle("Vapulla")
        .setContentText(text)
        .setContentIntent(contentIntent)
        .setSmallIcon(R.drawable.ic_vapulla)
        .setVibrate(longArrayOf(-1L))
        .setSound(null)
        .addAction(
            R.drawable.ic_exit_to_app,
            getString(R.string.notificationActionLogOut),
            pendingIntent
        )

    @Suppress("DEPRECATION")
    builder.priority =
        if (Utils.isAtLeastN) NotificationManager.IMPORTANCE_LOW else Notification.PRIORITY_LOW

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

    val bitmap: Bitmap = withContext(Dispatchers.IO) {
        Glide.with(applicationContext)
            .asBitmap()
            .load(Utils.getAvatarUrl(friend.avatar))
            .apply(Utils.avatarOptions)
            .submit()
            .get(5, TimeUnit.SECONDS)
    }

    val iconBitmap = IconCompat.createWithBitmap(bitmap)
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
            PendingIntent.FLAG_UPDATE_CURRENT
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

    var bitmap = withContext(Dispatchers.IO) {
        Glide.with(applicationContext)
            .asBitmap()
            .load(Utils.getAvatarUrl(Hex.toHexString(state.avatarHash)))
            .apply(Utils.avatarOptions)
            .submit()
            .get(5, TimeUnit.SECONDS)
    }

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
                if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
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
