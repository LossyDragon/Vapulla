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
import `in`.dragonbra.vapulla.activity.SettingsActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.UnifiedChatHandler
import `in`.dragonbra.vapulla.threading.executeAsyncTask
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
                    Timber.d("FLOWING: ${event.javaClass}")
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
        Timber.d("onDisconnected")
        closeApplication()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        Timber.d("onPostCreate")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("onResume")
        if (isBound) {
            steamService?.isActivityRunning = true
        }

        with(viewModel.accountManager) {
            addListener(this@HomeActivity)
            viewModel.onEvent(
                HomeEvent.UpdateAccount(avatarHash.orEmpty(), state.name, avatarHash.orEmpty())
            )
        }
    }

    override fun onPause() {
        super.onPause()
        Timber.d("onPause")
        if (isBound) {
            steamService?.isActivityRunning = false
        }

        viewModel.accountManager.removeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        viewModel.onDestroy()
    }

    override fun unAccountUpdate(account: AccountManager) {
        Timber.d("unAccountUpdate")
        viewModel.onEvent(
            HomeEvent.UpdateAccount(
                account.nickname.orEmpty(),
                account.state.name,
                account.avatarHash.orEmpty()
            )
        )
    }

    private fun closeApplication() {
        Timber.d("closeApplication")
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }.also {
            startActivity(it)
        }
        finish()
    }

    private fun onChatSelected(friend: FriendListItem) {
        Timber.d("onChatSelected: ${friend.nickname ?: friend.name}")
        Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.INTENT_STEAM_ID, friend.id)
        }.also {
            startActivity(it)
        }
    }

    private fun onProfileSelected(friend: FriendListItem) {
        Timber.d("onProfileSelected: ${friend.nickname ?: friend.name}")
        Intent(this, ProfileActivity::class.java).apply {
            putExtra(ProfileActivity.INTENT_STEAM_ID, friend.id)
        }.also {
            startActivity(it)
        }
    }

    private fun onSettings() {
        Timber.d("onSettings")

        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun onFriendAction(event: HomeUiEvent) {
        Timber.d("onFriendAction ${event.javaClass}")
        scope.executeAsyncTask {
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
                    viewModel.clearStates()
                    steamService?.getHandler<UnifiedChatHandler>()?.getFriendsList()
                }

                HomeUiEvent.AddFriend -> TODO()

                HomeUiEvent.LogOut -> {
                    steamService?.disconnect()
                }

                HomeUiEvent.Settings -> {
                    onSettings()
                }
            }
        }
    }
}
