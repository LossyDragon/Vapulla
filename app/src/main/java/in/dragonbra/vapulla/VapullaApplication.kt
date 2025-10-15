package `in`.dragonbra.vapulla

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import androidx.preference.PreferenceManager
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.component.DaggerVapullaComponent
import `in`.dragonbra.vapulla.component.VapullaComponent
import `in`.dragonbra.vapulla.di.appModule
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.StorageModule
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.ReleaseTree
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class VapullaApplication : Application() {

    lateinit var graph: VapullaComponent

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        // Application debugging & logging
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects() // Detect when Closeable objects are not properly closed
                    .penaltyLog() // Log violations to logcat
                    .build(),
            )

            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // Detect all violations (disk reads/writes, network operations, etc.)
                    .penaltyLog() // Log violations to logcat
                    .build(),
            )

            val tree = Timber.DebugTree()
            Timber.plant(tree = tree)
        } else {
            val tree = ReleaseTree()
            Timber.plant(tree = tree)
        }

        // JavaSteam logging
        val listener = object : LogListener {
            override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable?) {
                Timber.tag(tag = clazz.simpleName).i(t = throwable, message = message)
            }

            override fun onError(clazz: Class<*>, message: String?, throwable: Throwable?) {
                Timber.tag(tag = clazz.simpleName).e(t = throwable, message = message)
            }
        }
        LogManager.addListener(listener)

        // Create notification channels
        NotificationHelper.createNotificationChannels(this)

        startKoin {
            androidContext(androidContext = this@VapullaApplication)
            modules(modules = appModule)
        }

        graph = DaggerVapullaComponent.builder()
            .appModule(AppModule(this))
            .storageModule(StorageModule())
            .presenterModule(PresenterModule())
            .build()

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)
    }
}