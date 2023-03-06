package `in`.dragonbra.vapulla.compose.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.Options
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.apng.decode.APNGParser
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.icons.VR
import `in`.dragonbra.vapulla.core.Constants

/**
 * Helper functions that relate to Images/Icons
 */

@Composable
fun getStatusIcon(friend: FriendListItem?): ImageVector? {
    val flags = EPersonaStateFlag.from(friend?.stateFlags ?: 0)
    return when {
        friend?.isRequestRecipient() == true -> Icons.Default.PersonAdd
        friend?.isAwayOrSnooze() == true -> Icons.Default.Bedtime
        flags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
        flags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
        flags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
        flags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
        else -> null
    }
}

@Composable
fun StaticImage(
    modifier: Modifier,
    url: String
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
                    .data(url)
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
    url: String
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
                    .data(url)
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
    CoilImage(
        modifier = Modifier.size(width, height),
        imageLoader = { imageLoader },
        imageRequest = {
            ImageRequest.Builder(context)
                .data(String.format(Constants.GAME_LOGO_URL, appId))
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
                    imageVector = Icons.Default.Error,
                    contentDescription = null
                )
            }
        }
    )
}

/**
 * Coil Factory Extension for Animated PNGs
 */
class AnimatedPngDecoder(private val source: ImageSource) : Decoder {
    override suspend fun decode(): DecodeResult {
        return DecodeResult(
            drawable = APNGDrawable.fromFile(source.file().toString()),
            isSampled = false
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            val path = result.source.file().toFile().path
            if (APNGParser.isAPNG(path)) {
                return AnimatedPngDecoder(result.source)
            }

            return null
        }
    }
}
