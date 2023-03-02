package `in`.dragonbra.vapulla.compose.screens.home

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.screens.settings.SettingsActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.UnifiedChatHandler
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import javax.inject.Inject

// TODO: Make sure the friends list is updated shortly after initting.

@AndroidEntryPoint
class HomeActivity : AccountManager.AccountManagerListener, VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

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

    override fun onStart() {
        super.onStart()
        onServiceStart()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        accountManager.addListener(this@HomeActivity)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        closeApplication()
    }

    override fun onPause() {
        super.onPause()
        accountManager.removeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        viewModel.onDestroy()
    }

    override fun onAccountUpdate(account: AccountManager) {
        Timber.d("unAccountUpdate")
        val name = account.nickname.orEmpty()
        val state = account.state
        val avatarHash = account.avatarHash.orEmpty()
        viewModel.onEvent(HomeEvent.UpdateAccount(name, state, avatarHash))
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
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun onFriendAction(event: HomeUiEvent) {
        Timber.d("onFriendAction ${event.javaClass}")
        scope.executeAsyncTask {
            when (event) {
                HomeUiEvent.AddFriend -> TODO("Add Friend")
                HomeUiEvent.Disconnect -> steamService?.disconnect()
                HomeUiEvent.LogOut -> steamService?.disconnect()
                HomeUiEvent.Settings -> onSettings()
                HomeUiEvent.Refresh -> {
                    viewModel.clearStates()
                    steamService?.getHandler<UnifiedChatHandler>()?.getFriendsList()
                }

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
            }
        }
    }
}
