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
import `in`.dragonbra.javasteam.util.compat.Consumer
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

            subs.add(steamService?.callbackMgr?.subscribe(ConnectedCallback::class.java) {
                onConnected()
            })
            subs.add(steamService?.callbackMgr?.subscribe(DisconnectedCallback::class.java) {
                onDisconnected()
            })
            subs.add(steamService?.callbackMgr?.subscribe(LoggedOnCallback::class.java) {
                onLoggedOn(it)
            })
            subs.add(steamService?.callbackMgr?.subscribe(AliasHistoryCallback::class.java) {
                onAliasHistory(it) // Leaked; Unified this so it gets closed properly
            })

            isBound = true
            this@VapullaBaseActivity.onServiceConnected(name, service)
        }
    }

    open fun onServiceStart() {
        val intent = Intent(this, SteamService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        subs.forEach { it?.close() }
        subs.clear()
        isBound = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(STOP_INTENT)
        registerReceiver(stopReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopReceiver)
        steamService = null // Memory Leak Hunting
    }

    fun subscribe(sub: Closeable?) {
        subs.add(sub)
    }

    open fun onConnected(showLoading: () -> Unit = {}, isLoggedIn: () -> Unit = {}) {}

    open fun onDisconnected() {}

    open fun onLoginSuccess() {}

    open fun onLoggedOn(callback: LoggedOnCallback) {}

    open fun onAliasHistory(callback: AliasHistoryCallback) {}

    open fun onServiceConnected(name: ComponentName, service: IBinder) {}

    open fun onServiceDisconnected(name: ComponentName) {}

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
