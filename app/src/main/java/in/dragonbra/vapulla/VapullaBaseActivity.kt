package `in`.dragonbra.vapulla

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.service.SteamServiceBinder
import java.io.Closeable
import java.util.LinkedList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber

// TODO: better way to use timber? tag?
abstract class VapullaBaseActivity : ComponentActivity() {

    companion object {
        const val STOP_INTENT = "in.dragonbra.vapulla.SERVICE_STOP"
    }

    protected val scope = CoroutineScope(Dispatchers.Default + Job())

    private val subs: MutableList<Closeable?> = LinkedList()

    private val stopReceiver = StopReceiver()

    var isBound = false
        private set
    var steamService: SteamService? = null
        private set

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            subs.forEach { it?.close() }
            subs.clear()
            isBound = false
            this@VapullaBaseActivity.onServiceDisconnected(name)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            steamService = (service as SteamServiceBinder).getService()

            subs.add(steamService?.subscribe<ConnectedCallback> { onConnected() })
            subs.add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
            subs.add(steamService?.subscribe<LoggedOnCallback> { onLoggedOn(it) })
            subs.add(steamService?.subscribe<AliasHistoryCallback> { onAliasHistory(it) })

            isBound = true
            this@VapullaBaseActivity.onServiceConnected(name, service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("[${this::class.java.simpleName}] onCreate")

        val filter = IntentFilter(STOP_INTENT)

        if (Constants.isAtLeastT) {
            registerReceiver(stopReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stopReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("[${this::class.java.simpleName}] onPause")

        if (isBound) {
            steamService?.isActivityRunning = false
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.d("[${this::class.java.simpleName}] onResume")

        Intent(this, SteamService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("[${this::class.java.simpleName}] onDestroy")

        unregisterReceiver(stopReceiver)

        unbindService(connection)
        subs.forEach { it?.close() }
        subs.clear()

        isBound = false
        steamService = null
    }

    open fun onConnected() {
        Timber.d("[${this::class.java.simpleName}] onConnected")
    }

    open fun onDisconnected() {
        Timber.d("[${this::class.java.simpleName}] onDisconnected")
    }

    open fun onLoginSuccess() {
        Timber.d("[${this::class.java.simpleName}] onLoginSuccess")
    }

    open fun onLoggedOn(callback: LoggedOnCallback) {
        Timber.d("[${this::class.java.simpleName}] onLoggedOn")
    }

    open fun onAliasHistory(callback: AliasHistoryCallback) {
        Timber.d("[${this::class.java.simpleName}] onAliasHistory")
    }

    open fun onServiceConnected(name: ComponentName, service: IBinder) {
        Timber.d("[${this::class.java.simpleName}] Bound to Steam service")
        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    open fun onServiceDisconnected(name: ComponentName) {
        Timber.d("[${this::class.java.simpleName}] Unbound from Steam service")
    }

    fun startSteamService() {
        Timber.d("[${this::class.java.simpleName}] Starting steam service...")

        Intent(this, SteamService::class.java).also(::startService)

        if (steamService == null) {
            return
        }

        if (!steamService!!.isRunning) {
            steamService!!.connect()
            return
        }

        onConnected()
    }

    inline fun <reified T : ClientMsgHandler> getHandler(): T? = steamService?.getHandler()

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}
