package `in`.dragonbra.vapulla

import android.Manifest
import android.content.Context
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
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.NavigationRoot
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import `in`.dragonbra.vapulla.util.decoders.IconDecoder
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Single activity. Binds and starts [in.dragonbra.vapulla.service.SteamService], asks for the
 * notification permission, and hosts the Compose navigation root with a shared image loader.
 */
class MainActivity : ComponentActivity() {

    private val serviceConnection: ServiceConnection by inject()
    private val settingsManager: SettingsManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Created...")

        serviceConnection.bindService()
        serviceConnection.startForegroundService()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(scrim = TRANSPARENT, darkScrim = TRANSPARENT),
        )

        setContent {
            RequestNotificationPermission()

            val imageLoader = rememberVapullaImageLoader(LocalContext.current)

            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                val settings by settingsManager.settings.collectAsStateWithLifecycle(AppSettings())

                VapullaTheme(
                    isDarkTheme = settings.darkTheme,
                    useDynamicColor = settings.dynamicColor,
                ) {
                    NavigationRoot(modifier = Modifier.fillMaxSize())
                }
            }
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
