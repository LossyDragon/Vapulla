package `in`.dragonbra.vapulla.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** Notification channels and the notifications the app posts. */
object NotificationHelper {

    const val CHANNEL_FRIEND_MESSAGES = "friend_messages"
    const val CHANNEL_FRIEND_REQUESTS = "friend_requests"
    const val CHANNEL_FOREGROUND_SERVICE = "foreground_service"

    const val NOTIFICATION_ID_SERVICE = 1

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

    /** The persistent notification the foreground service runs under. */
    fun createServiceNotification(context: Context, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vapulla)
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

    fun sendFriendMessageNotification(
        context: Context,
        friendName: String,
        message: String,
        notificationId: Int,
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_MESSAGES)
            .setContentTitle(friendName)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        context.notificationManager.notify(notificationId, notification)
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
