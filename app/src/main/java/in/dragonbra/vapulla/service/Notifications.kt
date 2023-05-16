package `in`.dragonbra.vapulla.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import `in`.dragonbra.vapulla.core.Constants

object Notifications {

    fun NotificationManagerCompat.createChannels() {
        if (Constants.isAtLeastO) {
            createMessagesNotificationChannel(this)
            createRequestNotificationChannel(this)
            createServiceNotificationChannel(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createServiceNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-service",
            "Vapulla Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRequestNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-friend-request",
            "Vapulla Friend Requests",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMessagesNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-messages",
            "Vapulla Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }
}
