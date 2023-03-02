package `in`.dragonbra.vapulla

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import timber.log.Timber

@HiltAndroidApp
class VapullaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        LogManager.addListener(object : LogListener {
            override fun onLog(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.d("${clazz?.simpleName ?: "Unknown Class"} + $message")
            }

            override fun onError(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.d("${clazz?.simpleName ?: "Unknown Class"} + $message")
            }
        })
    }
}
