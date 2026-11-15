package `in`.dragonbra.vapulla.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection as AndroidServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.service.api.SteamApis
import `in`.dragonbra.vapulla.util.runSuspendCatching
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/** A request was made while there is no Steam session to make it on. */
class NotConnectedException : IllegalStateException("Not connected to Steam")

/** What to show the user when a request failed. */
val Throwable.requestMessage: String get() = message ?: "Request failed"

/**
 * App-wide handle to the bound [SteamService]. Injected as a singleton so the activity binds
 * once and view models reach the service through [steamService], or make a call to Steam
 * with [request].
 */
class SteamServiceConnection(private val context: Context) {

    /** The bound service, or null while unbound. */
    var steamService: SteamService? = null
        private set

    private val _isBound = MutableStateFlow(false)
    val isBound: StateFlow<Boolean> = _isBound.asStateFlow()

    private val serviceConnection = object : AndroidServiceConnection {
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

    /** Binds to the service, creating it if needed. Does nothing when already bound. */
    fun bindService() {
        if (isBound.value) return
        val intent = Intent(context, SteamService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /** Lets go of the service. It keeps running if it was started in the foreground. */
    fun unBindService() {
        if (!isBound.value) return
        context.unbindService(serviceConnection)
        _isBound.value = false
        steamService = null
    }

    /**
     * Starts the service so that it outlives the activity.
     *
     * @return false when Android refused, which it does while the app is not on screen.
     */
    fun startForegroundService(): Boolean = try {
        ContextCompat.startForegroundService(context, Intent(context, SteamService::class.java))
        true
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException on Android 12 and up.
        Timber.w(e, "Not allowed to start the service from the background")
        false
    }

    /** Shows the signed-in account to its friends as [state]. Does nothing without a session. */
    fun setPersonaState(state: EPersonaState) {
        steamService?.apis?.friends?.setPersonaState(state)
    }

    /**
     * Makes a call to Steam through the session's [SteamApis]: the one way view models ask
     * Steam for something. A failure is logged as "[what] failed" and returned, with
     * [requestMessage] as the text to show; without a session that is a
     * [NotConnectedException]. Cancelling the caller cancels the request, see
     * [runSuspendCatching].
     */
    suspend fun <T> request(what: String, block: suspend (SteamApis) -> T): Result<T> {
        val apis = steamService?.apis ?: return Result.failure(NotConnectedException())
        return runSuspendCatching { block(apis) }
            .onFailure { Timber.w(it, "$what failed") }
    }
}
