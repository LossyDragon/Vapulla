package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.info
import `in`.dragonbra.vapulla.util.recyclerview.FriendsComparator
import `in`.dragonbra.vapulla.view.HomeView
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

class HomePresenter(context: Context,
                    private val steamFriendDao: SteamFriendDao,
                    private val account: AccountManager
) : VapullaPresenter<HomeView>(context), AccountManager.AccountManagerListener {

    private lateinit var friendsData: LiveData<List<FriendListItem>>

    private var isSearching: Boolean = false

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")
        steamService?.isActivityRunning = true
    }

    override fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
    }

    override fun onPostCreate() {
        friendsData = steamFriendDao.getLive()
        // friendsData.observe(view as HomeActivity, dataObserver)
        ifViewAttached { friendsData.observe(it as HomeActivity, dataObserver) }

        ifViewAttached {
            val updateTime = System.currentTimeMillis()
            it.showFriends(friendsData.value?.sortedWith(FriendsComparator(context, updateTime))
                    ?: emptyList(), updateTime)
        }
    }

    override fun onResume() {
        if (bound) {
            steamService?.isActivityRunning = true
        }

        account.addListener(this)
        ifViewAttached {
            it.showAccount(account)
        }
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
        }

        account.removeListener(this)
    }

    override fun onDestroy() {
        friendsData.removeObserver(dataObserver)
    }

    override fun unAccountUpdate(account: AccountManager) {
        ifViewAttached { it.showAccount(account) }
    }

    private val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        if (!isSearching) {
            val updateTime = System.currentTimeMillis()
            ifViewAttached {
                it.showFriends(list?.sortedWith(FriendsComparator(context, updateTime))
                        ?: listOf(), updateTime)
            }
        }
    }

    // TODO:
    //  sometimes onPersonaState() still gets out of sync,
    //  usually when the app is in the background or the phone is idling for a long time
    //  Swipe refresh works, but we need to
    //  assume everyone else is Offline
    //  --> However: last_log_off and last_log_on will get messed up
    //  --> Possibly call for an "ENTIRE" friends list to fix these values.
    fun refreshFriendsList() {
        // First we need to assume everyone is offline.
        runOnBackgroundThread {
            steamFriendDao.clearOnlineState()
        }

        // Then call for an updated friends list, but it only returns anyone who is not Offline.
        runOnBackgroundThread { steamService?.getHandler<VapullaHandler>()?.getFriendsList() }
    }

    fun disconnect() {
        runOnBackgroundThread { steamService?.disconnect() }
    }

    fun changeStatus(state: EPersonaState) {
        if (account.state != state) {
            runOnBackgroundThread {
                steamService?.getHandler<SteamFriends>()?.setPersonaState(state)
            }
        }
    }

    fun acceptRequest(friend: FriendListItem) {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.addFriend(SteamID(friend.id))
        }
    }

    fun ignoreRequest(friend: FriendListItem) {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.removeFriend(SteamID(friend.id))
        }
    }

    fun blockRequest(friend: FriendListItem) {
        ifViewAttached { it.showBlockFriendDialog(friend) }
    }

    fun confirmBlockFriend(friend: FriendListItem) {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.ignoreFriend(SteamID(friend.id))
        }
    }

    fun setSearchStatus(searching: Boolean) {
        isSearching = searching
    }

    // friendsData might still crash, will need to investigate more when it happens.
    fun search(query: String) {
        val trimmedQuery = query.trim()
        friendsData.value?.let { list ->
            val updateTime = System.currentTimeMillis()
            if (Strings.isNullOrEmpty(trimmedQuery)) {
                ifViewAttached {
                    it.showFriends(
                            list.sortedWith(FriendsComparator(context, updateTime)),
                            updateTime
                    )
                }
                return@let
            }

            val filtered = list.filter {
                it.name?.contains(trimmedQuery, true) == true ||
                        it.nickname?.contains(trimmedQuery, true) == true
            }.sortedWith(FriendsComparator(context, updateTime))

            ifViewAttached { it.showFriends(filtered, updateTime) }
        }
    }
}
