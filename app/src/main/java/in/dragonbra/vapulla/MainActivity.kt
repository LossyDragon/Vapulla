package `in`.dragonbra.vapulla

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
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
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.NavigationRoot
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import `in`.dragonbra.vapulla.util.decoders.IconDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Single activity. Binds and starts [in.dragonbra.vapulla.service.SteamService], asks for the
 * notification permission, and hosts the Compose navigation root with a shared image loader.
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_OPEN_CHAT = "open_chat"
        private const val EXTRA_REQUEST_TIME = "open_chat_time"
        private const val STATE_HANDLED_REQUEST = "handled_open_chat"

        /**
         * Opens the app on the chat with [friendId]. An activity that already exists is reused
         * and gets the request through [onNewIntent]; otherwise a new one starts with it.
         */
        fun openChatIntent(context: Context, friendId: Long): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                .putExtra(EXTRA_OPEN_CHAT, friendId)
                // Tells one tap from the next, see handledRequest.
                .putExtra(EXTRA_REQUEST_TIME, System.currentTimeMillis())
    }

    private val serviceConnection: ServiceConnection by inject()
    private val settingsManager: SettingsManager by inject()

    /** The friend whose chat a tapped notification asked for, until navigation has opened it. */
    private val chatToOpen = MutableStateFlow<Long?>(null)

    /**
     * The request time of the newest open-chat intent acted on. The system hands an old intent
     * back whenever it recreates the activity, and this keeps it from reopening that chat.
     */
    private var handledRequest = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Created...")

        handledRequest = savedInstanceState?.getLong(STATE_HANDLED_REQUEST, -1L) ?: -1L
        readOpenChat(intent)

        serviceConnection.bindService()
        serviceConnection.startForegroundService()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = TRANSPARENT),
        )

        setContent {
            RequestNotificationPermission()

            val imageLoader = rememberVapullaImageLoader(LocalContext.current)

            val settings by settingsManager.settings.collectAsStateWithLifecycle(AppSettings())

            CompositionLocalProvider(
                LocalCoilImageLoader provides imageLoader,
                LocalAppSettings provides settings,
            ) {
                VapullaTheme(
                    isDarkTheme = settings.darkTheme,
                    useDynamicColor = settings.dynamicColor,
                ) {
                    val openChat by chatToOpen.collectAsStateWithLifecycle()

                    NavigationRoot(
                        modifier = Modifier.fillMaxSize(),
                        openChat = openChat,
                        onChatOpened = { chatToOpen.value = null },
                    )
                }
            }
        }
    }

    /** A notification tapped while the activity already exists arrives here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readOpenChat(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_HANDLED_REQUEST, handledRequest)
    }

    private fun readOpenChat(intent: Intent?) {
        val friendId = intent?.getLongExtra(EXTRA_OPEN_CHAT, 0L) ?: 0L
        val requestTime = intent?.getLongExtra(EXTRA_REQUEST_TIME, 0L) ?: 0L
        Timber.d("readOpenChat: friend=$friendId request=$requestTime handled=$handledRequest")

        // A recreated activity is handed the intent it was first started with, which may be a
        // tap it acted on long ago. Only a tap newer than the last one handled counts.
        if (friendId != 0L && requestTime > handledRequest) {
            handledRequest = requestTime
            chatToOpen.value = friendId
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection.unBindService()
    }
}

/** Prompts once for POST_NOTIFICATIONS if it is not already granted. */
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

/**
 * Coil loader shared by every image in the app: a small memory and disk cache plus decoders
 * for Steam's `.ico` client icons and animated PNG avatars.
 */
@Composable
private fun rememberVapullaImageLoader(context: Context): ImageLoader = remember(context) {
    ImageLoader.Builder(context)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.1)
                .strongReferencesEnabled(true)
                .build()
        }
        .diskCachePolicy(CachePolicy.ENABLED)
        .diskCache {
            DiskCache.Builder()
                .maxSizePercent(0.03)
                .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                .build()
        }
        .components {
            add(IconDecoder.Factory())
            add(AnimatedPngDecoder.Factory())
        }
        .build()
}
