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
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.service.SteamService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import timber.log.Timber
import java.io.Closeable
import java.util.LinkedList

abstract class VapullaBaseActivity : ComponentActivity() {

    companion object {
        const val STOP_INTENT = "in.dragonbra.vapulla.SERVICE_STOP"
    }

    protected val scope = CoroutineScope(Dispatchers.Default + Job())

    private val stopReceiver = StopReceiver()

    private val subs: MutableList<Closeable?> = LinkedList()

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
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()

            subs.add(steamService?.subscribe<ConnectedCallback> { onConnected() })
            subs.add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
            subs.add(steamService?.subscribe<LoggedOnCallback> { onLoggedOn(it) })
            subs.add(steamService?.subscribe<AliasHistoryCallback> { onAliasHistory(it) })

            isBound = true
            this@VapullaBaseActivity.onServiceConnected(name, service)
        }
    }

    open fun onServiceStart() {
        val intent = Intent(this, SteamService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(STOP_INTENT)
        registerReceiver(stopReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            subs.forEach { it?.close() }
            subs.clear()
        }
        isBound = false
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            steamService?.isActivityRunning = false
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopReceiver)
        steamService = null
    }

    open fun onConnected() {}

    open fun onDisconnected() {}

    open fun onLoginSuccess() {}

    open fun onLoggedOn(callback: LoggedOnCallback) {}

    open fun onAliasHistory(callback: AliasHistoryCallback) {}

    open fun onServiceConnected(name: ComponentName, service: IBinder) {
        Timber.d("Bound to Steam service")
        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    open fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Unbound from Steam service")
    }

    fun startSteamService(showLoading: () -> Unit) {
        Timber.d("Starting steam service...")

        val intent = Intent(this, SteamService::class.java)
        startService(intent)

        steamService?.let { service ->
            if (!service.isRunning) {
                steamService?.connect()

                showLoading()
            } else {
                onConnected()
            }
        }
    }

    inline fun <reified T : ClientMsgHandler> getHandler(): T? {
        return steamService?.getHandler()
    }

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}
