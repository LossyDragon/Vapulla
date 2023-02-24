package `in`.dragonbra.vapulla.compose.screens.home

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.ProfileActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.UnifiedChatHandler
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class HomeActivity : AccountManager.AccountManagerListener, VapullaBaseActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("HomeActivity")
        setContent {
            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    onFriendAction(event)
                }
            }

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
        viewModel.friendsData.observe(this, viewModel.dataObserver)
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

        viewModel.account.removeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun unAccountUpdate(account: AccountManager) {
        Timber.w("ACCOUNT UPDATE TODO")
        viewModel.onEvent(
            HomeEvent.UpdateAccount(
                account.nickname.orEmpty(),
                account.state.name,
                account.avatarHash.orEmpty()
            )
        )
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

    private fun onFriendAction(event: HomeUiEvent) {
        Runnable {
            when (event) {
                is HomeUiEvent.AcceptRequest -> {
                    val friend = SteamID(event.friend.id)
                    steamService?.getHandler<SteamFriends>()?.addFriend(friend)
                }

                is HomeUiEvent.BlockFriend -> {
                    val friend = SteamID(event.friend.id)
                    steamService?.getHandler<SteamFriends>()?.ignoreFriend(friend)
                }

                is HomeUiEvent.ChangeStatus -> {
                    steamService?.getHandler<SteamFriends>()?.setPersonaState(event.state)
                }

                is HomeUiEvent.IgnoreRequest -> {
                    val friend = SteamID(event.friend.id)
                    steamService?.getHandler<SteamFriends>()?.removeFriend(friend)
                }

                HomeUiEvent.Disconnect -> {
                    steamService?.disconnect()
                }

                HomeUiEvent.Refresh -> {
                    steamService?.getHandler<UnifiedChatHandler>()?.getFriendsList()
                }
            }
        }
    }
}
