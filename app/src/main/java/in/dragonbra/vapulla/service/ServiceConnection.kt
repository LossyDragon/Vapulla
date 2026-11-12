package `in`.dragonbra.vapulla.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import `in`.dragonbra.javasteam.enums.EPersonaState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * App-wide handle to the bound [SteamService]. Injected as a singleton so the activity binds
 * once and view models reach the service through [steamService].
 */
class ServiceConnection(private val context: Context) {

    /** The bound service, or null while unbound. */
    var steamService: SteamService? = null
        private set

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            steamService = (binder as SteamService.ServiceBinder).service
            _isBound.value = true
            Timber.d("Service connected")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            steamService = null
            _isBound.value = false
            Timber.d("Service disconnected")
        }
    }

    fun bindService() {
        if (isBound.value) return
        val intent = Intent(context, SteamService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unBindService() {
        if (!isBound.value) return
        context.unbindService(serviceConnection)
        _isBound.value = false
        steamService = null
    }

    fun startForegroundService() {
        ContextCompat.startForegroundService(context, Intent(context, SteamService::class.java))
    }

    fun stopForegroundService() {
        context.stopService(Intent(context, SteamService::class.java))
    }

    fun setPersonaState(state: EPersonaState) {
        val service = checkNotNull(steamService) { "SteamService is not bound" }
        service.apis?.friends?.setPersonaState(state)
    }
}
