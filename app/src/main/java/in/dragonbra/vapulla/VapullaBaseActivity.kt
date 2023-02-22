package `in`.dragonbra.vapulla

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.service.SteamService
import timber.log.Timber
import java.io.Closeable
import java.util.LinkedList

abstract class VapullaBaseActivity : ComponentActivity() {

    companion object {
        const val STOP_INTENT = "in.dragonbra.vapulla.SERVICE_STOP"
    }

    private val stopReceiver = StopReceiver()

    val vapulla: VapullaApplication
        get() = application as VapullaApplication

    private val serviceSubscriptions = LinkedList<Closeable?>()

    private var isBound = false
    var steamService: SteamService? = null
        private set

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            serviceSubscriptions.run {
                forEach { it?.close() }
                clear()
            }
            isBound = false
            this@VapullaBaseActivity.onServiceDisconnected(name)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            serviceSubscriptions.run {
                add(steamService?.subscribe<ConnectedCallback> { onConnected() })
                add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
                add(steamService?.subscribe<LoggedOnCallback> { onLoggedOn(it) })
            }
            isBound = true
            this@VapullaBaseActivity.onServiceConnected(name, service)
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SteamService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        serviceSubscriptions.run {
            forEach { it?.close() }
            clear()
        }
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
    }

    open fun onConnected(showLoading: () -> Unit = {}, isLoggedIn: () -> Unit = {}) {}

    open fun onDisconnected() {}

    open fun onLoginSuccess() {}

    open fun onLoggedOn(callback: LoggedOnCallback) {}

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

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}
