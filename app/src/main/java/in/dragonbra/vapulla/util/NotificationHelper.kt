package `in`.dragonbra.vapulla.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import `in`.dragonbra.vapulla.R

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
        title: String,
        text: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND_SERVICE)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vapulla)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateServiceNotification(
        context: Context,
        title: String,
        text: String
    ) {
        val notification = createServiceNotification(context, title, text)
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

    fun sendFriendRequestNotification(
        context: Context,
        friendName: String,
        notificationId: Int
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUESTS)
            .setContentTitle("Friend Request")
            .setContentText("$friendName sent you a friend request")
            .setSmallIcon(R.drawable.ic_add_friend)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}