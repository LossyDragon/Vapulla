package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.util.VapullaLogger
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.CallSuper
import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter
import com.hannesdorfmann.mosby3.mvp.MvpView
import java.io.Closeable
import java.util.*

abstract class VapullaPresenter<V : MvpView>(val context: Context) : MvpBasePresenter<V>(), VapullaLogger {

    protected var bound = false

    protected var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            subs.forEach { it?.close() }
            subs.clear()
            bound = false
            this@VapullaPresenter.onServiceDisconnected(name)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()
            subs.add(steamService?.subscribe<ConnectedCallback> { onConnected() })
            subs.add(steamService?.subscribe<DisconnectedCallback> { onDisconnected() })
            bound = true
            this@VapullaPresenter.onServiceConnected(name, service)
        }
    }

    open fun onPostCreate() {
    }
    @CallSuper
    open fun onStart() {
        context.bindService(Intent(context, SteamService::class.java), connection, Context.BIND_AUTO_CREATE)
    }
    open fun onResume() {}
    open fun onPause() {}
    @CallSuper
    open fun onStop() {
        context.unbindService(connection)
        subs.forEach { it?.close() }
        subs.clear()
        bound = false
    }
    open fun onDestroy() {

    }

    open fun onServiceConnected(name: ComponentName, service: IBinder) {}
    open fun onServiceDisconnected(name: ComponentName) {}
    open fun onConnected() {}
    open fun onDisconnected() {}

    fun subscribe(sub: Closeable?) {
        subs.add(sub)
    }
}