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
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import `in`.dragonbra.vapulla.MainActivity
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.broadcastreceiver.AcceptRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.BlockRequestReceiver
import `in`.dragonbra.vapulla.broadcastreceiver.IgnoreRequestReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

        val manager = context.getSystemService(NotificationManager::class.java)
        channels.forEach { manager.createNotificationChannel(it) }
    }

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
        val notification = createServiceNotification(context, text)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_SERVICE, notification)
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

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    suspend fun sendFriendRequestNotification(
        context: Context,
        friendId: Long,
        friendName: String,
        avatarUrl: String? = null,
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
            Intent(context, AcceptRequestReceiver::class.java).apply {
                putExtra(AcceptRequestReceiver.EXTRA_ID, friendId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val ignorePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, IgnoreRequestReceiver::class.java).apply {
                putExtra(IgnoreRequestReceiver.EXTRA_ID, friendId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, BlockRequestReceiver::class.java).apply {
                putExtra(BlockRequestReceiver.EXTRA_ID, friendId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Create content intent to open the app
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Build notification
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
                acceptPendingIntent,
            )
            .addAction(
                R.drawable.ic_close,
                "Ignore",
                ignorePendingIntent,
            )
            .addAction(
                R.drawable.ic_block,
                "Block",
                blockPendingIntent,
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    private suspend fun loadAvatarBitmap(context: Context, avatarUrl: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = ImageLoader.Builder(context).build()
                val request = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .transformations(CircleCropTransformation())
                    .size(200)
                    .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    result.drawable.toBitmap()
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }
}
