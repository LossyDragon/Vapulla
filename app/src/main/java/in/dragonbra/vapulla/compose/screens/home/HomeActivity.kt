package `in`.dragonbra.vapulla.compose.screens.home

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.settings.SettingsActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

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
                HomeScreen(viewModel = viewModel)
            }
        }
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
        Timber.d("onAccountUpdate")
        val name = account.nickname.orEmpty()
        val state = account.state
        val avatarHash = account.avatarHash.orEmpty()
        viewModel.onUpdateAccount(name, state, avatarHash)
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

    private fun onFriendAction(event: HomeUiEvent) {
        Timber.d("onFriendAction ${event.javaClass}")
        scope.launch(Dispatchers.IO) {
            when (event) {
                HomeUiEvent.AddFriend -> {
                    // TODO there is a protobuf to create/get an invite token,
                    //  but is it possible to figure out a way to construct the s.team url?
                    runOnUiThread {
                        Toast.makeText(
                            this@HomeActivity,
                            "Not Available",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                HomeUiEvent.Disconnect -> steamService?.disconnect()
                HomeUiEvent.LogOut -> steamService?.disconnect()
                HomeUiEvent.Settings -> onSettings()
                HomeUiEvent.Refresh -> {
                    viewModel.clearStates()
                    steamService?.getFriendPersonaStates()
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
