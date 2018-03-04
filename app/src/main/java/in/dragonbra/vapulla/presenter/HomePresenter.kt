package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.view.HomeView
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.intentFor
import java.io.Closeable
import java.util.*

class HomePresenter(val context: Context,
                    val steamFriendDao: SteamFriendDao) : VapullaPresenter<HomeView>(), AnkoLogger {

    private var bound = false

    private var steamService: SteamService? = null

    private val subs: MutableList<Closeable?> = LinkedList()

    private val account = AccountManager(context)

    private var friendsData: LiveData<List<SteamFriend>>? = null

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            info("Unbound from Steam service")

            subs.forEach { it?.close() }
            subs.clear()

            bound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            info("Bound to Steam service")
            val binder = service as SteamService.SteamBinder
            steamService = binder.getService()

            subs.add(steamService?.subscribe<DisconnectedCallback>({ onDisconnected() }))

            bound = true
        }
    }

    private fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
    }

    override fun onStart() {
        context.bindService(context.intentFor<SteamService>(), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        context.unbindService(connection)
        bound = false
    }

    override fun onResume() {
        friendsData = steamFriendDao.getAllObservable()
        friendsData?.observe(view as HomeActivity, dataObserver)

        ifViewAttached { it.showFriends(friendsData?.value) }
    }

    override fun onPause() {
        friendsData?.removeObserver(dataObserver)
    }

    val dataObserver: Observer<List<SteamFriend>> = Observer { list ->
        ifViewAttached { it.showFriends(list!!) }
    }

    fun disconnect() {
        runOnBackgroundThread { steamService?.disconnect() }
    }
}