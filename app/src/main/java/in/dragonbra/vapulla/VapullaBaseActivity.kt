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
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
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
            steamService = (service as SteamServiceBinder).service

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
        Timber.tag(this::class.java.simpleName).d("onCreate")

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
        Timber.tag(this::class.java.simpleName).d("onPause")

        if (isBound) {
            steamService?.isActivityRunning = false
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(this::class.java.simpleName).d("onResume")

        Intent(this, SteamService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag(this::class.java.simpleName).d("onDestroy")

        unregisterReceiver(stopReceiver)

        unbindService(connection)
        subs.forEach { it?.close() }
        subs.clear()

        isBound = false
        steamService = null
    }

    open fun onConnected() {
        Timber.tag(this::class.java.simpleName).d("onConnected")
    }

    open fun onDisconnected() {
        Timber.tag(this::class.java.simpleName).d("onDisconnected")
    }

    open fun onLoginSuccess() {
        Timber.tag(this::class.java.simpleName).d("onLoginSuccess")
    }

    open fun onLoggedOn(callback: LoggedOnCallback) {
        Timber.tag(this::class.java.simpleName).d("onLoggedOn")
    }

    open fun onAliasHistory(callback: AliasHistoryCallback) {
        Timber.tag(this::class.java.simpleName).d("onAliasHistory")
    }

    open fun onServiceConnected(name: ComponentName, service: IBinder) {
        Timber.tag(this::class.java.simpleName).d("Bound to Steam service")
        if (isBound) {
            steamService?.isActivityRunning = true
        }
    }

    open fun onServiceDisconnected(name: ComponentName) {
        Timber.tag(this::class.java.simpleName).d("Unbound from Steam service")
    }

    fun startSteamService() {
        Timber.tag(this::class.java.simpleName).d("Starting steam service...")

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

    internal inline fun <reified T : ClientMsgHandler> getHandler(): T? = steamService?.getHandler()

    inner class StopReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishAffinity()
        }
    }
}
