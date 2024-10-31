package `in`.dragonbra.vapulla.di

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import `in`.dragonbra.vapulla.R

@Module
@InstallIn(ServiceComponent::class)
object NotificationModule {

    @ServiceScoped
    fun provideServiceNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, "vapulla-service")
        .setContentTitle("Vapulla")
        .setDefaults(0)
        .setPriority(NotificationManager.IMPORTANCE_LOW)
        .setShowWhen(false)
        .setSmallIcon(R.drawable.ic_vapulla)
        .setSound(null)
        .setVibrate(longArrayOf(-1L))

    @ServiceScoped
    fun provideMessageNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, "vapulla-message")
        .setAutoCancel(true)
        .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSmallIcon(R.drawable.ic_message)

    @ServiceScoped
    fun provideRequestNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, "vapulla-friend-request")
        .setAutoCancel(true)
        .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSmallIcon(R.drawable.ic_add_friend)
}
