package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.util.Utils.isGreaterThanO
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import `in`.dragonbra.javasteam.util.log.LogListener

@HiltAndroidApp
class VapullaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

//        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)


        LogManager.addListener(object : LogListener {
            override fun onLog(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Log.d(clazz?.simpleName, message, throwable)
            }

            override fun onError(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Log.e(clazz?.simpleName, message, throwable)
            }
        })

        if (isGreaterThanO) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val serviceChannel = NotificationChannel(
                "vapulla-service",
                "Vapulla service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                enableVibration(false)
                importance = NotificationManager.IMPORTANCE_LOW
                enableLights(false)
            }

            notificationManager.createNotificationChannel(serviceChannel)

            val friendRequestChannel = NotificationChannel(
                "vapulla-friend-request",
                "Friend request",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                importance = NotificationManager.IMPORTANCE_DEFAULT
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                lightColor = 0xffffffff.toInt()
            }

            notificationManager.createNotificationChannel(friendRequestChannel)

            val messageChannel = NotificationChannel(
                "vapulla-message",
                "New messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                lightColor = 0xffffffff.toInt()
            }

            notificationManager.createNotificationChannel(messageChannel)
        }

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)
    }
}
