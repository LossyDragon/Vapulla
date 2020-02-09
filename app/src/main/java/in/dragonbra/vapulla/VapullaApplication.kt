package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.component.DaggerVapullaComponent
import `in`.dragonbra.vapulla.component.VapullaComponent
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.StorageModule
import `in`.dragonbra.vapulla.util.Utils.isGreaterThanO
import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics

class VapullaApplication : Application() {

    lateinit var graph: VapullaComponent

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        LogManager.addListener { clazz, message, throwable ->
            Log.d(clazz.simpleName, message, throwable)
        }

        if (isGreaterThanO()) {
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

        graph = DaggerVapullaComponent.builder()
                .appModule(AppModule(this))
                .storageModule(StorageModule())
                .presenterModule(PresenterModule())
                .build()

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)
    }
}
