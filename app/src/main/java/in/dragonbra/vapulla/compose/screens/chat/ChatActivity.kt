package `in`.dragonbra.vapulla.compose.screens.chat

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@AndroidEntryPoint
class ChatActivity : VapullaBaseActivity() {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate")

        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        viewModel.setChatSteamID(steamId)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.uiState.collectLatest { event ->
                    Timber.d("FLOWING: ${event.javaClass}")
                    onChatEvent(event)
                }
            }

            VapullaTheme {
                ChatScreen(
                    viewModel = viewModel,
                    onBackPressed = { finish() },
                    onViewProfile = { viewProfile(it) },
                )
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        viewModel.onPostCreate(this)
    }

    override fun onResume() {
        super.onResume()
        if (isBound) {
            val steamID = viewModel.state.value.currentChatSteamID

            if (steamID == null) {
                Timber.w("SteamID was null onResume")
                return
            }

            steamService?.setChatFriendId(steamID)
            steamService?.isActivityRunning = true
            viewModel.getMessageHistory()
        }

        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            steamService?.isActivityRunning = false
            steamService?.removeChatFriendId()
        }

        viewModel.onPause()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        Timber.i("Bound to Steam service")

        val steamID = viewModel.state.value.currentChatSteamID
            ?: throw IllegalArgumentException("SteamID was null in onServiceConnected")

        steamService?.setChatFriendId(steamID)
        steamService?.isActivityRunning = true
        viewModel.getMessageHistory()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.i("Unbound from Steam service")
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            addCategory(Intent.CATEGORY_HOME)
        }.also {
            startActivity(intent)
        }
        finish()
    }

    private fun viewProfile(steamID: SteamID) {
        Intent(this, ProfileActivity::class.java).apply {
            putExtra(ProfileActivity.INTENT_STEAM_ID, steamID.convertToUInt64())
        }.also {
            startActivity(it)
        }
    }

    private fun onChatEvent(event: ChatUiEvent) {
        scope.executeAsyncTask {
            when (event) {
                ChatUiEvent.RequestEmotes -> {
                    steamService?.getHandler<VapullaHandler>()?.getEmoticonList()
                }

                ChatUiEvent.NavigateUp -> {
                    finish()
                }

                is ChatUiEvent.RequestMsgHistory -> {
                    steamService?.getHandler<SteamFriends>()?.requestMessageHistory(event.id)
                    steamService?.getMessageHistory(event.id)
                }

                is ChatUiEvent.SendTypingStatus -> {
                    steamService
                        ?.getHandler<SteamFriends>()
                        ?.sendChatMessage(event.id, EChatEntryType.Typing, "")
                }

                is ChatUiEvent.SendMessage -> {
                    steamService?.sendMessage(event.id, event.message, event.emoteSet)
                }

                is ChatUiEvent.UpdateFriend -> TODO()
            }
        }
    }
}
