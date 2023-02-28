package `in`.dragonbra.vapulla.compose.screens.profile

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.AliasHistoryCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.games.GamesActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.retrofit.response.Games
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import `in`.dragonbra.vapulla.util.Utils
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
        Timber.d("onCreate")

        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(Unit) {
                viewModel.onPostCreate(lifecycleOwner, steamId)
            }

            LaunchedEffect(Unit) {
                viewModel.uiEvent.collectLatest { event ->
                    onProfileEvent(event, steamId)
                }
            }

            VapullaTheme {
                ProfileScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onChatClick = { viewChat(it) },
                    onAccountClick = { viewProfile(it) },
                    onGamesClick = { games, name -> viewGames(games, name) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            steamService?.isActivityRunning = true
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
        viewModel.onDestroy()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        Timber.d("Bound to Steam service")
        subscribe(steamService?.subscribe<AliasHistoryCallback> { viewModel.onAliasHistory(it) })
        steamService?.isActivityRunning = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.d("Unbound from Steam service")
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

    private fun navigateUp() {
        finish()
    }

    private fun viewChat(steamId: SteamID) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(INTENT_STEAM_ID, steamId.convertToUInt64())
        }
        startActivity(intent)
    }

    private fun viewProfile(steamID: SteamID) {
        val url = Utils.PROFILE_URL + steamID.convertToUInt64()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }
        startActivity(intent)
    }

    private fun viewGames(gamesList: ArrayList<Games>, friendName: String) {
        val bundle = Bundle().apply {
            putParcelableArrayList(GamesActivity.INTENT_GAMES, gamesList)
            putString("name", friendName)
        }
        val intent = Intent(this, GamesActivity::class.java).apply {
            putExtras(bundle)
        }
        startActivity(intent)
    }

    private fun onProfileEvent(event: ProfileUiEvent, steamID: SteamID) {
        scope.executeAsyncTask {
            when (event) {
                ProfileUiEvent.NavigateBack -> navigateUp()
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
