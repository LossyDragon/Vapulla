package `in`.dragonbra.vapulla.compose.screens.home

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.ProfileActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.UnifiedChatHandler
import `in`.dragonbra.vapulla.util.recyclerview.FriendsComparator
import timber.log.Timber

sealed class HomeAction {
    data class ChangeStatus(val state: EPersonaState) : HomeAction()
    data class AcceptRequest(val friend: FriendListItem) : HomeAction()
    data class IgnoreRequest(val friend: FriendListItem) : HomeAction()
    data class BlockFriend(val friend: FriendListItem) : HomeAction()
    object Disconnect : HomeAction()
    object Refresh : HomeAction()
}

class HomeActivity : AccountManager.AccountManagerListener, VapullaBaseActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("HomeActivity")
        setContent {
            VapullaTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onChatSelected = { friend ->
                        onChatSelected(friend)
                    },
                    onProfileSelected = { friend ->
                        onProfileSelected(friend)
                    }
                )
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        Timber.d("Bound to Steam service")
        steamService?.isActivityRunning = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.d("Unbound from Steam service")
    }

    override fun onDisconnected() {
        super.onDisconnected()
        closeApplication()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        viewModel.homeState.list.observe(this, viewModel.dataObserver)

        val updateTime = System.currentTimeMillis()
        val friends = viewModel.getLive().sortedWith(FriendsComparator(this, updateTime))
        viewModel.onEvent(HomeEvent.UpdateFriends(MutableLiveData(friends), updateTime))
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            steamService?.isActivityRunning = true
        }

        with(viewModel.account) {
            addListener(this@HomeActivity)
            viewModel.onEvent(
                HomeEvent.UpdateAccount(
                    avatarHash.orEmpty(),
                    state.name,
                    avatarHash.orEmpty()
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            steamService?.isActivityRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.homeState.list.removeObserver(viewModel.dataObserver)
    }

    override fun unAccountUpdate(account: AccountManager) {
        Timber.w("ACCOUNT UPDATE TODO")
        // TODO: get Avatar, Name, Status
    }

    private fun closeApplication() {
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }.also {
            startActivity(it)
            finish()
        }
    }

    private fun onChatSelected(friend: FriendListItem) {
        Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.INTENT_STEAM_ID, friend.id)
        }.also {
            startActivity(it)
        }
    }

    private fun onProfileSelected(friend: FriendListItem) {
        Intent(this, ProfileActivity::class.java).apply {
            putExtra(ProfileActivity.INTENT_STEAM_ID, friend.id)
        }.also {
            startActivity(it)
        }
    }

    private fun onFriendAction(event: HomeAction) {
        when (event) {
            is HomeAction.AcceptRequest -> {
                val friend = SteamID(event.friend.id)
                steamService?.getHandler<SteamFriends>()?.addFriend(friend)
            }
            is HomeAction.BlockFriend -> {
                val friend = SteamID(event.friend.id)
                steamService?.getHandler<SteamFriends>()?.ignoreFriend(friend)
            }
            is HomeAction.ChangeStatus -> {
                steamService?.getHandler<SteamFriends>()?.setPersonaState(event.state)
            }
            is HomeAction.IgnoreRequest -> {
                val friend = SteamID(event.friend.id)
                steamService?.getHandler<SteamFriends>()?.removeFriend(friend)
            }
            HomeAction.Disconnect -> {
                steamService?.disconnect()
            }
            HomeAction.Refresh -> {
                steamService?.getHandler<UnifiedChatHandler>()?.getFriendsList()
            }
        }
    }
}
