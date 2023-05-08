package `in`.dragonbra.vapulla.compose.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.ImageRequest
import coil.request.Options
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.apng.decode.APNGParser
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.icons.VR
import `in`.dragonbra.vapulla.model.FriendListItem

/**
 * Helper functions that relate to Images/Icons
 */
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
    imageLoader: ImageLoader,
    contentScale: ContentScale = ContentScale.Fit,
    url: String
) {
    val context = LocalContext.current
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
            imageOptions = ImageOptions(contentScale = contentScale)
        )
    }
}

// Modifier.size(150.dp)
@Composable
fun StickerImage(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    url: String
) {
    val context = LocalContext.current

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
