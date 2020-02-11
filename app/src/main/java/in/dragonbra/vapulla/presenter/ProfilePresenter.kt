package `in`.dragonbra.vapulla.presenter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.JobID
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.activity.ProfileActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.info
import `in`.dragonbra.vapulla.view.ProfileView
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import javax.inject.Inject

class ProfilePresenter(context: Context,
                       private val steamId: SteamID,
                       private val steamFriendDao: SteamFriendDao,
                       private val schemaManager: GameSchemaManager,
                       private val levelManager: ProfileManager
) : VapullaPresenter<ProfileView>(context) {

    private lateinit var friendData: LiveData<FriendListItem>

    @Inject
    lateinit var steamApi: SteamApi

    private var aliasJobId: JobID? = null

    private val friendObserver = Observer<FriendListItem> { friend ->
        ifViewAttached { v ->
            friend?.let {
                if (it.relation == EFriendRelationship.Friend.code()) {
                    v.updateFriendData(friend)
                } else {
                    v.navigateUp()
                }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        info("Unbound from Steam service")
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        info("Bound to Steam service")
        subscribe(steamService?.subscribe<AliasHistoryCallback> { onAliasHistory(it) })

        steamService?.isActivityRunning = true
    }

    override fun onPostCreate() {
        friendData = steamFriendDao.findLive(steamId.convertToUInt64())

        ifViewAttached { friendData.observe(it as ProfileActivity, friendObserver) }

        friendData.value?.let {
            if (it.gameAppId > 0) {
                runOnBackgroundThread {
                    schemaManager.touch(it.gameAppId)
                }
            }
        }

        ifViewAttached {
            it.updateFriendData(friendData.value)
        }
    }

    override fun onDisconnected() {
        ifViewAttached {
            it.closeApp()
        }
    }

    override fun onResume() {
        if (bound) {
            steamService?.isActivityRunning = true
        }

        ifViewAttached {
            it.updateFriendData(friendData.value)
        }
    }

    override fun onPause() {
        if (bound) {
            steamService?.isActivityRunning = false
        }
    }

    override fun onDestroy() {
        friendData.removeObserver(friendObserver)
    }

    fun menuSetNickname() {
        ifViewAttached { it.showSetNicknameDialog(friendData.value?.nickname) }
    }

    fun menuViewAliases() {
        runOnBackgroundThread {
            aliasJobId = steamService?.getHandler<SteamFriends>()?.requestAliasHistory(steamId)
        }
    }

    fun menuRemoveFriend() {
        ifViewAttached {
            it.showRemoveFriendDialog(friendData.value?.name)
        }
    }

    fun menuBlockFriend() {
        ifViewAttached {
            it.showBlockFriendDialog(friendData.value?.name)
        }
    }

    fun buttonViewChat() {
        ifViewAttached {
            it.viewChat(steamId.convertToUInt64())
        }
    }

    fun buttonViewProfile() {
        ifViewAttached {
            it.viewProfile("${Utils.PROFILE_URL}${steamId.convertToUInt64()}")
        }
    }

    fun buttonViewGames() {
        ifViewAttached {
            it.viewGames("${Utils.PROFILE_URL}${steamId.convertToUInt64()}/games/?tab=all")
        }
    }

    fun buttonViewManage() {
        ifViewAttached {
            it.showManageDialog(steamId)
        }
    }

    fun getLevel() {
        runOnBackgroundThread {
            ifViewAttached {
                    it.updateBadgeLevel(levelManager.getLevel(steamId))
            }
        }
    }

    fun getGameCount() {
        runOnBackgroundThread {
            ifViewAttached {
                it.updateGameCount(levelManager.getGames(steamId))
            }
        }
    }

    fun menuConfirmSetNickName(nickname: String) {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.setFriendNickname(steamId, nickname)
            val friend = steamFriendDao.find(steamId.convertToUInt64())

            if (friend != null) {
                friend.nickname = nickname
                steamFriendDao.update(friend)
            }
        }
    }

    fun menuConfirmBlockFriend() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.ignoreFriend(steamId)
        }
    }

    fun menuConfirmRemoveFriend() {
        runOnBackgroundThread {
            steamService?.getHandler<SteamFriends>()?.removeFriend(steamId)
        }
    }

    private fun onAliasHistory(callback: AliasHistoryCallback) {
        if (aliasJobId == callback.jobID) {
            ifViewAttached { it ->
                val list = callback.responses[0].names.toMutableList()
                list.sortByDescending { it.nameSince }
                val nicknames = list.map { it.name }
                it.showAliasesDialog(nicknames)
            }
        }
    }
}
