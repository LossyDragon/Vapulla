package `in`.dragonbra.vapulla

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.di.AppModules
import `in`.dragonbra.vapulla.di.DatabaseModules
import `in`.dragonbra.vapulla.di.ViewModelModules
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.NetworkLog
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.ReleaseTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/** Sets up logging, JavaSteam log forwarding, notification channels and Koin. */
class VapullaApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())

        // Route JavaSteam's own logging through Timber, tagged with the originating class.
        LogManager.addListener(
            object : LogListener {
                override fun onLog(clazz: Class<*>, message: String?, throwable: Throwable?) {
                    Timber.tag(clazz.simpleName).i(throwable, message)
                }

                override fun onError(clazz: Class<*>, message: String?, throwable: Throwable?) {
                    Timber.tag(clazz.simpleName).e(throwable, message)
                }
            },
        )

        NotificationHelper.createNotificationChannels(this)

        startKoin {
            androidContext(this@VapullaApplication)
            modules(AppModules, ViewModelModules, DatabaseModules)
        }

        // The network log is a plain object, so its size is pushed in rather than injected.
        appScope.launch {
            get<SettingsManager>().settings.collect { NetworkLog.maxEntries = it.networkLogSize }
        }
    }
}
