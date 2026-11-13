package `in`.dragonbra.vapulla

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
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

        // The network log is a plain object, so its size is pushed in rather than injected.
        appScope.launch {
            get<SettingsManager>().settings.collect { NetworkLog.maxEntries = it.networkLogSize }
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

    /**
     * Logs how the last few runs of the app ended. A run that the system killed, or that
     * froze, leaves no trace of its own, so this is the only place it shows up.
     */
    private fun logPreviousExits() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val exits = runCatching {
            getSystemService(ActivityManager::class.java)
                .getHistoricalProcessExitReasons(null, 0, EXITS_TO_LOG)
        }.getOrNull().orEmpty()

        exits.forEach { exit ->
            val line = "Earlier run ended at ${Date(exit.timestamp)}: ${exitReason(exit.reason)}" +
                exit.description?.let { " ($it)" }.orEmpty()

            if (exit.reason in ABNORMAL_EXITS) Timber.w(line) else Timber.i(line)
        }
    }

    private fun exitReason(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_ANR -> "froze (ANR)"
        ApplicationExitInfo.REASON_CRASH -> "crashed"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "crashed in native code"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "killed for memory"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "killed for using too many resources"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "failed to start"
        ApplicationExitInfo.REASON_SIGNALED -> "killed by a signal"
        ApplicationExitInfo.REASON_EXIT_SELF -> "exited by itself"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "stopped by the user"
        ApplicationExitInfo.REASON_USER_STOPPED -> "stopped with its user"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "killed by a permission change"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "killed with a process it depended on"
        ApplicationExitInfo.REASON_FREEZER -> "killed while frozen in the background"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "stopped by a change to the app's state"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "stopped to install an update"
        ApplicationExitInfo.REASON_OTHER -> "killed by the system"
        else -> "unknown reason $reason"
    }

    private companion object {
        const val EXITS_TO_LOG = 3

        val ABNORMAL_EXITS = setOf(
            ApplicationExitInfo.REASON_ANR,
            ApplicationExitInfo.REASON_CRASH,
            ApplicationExitInfo.REASON_CRASH_NATIVE,
            ApplicationExitInfo.REASON_LOW_MEMORY,
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE,
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE,
        )
    }
}
