package `in`.dragonbra.vapulla.compose.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.util.Utils

@Composable
fun StaticImage(
    modifier: Modifier,
    avatarUrl: String
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()
    CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
        CoilImage(
            modifier = modifier,
            imageRequest = {
                ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .placeholder(R.drawable.vapulla)
                    .crossfade(true)
                    .build()
            },
            previewPlaceholder = R.mipmap.ic_launcher_foreground,
            imageOptions = ImageOptions(contentScale = ContentScale.Fit)
        )
    }
}

// Modifier.size(150.dp)
@Composable
fun StickerImage(
    modifier: Modifier = Modifier,
    stickerUrl: String
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.components {
            add(AnimatedPngDecoder.Factory())
        }.build()
    CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
        CoilImage(
            modifier = modifier,
            imageRequest = {
                ImageRequest.Builder(context)
                    .data(stickerUrl)
                    .placeholder(R.drawable.vapulla)
                    .crossfade(true)
                    .build()
            },
            previewPlaceholder = R.mipmap.ic_launcher_foreground,
            imageOptions = ImageOptions(contentScale = ContentScale.Fit)
        )
    }
}

@Composable
fun GameImage(appId: Int) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val height = with(density) { 215.toDp() }
    val width = with(density) { 460.toDp() }

    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()
    CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
        CoilImage(
            modifier = Modifier.size(width, height),
            imageRequest = {
                ImageRequest.Builder(context)
                    .data(String.format(Utils.GAME_LOGO_URL, appId))
                    .crossfade(true)
                    .build()
            },
            imageOptions = ImageOptions(
                contentScale = ContentScale.Fit
            ),
            previewPlaceholder = R.mipmap.ic_launcher_foreground,
            loading = {
                Box(
                    modifier = Modifier.size(width, height),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            failure = {
                Box(
                    modifier = Modifier.size(width, height),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(id = R.mipmap.ic_launcher_foreground),
                        contentDescription = null
                    )
                }
            }
        )
    }
}
