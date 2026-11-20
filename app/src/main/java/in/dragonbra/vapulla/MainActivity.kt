package `in`.dragonbra.vapulla

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.ui.NavigationRoot
import `in`.dragonbra.vapulla.ui.Routes
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Single activity. Binds and starts [in.dragonbra.vapulla.service.SteamService], asks for the
 * notification permission, and hosts the Compose navigation root.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_OPEN_CHAT = "open_chat"
        private const val EXTRA_OPEN_PROFILE = "open_profile"
        private const val EXTRA_OPEN_DOWNLOADS = "open_downloads"
        private const val EXTRA_REQUEST_TIME = "open_chat_time"
        private const val STATE_HANDLED_REQUEST = "handled_open_chat"

        /**
         * Opens the app on the chat with [friendId]. An activity that already exists is reused
         * and gets the request through [onNewIntent]; otherwise a new one starts with it.
         */
        fun openChatIntent(context: Context, friendId: Long): Intent =
            openIntent(context).putExtra(EXTRA_OPEN_CHAT, friendId)

        /** Opens the app on the profile of [friendId], the way [openChatIntent] opens a chat. */
        fun openProfileIntent(context: Context, friendId: Long): Intent =
            // The action keeps this apart from the same friend's chat intent: extras alone
            // do not make two pending intents different.
            openIntent(context).setAction(EXTRA_OPEN_PROFILE).putExtra(EXTRA_OPEN_PROFILE, friendId)

        /** Opens the app on the Downloads screen. */
        fun openDownloadsIntent(context: Context): Intent =
            openIntent(context).setAction(EXTRA_OPEN_DOWNLOADS).putExtra(EXTRA_OPEN_DOWNLOADS, true)

        private fun openIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            // Tells one tap from the next, see handledRequest.
            .putExtra(EXTRA_REQUEST_TIME, System.currentTimeMillis())
    }

    private val serviceConnection: SteamServiceConnection by inject()
    private val settingsManager: SettingsManager by inject()

    /** The screen a tapped notification asked for, until navigation has opened it. */
    private val screenToOpen = MutableStateFlow<Routes?>(null)

    /**
     * The request time of the newest intent with a screen to open that was acted on. The system hands an old intent
     * back whenever it recreates the activity, and this keeps it from reopening that screen.
     */
    private var handledRequest = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Created...")

        handledRequest = savedInstanceState?.getLong(STATE_HANDLED_REQUEST, -1L) ?: -1L
        readOpenRequest(intent, isNew = savedInstanceState == null)

        serviceConnection.bindService()
        serviceConnection.startForegroundService()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = TRANSPARENT),
        )

        setContent {
            // Before Android 13 notifications need no permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RequestNotificationPermission()
            }

            val settings by settingsManager.settings.collectAsStateWithLifecycle(AppSettings())

            CompositionLocalProvider(
                LocalAppSettings provides settings,
            ) {
                VapullaTheme(
                    isDarkTheme = settings.darkTheme,
                    useDynamicColor = settings.dynamicColor,
                ) {
                    val openScreen by screenToOpen.collectAsStateWithLifecycle()

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        NavigationRoot(
                            modifier = Modifier.fillMaxSize(),
                            openScreen = openScreen,
                            onScreenOpened = { screenToOpen.value = null },
                        )
                    }
                }
            }
        }
    }

    /** A notification tapped while the activity already exists arrives here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readOpenRequest(intent, isNew = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_HANDLED_REQUEST, handledRequest)
    }

    /** @param isNew true for an intent the activity has not been handed before. */
    private fun readOpenRequest(intent: Intent?, isNew: Boolean) {
        val chat = intent?.getLongExtra(EXTRA_OPEN_CHAT, 0L) ?: 0L
        val profile = intent?.getLongExtra(EXTRA_OPEN_PROFILE, 0L) ?: 0L
        val screen = when {
            chat != 0L -> Routes.FriendChat(chat)
            profile != 0L -> Routes.FriendProfile(profile)
            intent?.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false) == true -> Routes.Downloads
            else -> null
        }
        val requestTime = intent?.getLongExtra(EXTRA_REQUEST_TIME, 0L) ?: 0L

        Timber.d("readOpenRequest: screen=$screen request=$requestTime handled=$handledRequest")

        // A recreated activity is handed the intent it was first started with, which may be a
        // tap it acted on long ago, so there only a tap newer than the last one handled
        // counts. A friend's shortcut always carries the time it was made, so a new intent
        // counts whatever its time.
        if (screen != null && (isNew || requestTime > handledRequest)) {
            handledRequest = maxOf(handledRequest, requestTime)
            screenToOpen.value = screen
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection.unBindService()
    }
}

/**
 * Asks for POST_NOTIFICATIONS when it is not granted: once each time the activity is created,
 * which Android itself stops showing after the second refusal.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    var granted by remember {
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        mutableStateOf(result == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted = it },
    )

    LaunchedEffect(granted) {
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
