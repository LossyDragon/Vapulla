package `in`.dragonbra.vapulla

import android.content.*
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
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
    private var steamService: SteamService? = null
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            Timber.d("Unbound from Steam service")
            serviceSubscriptions.run {
                forEach { it?.close() }
                clear()
            }
            isBound = false
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("Bound to Steam service")
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            serviceSubscriptions.run {
                add(steamService?.subscribe<ConnectedCallback> { onConnected() })
                add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
            }
            isBound = true
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, SteamService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        this.unbindService(connection)
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

    open fun onConnected() {}

    open fun onDisconnected() {}

    fun startSteamService(logOnDetails: LogOnDetails) {
        Timber.d("Starting steam service...")
        val intent = Intent(this, SteamService::class.java)
        startService(intent)

        steamService?.let { service ->
            if (!service.isRunning) {
                steamService?.connect()

                // TODO show that we're trying to connect to Steam
            } else {
                if (service.isLoggedIn) {
                    // We're logged in, go to home
                    Timber.d("Logged into Steam")
                    return
                }

                service.logOn(logOnDetails)
                // TODO show that we're logging in
            }
        }
    }

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}