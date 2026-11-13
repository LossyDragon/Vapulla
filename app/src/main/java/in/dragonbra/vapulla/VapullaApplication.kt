package `in`.dragonbra.vapulla

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.di.AppModules
import `in`.dragonbra.vapulla.di.DatabaseModules
import `in`.dragonbra.vapulla.di.ViewModelModules
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.NetworkLog
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.ProcessExits
import `in`.dragonbra.vapulla.util.ReleaseTree
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import `in`.dragonbra.vapulla.util.decoders.IconDecoder
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

/**
 * Sets up logging, JavaSteam log forwarding, notification channels and Koin, and supplies
 * the image loader.
 */
class VapullaApplication :
    Application(),
    ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        Timber.plant(if (BuildConfig.DEBUG) Timber.DebugTree() else ReleaseTree())

        logPreviousExits()

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

        // The network log is a plain object, so its settings are pushed in rather than injected.
        appScope.launch {
            get<SettingsManager>().settings.collect {
                NetworkLog.maxEntries = it.networkLogSize
                NetworkLog.keepBodies = it.networkLogBodies
            }
        }
    }

    /**
     * The Coil loader behind every image in the app, notifications included: a small memory
     * and disk cache plus decoders for Steam's `.ico` client icons and animated PNG avatars.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.1)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .maxSizePercent(0.03)
                .directory(cacheDir.resolve("image_cache").toOkioPath())
                .build()
        }
        .components {
            add(IconDecoder.Factory())
            add(AnimatedPngDecoder.Factory())
        }
        .build()

    /**
     * Logs mistakes that do not crash but cost stability: slow work on the main thread and
     * resources that are never released. Violations only go to logcat, since many come from
     * libraries and cannot be fixed here.
     */
    private fun enableStrictMode() {
        // Only watches the thread it is set on, so the Steam and download threads are exempt.
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )

        // Picked one by one: detectAll() also reports every socket JavaSteam opens.
        val vmPolicy = StrictMode.VmPolicy.Builder()
            .detectLeakedClosableObjects()
            .detectLeakedSqlLiteObjects()
            .detectActivityLeaks()
            .detectLeakedRegistrationObjects()
            .penaltyLog()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vmPolicy.detectUnsafeIntentLaunch().detectIncorrectContextUse()
        }

        StrictMode.setVmPolicy(vmPolicy.build())
    }

    /** Logs how the last few runs of the app ended; see [ProcessExits]. */
    private fun logPreviousExits() {
        ProcessExits.recent(this, EXITS_TO_LOG).forEach { exit ->
            val detail = exit.detail.takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
            val line = "Earlier run ended at ${Date(exit.timestamp)}: ${exit.reason}$detail"

            if (exit.abnormal) Timber.w(line) else Timber.i(line)
        }
    }

    private companion object {
        const val EXITS_TO_LOG = 3
    }
}
