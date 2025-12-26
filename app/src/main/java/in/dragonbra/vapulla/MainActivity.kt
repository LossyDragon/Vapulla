package `in`.dragonbra.vapulla

import android.Manifest
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.NavigationRoot
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import `in`.dragonbra.vapulla.util.decoders.IconDecoder
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.inject
import org.koin.compose.getKoin
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val serviceConnection: ServiceConnection by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("Created...")

        serviceConnection.bindService()
        serviceConnection.startForegroundService()

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = TRANSPARENT,
                darkScrim = TRANSPARENT,
            ),
        )

        setContent {
            val context = LocalContext.current

            // Ask permission to post notifications.
            var hasNotificationPermission by remember {
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
                mutableStateOf(permission == PackageManager.PERMISSION_GRANTED)
            }
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { hasNotificationPermission = it },
            )
            LaunchedEffect(key1 = hasNotificationPermission) {
                if (!hasNotificationPermission) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Image Caching
            val imageLoader = remember {
                val memoryCache = MemoryCache.Builder(context)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()

                val diskCache = DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .build()

                ImageLoader.Builder(context)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache(memoryCache)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCache(diskCache)
                    .components {
                        add(IconDecoder.Factory())
                        add(AnimatedPngDecoder.Factory())
                    }
                    .build()
            }

            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                VapullaTheme {
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
