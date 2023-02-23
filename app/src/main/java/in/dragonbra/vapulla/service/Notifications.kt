package `in`.dragonbra.vapulla.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat

object Notifications {

    @RequiresApi(Build.VERSION_CODES.O)
    fun createServiceNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-service",
            "Vapulla Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createRequestNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-friend-request",
            "Vapulla Friend Requests",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createMessagesNotificationChannel(notificationManager: NotificationManagerCompat) {
        val notificationChannel = NotificationChannel(
            "vapulla-messages",
            "Vapulla Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }
}
