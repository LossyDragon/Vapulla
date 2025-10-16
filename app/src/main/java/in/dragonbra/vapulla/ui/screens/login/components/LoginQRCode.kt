package `in`.dragonbra.vapulla.ui.screens.login.components

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var isCodeEmpty by rememberSaveable { mutableStateOf(false) }
    val painter = rememberQrCodePainter(
        data = code,
        shapes = QrShapes(
            darkPixel = QrPixelShape.roundCorners(radius = .25f),
            ball = QrBallShape.roundCorners(radius = .25f),
            frame = QrFrameShape.roundCorners(corner = .25f)
        )
    )

    LaunchedEffect(key1 = code) {
        isCodeEmpty = code.isBlank()
    }

    Card {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Crossfade(isWaitingForConfirmation) { value ->
                when (value) {
                    true -> {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            LoadingIndicator(modifier = Modifier.size(150.dp))

                            Text(text = "Use the Steam Mobile App\nto confirm your sign in...")
                        }
                    }

                    false -> {
                        if (isCodeEmpty) {
                            LoadingIndicator(modifier = Modifier.size(150.dp))
                        } else {
                            Image(
                                modifier = Modifier
                                    .size(150.dp)
                                    .background(Color.White),
                                painter = painter,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onQrCodeCancel,
                content = {
                    Text(text = "Cancel")
                }
            )
        }
    }
}

class QrCodeStateProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview(
    @PreviewParameter(QrCodeStateProvider::class) value: Boolean
) {
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