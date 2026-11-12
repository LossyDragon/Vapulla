package `in`.dragonbra.vapulla.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import `in`.dragonbra.vapulla.MainActivity
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.broadcastreceiver.AcceptRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.BlockRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.FriendRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.IgnoreRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** Notification channels and the notifications the app posts. */
object NotificationHelper {

    const val CHANNEL_FRIEND_MESSAGES = "friend_messages"
    const val CHANNEL_FRIEND_REQUESTS = "friend_requests"
    const val CHANNEL_FOREGROUND_SERVICE = "foreground_service"

    const val NOTIFICATION_ID_SERVICE = 1

    /** Keeps chat notifications apart from friend requests, which share the friend's ID. */
    private const val TAG_FRIEND_MESSAGE = "friend_message"

    private const val AVATAR_TIMEOUT_MS = 5_000L
    private const val AVATAR_SIZE_PX = 200

    fun createNotificationChannels(context: Context) {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_FRIEND_MESSAGES,
                "Friend Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Messages from friends"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Friend requests"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows that Vapulla is running"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            },
        )

        channels.forEach { context.notificationManager.createNotificationChannel(it) }
    }

    /** The persistent notification the foreground service runs under; tapping it opens the app. */
    fun createServiceNotification(context: Context, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vapulla)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    // The launcher's own intent, so an app that is already open is brought
                    // back as it was instead of getting a second activity on top.
                    context.packageManager.getLaunchIntentForPackage(context.packageName)
                        ?: Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun updateServiceNotification(context: Context, text: String) {
        context.notificationManager.notify(
            NOTIFICATION_ID_SERVICE,
            createServiceNotification(context, text),
        )
    }

    /**
     * Posts a chat message from a friend with an inline Reply action. Messages from the same
     * friend are collected into one conversation notification until it is cleared.
     */
    suspend fun sendFriendMessageNotification(
        context: Context,
        friendId: Long,
        friendName: String,
        message: String,
        avatarUrl: String? = null,
    ) {
        val notificationId = friendId.toInt()

        val avatarBitmap = avatarUrl?.let { url ->
            withTimeoutOrNull(AVATAR_TIMEOUT_MS) { loadAvatarBitmap(context, url) }
        }

        val sender = Person.Builder()
            .setName(friendName)
            .setKey(friendId.toString())
            .setIcon(avatarBitmap?.let(IconCompat::createWithBitmap))
            .build()

        // Carry on the conversation already shown for this friend, if there is one.
        val style = context.notificationManager.activeNotifications
            .firstOrNull { it.tag == TAG_FRIEND_MESSAGE && it.id == notificationId }
            ?.let {
                NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                    it.notification,
                )
            }
            ?: NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())

        style.addMessage(message, System.currentTimeMillis(), sender)

        // Tapping opens this friend's chat. The request code is per friend, otherwise every
        // message notification would share one intent and open the same chat.
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            MainActivity.openChatIntent(context, friendId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Mutable, because the system writes the typed reply into the intent.
        val replyIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, ReplyReceiver::class.java).putExtra(ReplyReceiver.EXTRA_ID, friendId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )

        val replyAction = NotificationCompat.Action
            .Builder(R.drawable.ic_message, "Reply", replyIntent)
            .addRemoteInput(
                RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY).setLabel("Reply").build(),
            )
            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
            .setShowsUserInterface(false)
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_MESSAGES)
            .setStyle(style)
            .setSmallIcon(R.drawable.ic_message)
            .setLargeIcon(avatarBitmap)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(replyAction)
            .build()

        context.notificationManager.notify(TAG_FRIEND_MESSAGE, notificationId, notification)
    }

    /** Removes the chat notification for a friend, e.g. once their chat has been read. */
    fun cancelFriendMessageNotification(context: Context, friendId: Long) {
        context.notificationManager.cancel(TAG_FRIEND_MESSAGE, friendId.toInt())
    }

    /**
     * Posts a friend request with Accept, Ignore and Block actions. The sender's avatar is
     * loaded for the large icon when it arrives within [AVATAR_TIMEOUT_MS].
     */
    suspend fun sendFriendRequestNotification(
        context: Context,
        friendId: Long,
        friendName: String,
        avatarUrl: String? = null,
    ) {
        val notificationId = friendId.toInt()

        val avatarBitmap = avatarUrl?.let { url ->
            withTimeoutOrNull(AVATAR_TIMEOUT_MS) { loadAvatarBitmap(context, url) }
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUESTS)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_add_friend)
            .setLargeIcon(avatarBitmap)
            .setContentTitle("New friend request")
            .setContentText("$friendName has added to their friends list!")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_check,
                "Accept",
                requestActionIntent<AcceptRequestReceiver>(context, friendId, notificationId),
            )
            .addAction(
                R.drawable.ic_close,
                "Ignore",
                requestActionIntent<IgnoreRequestReceiver>(context, friendId, notificationId),
            )
            .addAction(
                R.drawable.ic_block,
                "Block",
                requestActionIntent<BlockRequestReceiver>(context, friendId, notificationId),
            )
            .build()

        context.notificationManager.notify(notificationId, notification)
    }

    /** Broadcast to one of the [FriendRequestReceiver] subclasses carrying the friend's ID. */
    private inline fun <reified R : FriendRequestReceiver> requestActionIntent(
        context: Context,
        friendId: Long,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, R::class.java).putExtra(FriendRequestReceiver.EXTRA_ID, friendId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private suspend fun loadAvatarBitmap(context: Context, avatarUrl: String): Bitmap? = try {
        val request = ImageRequest.Builder(context)
            .data(avatarUrl)
            .transformations(CircleCropTransformation())
            .size(AVATAR_SIZE_PX)
            .build()

        (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
    } catch (e: Exception) {
        Timber.e(e, "Failed to load avatar for notification")
        null
    }

    private val Context.notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)
}
