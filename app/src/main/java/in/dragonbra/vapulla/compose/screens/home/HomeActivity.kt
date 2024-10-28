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
import `in`.dragonbra.vapulla.compose.screens.invites.links.InviteLinks
import `in`.dragonbra.vapulla.compose.screens.settings.SettingsActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : AccountManager.AccountManagerListener, VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    onFriendAction(event)
                }
            }

            VapullaTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        accountManager.addListener(this@HomeActivity)

        onAccountUpdate(accountManager)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        closeApplication()
    }

    override fun onPause() {
        super.onPause()
        accountManager.removeListener(this)
    }

    override fun onAccountUpdate(account: AccountManager) {
        Timber.d("onAccountUpdate")
        with(account) {
            viewModel.onUpdateAccount(nickname.orEmpty(), state, avatarHash.orEmpty())
        }
    }

    private fun closeApplication() {
        Timber.d("closeApplication")
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }.also(::startActivity)
        finish()
    }

    private fun onSettings() {
        Timber.d("onSettings")
        Intent(this, SettingsActivity::class.java).also(::startActivity)
    }

    private fun onInviteLinks() {
        Timber.d("onInvites")
        Intent(this, InviteLinks::class.java).also(::startActivity)
    }

    private fun onPendingInvites() {
        TODO()
    }

    private fun onFriendAction(event: HomeUiEvent) {
        Timber.d("onFriendAction ${event.javaClass}")
        scope.launch(Dispatchers.IO) {
            when (event) {
                HomeUiEvent.PendingInvites -> onPendingInvites()
                HomeUiEvent.InviteLinks -> onInviteLinks()
                HomeUiEvent.Settings -> onSettings()
                HomeUiEvent.Disconnect,
                HomeUiEvent.LogOut -> steamService?.disconnect()

                HomeUiEvent.Refresh -> {
                    viewModel.clearStates()
                    steamService?.getFriendPersonaStates()
                }

                is HomeUiEvent.AcceptRequest -> {
                    val friend = SteamID(event.friend.id)
                    getHandler<SteamFriends>()?.addFriend(friend)
                }

                is HomeUiEvent.BlockFriend -> {
                    val friend = SteamID(event.friend.id)
                    getHandler<SteamFriends>()?.ignoreFriend(friend)
                }

                is HomeUiEvent.ChangeStatus -> {
                    getHandler<SteamFriends>()?.setPersonaState(event.state)
                }

                is HomeUiEvent.IgnoreRequest -> {
                    val friend = SteamID(event.friend.id)
                    getHandler<SteamFriends>()?.removeFriend(friend)
                }
            }
        }
    }
}
