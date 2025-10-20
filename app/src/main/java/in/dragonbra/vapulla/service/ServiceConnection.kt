package `in`.dragonbra.vapulla.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

sealed class LoginResult {
    object Loading : LoginResult()
    object Success : LoginResult()
    object QRCodeEnded : LoginResult()
    data class Error(var error: String) : LoginResult()
    data class QRCode(val qrCode: String) : LoginResult()
}

class ServiceConnection(
    private val context: Context
) {
    var steamService: SteamService? = null // TODO maybe interface some methods.
        private set

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as SteamService.ServiceBinder
            steamService = localBinder.service
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
        if (!isBound.value) {
            val intent = Intent(context, SteamService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unBindService() {
        if (isBound.value) {
            context.unbindService(serviceConnection)
            _isBound.value = false
            steamService = null
        }
    }

    fun startForegroundService() {
        val intent = Intent(context, SteamService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopForegroundService() {
        val intent = Intent(context, SteamService::class.java)
        context.stopService(intent)
    }
}