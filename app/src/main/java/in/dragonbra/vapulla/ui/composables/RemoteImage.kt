package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/** How a [RemoteImage] is drawn and described. */
@Immutable
data class ImageOptions(
    val contentScale: ContentScale = ContentScale.Crop,
    val contentDescription: String? = null,
)

/**
 * An image from the network, loaded with the app's Coil loader (see `VapullaApplication`).
 *
 * The image takes the size of [modifier], so that has to set one. [loading] and [failure]
 * are stretched to that same size.
 *
 * @param imageModel what to load, usually a URL. Read once per composition.
 * @param previewPlaceholder drawn instead of the image in Android Studio previews.
 * @param loading shown until the image arrives.
 * @param failure shown when there is nothing to load or loading failed.
 */
@Composable
fun RemoteImage(
    imageModel: () -> Any?,
    modifier: Modifier = Modifier,
    imageOptions: ImageOptions = ImageOptions(),
    previewPlaceholder: Painter? = null,
    loading: (@Composable BoxScope.() -> Unit)? = null,
    failure: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        if (LocalInspectionMode.current && previewPlaceholder != null) {
            Image(
                painter = previewPlaceholder,
                contentDescription = imageOptions.contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = imageOptions.contentScale,
            )
            return@Box
        }

        val model = imageModel()
        var state by remember(model) {
            mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
        }

        AsyncImage(
            model = model,
            contentDescription = imageOptions.contentDescription,
            modifier = Modifier.matchParentSize(),
            onState = { state = it },
            contentScale = imageOptions.contentScale,
        )

        when (state) {
            is AsyncImagePainter.State.Success -> Unit
            is AsyncImagePainter.State.Error -> failure?.invoke(this)
            else -> loading?.invoke(this)
        }
    }
}
