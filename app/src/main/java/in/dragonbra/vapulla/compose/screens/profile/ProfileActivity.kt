package `in`.dragonbra.vapulla.compose.screens.profile

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ProfileActivity : VapullaBaseActivity() {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        val steamID = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        viewModel.setSteamID(steamID)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    onProfileEvent(event)
                }
            }

            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        viewModel.onPostCreate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }.also(::startActivity)
        finish()
    }

    override fun onAliasHistory(callback: AliasHistoryCallback) {
        super.onAliasHistory(callback)
        viewModel.onAliasHistory(callback)
    }

    private fun onProfileEvent(event: ProfileUiEvent) {
        val steamID = viewModel.state.value.steamID
        scope.launch(Dispatchers.IO) {
            when (event) {
                ProfileUiEvent.BlockFriend -> getHandler<SteamFriends>()?.ignoreFriend(steamID)
                ProfileUiEvent.GetAliases -> getHandler<SteamFriends>()?.requestAliasHistory(
                    steamID
                )
                ProfileUiEvent.NavigateBack -> finish()
                ProfileUiEvent.RemoveFriend -> getHandler<SteamFriends>()?.removeFriend(steamID)
                is ProfileUiEvent.SetNickName ->
                    getHandler<SteamFriends>()?.setFriendNickname(steamID, event.nickName)
            }
        }
    }
}
