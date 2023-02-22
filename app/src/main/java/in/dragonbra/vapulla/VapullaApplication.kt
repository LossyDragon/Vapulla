package `in`.dragonbra.vapulla

import `in`.dragonbra.javasteam.util.log.LogManager
import android.app.Application
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import `in`.dragonbra.javasteam.util.log.LogListener
import timber.log.Timber

@HiltAndroidApp
class VapullaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false)
        Timber.plant(Timber.DebugTree())
        LogManager.addListener(object : LogListener {
            override fun onLog(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.tag(clazz?.simpleName ?: "Unknown Class").d(throwable, message)
            }

            override fun onError(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.tag(clazz?.simpleName ?: "Unknown Class").e(throwable, message)
            }
        })
    }
}
