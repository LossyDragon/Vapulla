package `in`.dragonbra.vapulla.ui.composables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState

/**
 * One image over a black backdrop that fills the screen. It can be pinched or double tapped
 * to zoom and dragged around; a tap hides the close button and [footer], another brings
 * them back. Back closes it.
 *
 * Place it last in a `Box` that fills the screen, so it is drawn over everything else.
 *
 * @param footer what to say about the image, along the bottom edge. Text should be white.
 */
@Composable
fun ImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    BackHandler(onBack = onDismiss)

    var showControls by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keeps taps from reaching what is underneath.
            .pointerInput(Unit) {}
            .systemBarsPadding(),
    ) {
        if (LocalInspectionMode.current) {
            Image(
                painter = painterResource(R.drawable.vapulla_background),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val imageState = rememberZoomableImageState()

            ZoomableAsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                state = imageState,
                onClick = { showControls = !showControls },
            )

            if (!imageState.isImageDisplayed) {
                LoadingBox(modifier = Modifier.fillMaxSize())
            }
        }

        if (showControls) {
            FilledTonalIconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            if (footer != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    content = footer,
                )
            }
        }
    }
}
