package `in`.dragonbra.vapulla.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import `in`.dragonbra.vapulla.R
import kotlinx.coroutines.withTimeoutOrNull
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.toBitmap
import coil3.transform.CircleCropTransformation
import `in`.dragonbra.vapulla.MainActivity
import `in`.dragonbra.vapulla.broadcastreceiver.AcceptRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.BlockRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.IgnoreRequestReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object NotificationHelper {

    const val CHANNEL_FRIEND_MESSAGES = "friend_messages"
    const val CHANNEL_FRIEND_REQUESTS = "friend_requests"
    const val CHANNEL_FOREGROUND_SERVICE = "foreground_service"

    const val NOTIFICATION_ID_SERVICE = 1

    fun createNotificationChannels(context: Context) {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_FRIEND_MESSAGES,
                "Friend Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Messages from friends"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },

            NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friend requests"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },

            NotificationChannel(
                CHANNEL_FOREGROUND_SERVICE,
                "Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that Vapulla is running"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun createServiceNotification(
        context: Context,
        text: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vapulla)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun updateServiceNotification(
        context: Context,
        text: String,
    ) {
        val notification = createServiceNotification(context, text)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_SERVICE, notification)
    }

    fun sendFriendMessageNotification(
        context: Context,
        friendName: String,
        message: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_MESSAGES)
            .setContentTitle(friendName)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    suspend fun sendFriendRequestNotification(
        context: Context,
        friendId: Long,
        friendName: String,
        avatarUrl: String? = null
    ) {
        val notificationId = friendId.toInt()

        // Load avatar bitmap using Coil
        val avatarBitmap = avatarUrl?.let { url ->
            withTimeoutOrNull(5000L) {
                loadAvatarBitmap(context, url)
            }
        }

        // Create pending intents for actions
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            context.intentFor<AcceptRequestReceiver>(
                AcceptRequestReceiver.EXTRA_ID to friendId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            context.intentFor<IgnoreRequestReceiver>(
                IgnoreRequestReceiver.EXTRA_ID to friendId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            context.intentFor<BlockRequestReceiver>(
                BlockRequestReceiver.EXTRA_ID to friendId
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create content intent to open the app
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            context.intentFor<MainActivity>(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUESTS)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_add_friend)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(context.getString(R.string.notificationTitleFriendRequest))
            .setContentText(context.getString(R.string.notificationMessageFriendRequest, friendName))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notificationActionAccept),
                acceptPendingIntent
            )
            .addAction(
                R.drawable.ic_close,
                context.getString(R.string.notificationActionIgnore),
                ignorePendingIntent
            )
            .addAction(
                R.drawable.ic_block,
                context.getString(R.string.notificationActionBlock),
                blockPendingIntent
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private suspend fun loadAvatarBitmap(
        context: Context,
        avatarUrl: String
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = ImageLoader.Builder(context).build()
            val request = ImageRequest.Builder(context)
                .data(avatarUrl)
                .transformations(CircleCropTransformation())
                .size(200) // Adjust size as needed for notification
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                result.image.toBitmap()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }
}