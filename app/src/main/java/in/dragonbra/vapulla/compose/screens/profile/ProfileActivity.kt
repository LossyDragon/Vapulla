package `in`.dragonbra.vapulla.compose.screens.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import kotlinx.coroutines.flow.collectLatest
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

        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(Unit) {
                viewModel.onPostCreate(lifecycleOwner, steamId)
                viewModel.uiEvent.collectLatest { event ->
                    onProfileEvent(event, steamId)
                }
            }

            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onServiceStart()
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
        }.also {
            startActivity(it)
        }
        finish()
    }

    override fun onAliasHistory(callback: AliasHistoryCallback) {
        super.onAliasHistory(callback)
        viewModel.onAliasHistory(callback)
    }

    private fun onProfileEvent(event: ProfileUiEvent, steamID: SteamID) {
        scope.executeAsyncTask {
            when (event) {
                ProfileUiEvent.NavigateBack -> finish()
                is ProfileUiEvent.GetAliases -> {
                    val jobID = getHandler<SteamFriends>()?.requestAliasHistory(event.steamID)
                    viewModel.setJobID(jobID)
                }
                is ProfileUiEvent.SetNickName -> {
                    getHandler<SteamFriends>()?.setFriendNickname(event.steamID, event.nickName)
                }
                is ProfileUiEvent.BlockFriend -> {
                    getHandler<SteamFriends>()?.ignoreFriend(steamID)
                }
                is ProfileUiEvent.RemoveFriend -> {
                    getHandler<SteamFriends>()?.removeFriend(steamID)
                }
            }
        }
    }
}
