package `in`.dragonbra.vapulla.ui.screens.login.components

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginQRCode(
    code: String,
    isWaitingForConfirmation: Boolean,
    onQrCodeCancel: () -> Unit,
) {
    val painter = rememberQrCodePainter(
        data = code,
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(radius = .25f),
            ball = QrBallShape.roundCorners(radius = .25f),
            frame = QrFrameShape.roundCorners(corner = .25f),
        ),
        scale = 0.875f,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Crossfade(
            targetState = isWaitingForConfirmation || code.isBlank(),
            label = "qr_state",
        ) { busy ->
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    LoadingIndicator(modifier = Modifier.size(120.dp))
                } else {
                    // QR codes need a light quiet zone, so this stays white in both themes.
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color.White,
                    ) {
                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            painter = painter,
                            contentDescription = "Sign-in QR code",
                        )
                    }
                }
            }
        }

        Text(
            text = if (isWaitingForConfirmation) {
                "Confirm the sign-in in the Steam mobile app"
            } else {
                "Steam app > Steam Guard > Scan a QR code"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        OutlinedButton(
            onClick = onQrCodeCancel,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            Text(text = "Cancel", style = MaterialTheme.typography.titleMedium)
        }
    }
}

class QrCodeStateProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview(@PreviewParameter(QrCodeStateProvider::class) value: Boolean) {
    VapullaTheme {
        Surface {
            LoginQRCode(
                code = "Hello World",
                isWaitingForConfirmation = value,
                onQrCodeCancel = { },
            )
        }
    }
}
