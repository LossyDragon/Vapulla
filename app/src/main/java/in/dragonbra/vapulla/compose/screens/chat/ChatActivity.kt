package `in`.dragonbra.vapulla.compose.screens.chat

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
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.steam.VapullaHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class ChatActivity : VapullaBaseActivity() {

    companion object {
        const val INTENT_STEAM_ID = "steam_id"
    }

    val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        val steamId = SteamID(intent.getLongExtra(INTENT_STEAM_ID, 0L))
        viewModel.setChatSteamID(steamId)

        setContent {
            LaunchedEffect(Unit) {
                viewModel.uiState.collectLatest { event ->
                    onChatEvent(event)
                }
            }

            VapullaTheme {
                CompositionLocalProvider(LocalActivity provides this) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onServiceStart()
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

            scope.launch(Dispatchers.IO) {
                steamService?.getMessageHistory(steamID)
            }
        }

        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (isBound) {
            steamService?.isActivityRunning = false
            steamService?.removeChatFriendId()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)

        val steamID = viewModel.state.value.currentChatSteamID
            ?: throw IllegalArgumentException("SteamID was null in onServiceConnected")

        steamService?.setChatFriendId(steamID)
        steamService?.isActivityRunning = true

        scope.launch(Dispatchers.IO) {
            steamService?.getMessageHistory(steamID)
            steamService?.getHandler<VapullaHandler>()?.getEmoticonList()
        }

        viewModel.onPostCreate(this)
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

    private fun onChatEvent(event: ChatUiEvent) {
        Timber.d("onChatEvent: ${event::class.java.simpleName}")
        scope.launch(Dispatchers.IO) {
            when (event) {
                ChatUiEvent.NavigateUp -> finish()
                is ChatUiEvent.SendTypingStatus -> steamService?.setTyping(event.id)
                is ChatUiEvent.SendMessage -> steamService?.sendMessage(event.id, event.message)
            }
        }
    }
}
