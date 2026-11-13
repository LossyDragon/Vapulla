package `in`.dragonbra.vapulla.ui.composables.dialog

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun MessageDialog(
    visible: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    onConfirmClick: (() -> Unit)? = null,
    onDismissClick: (() -> Unit)? = null,
    confirmBtnText: String = "OK",
    dismissBtnText: String = "Cancel",
    icon: ImageVector? = null,
    title: String? = null,
    message: String? = null,
    useHtmlInMsg: Boolean = false,
) {
    when {
        visible -> {
            AlertDialog(
                icon = icon?.let { { Icon(imageVector = icon, contentDescription = title) } },
                title = title?.let { { Text(text = it) } },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        message?.let {
                            if (useHtmlInMsg) {
                                Text(
                                    text = AnnotatedString.fromHtml(
                                        htmlString = it,
                                        linkStyles = TextLinkStyles(
                                            style = SpanStyle(
                                                textDecoration = TextDecoration.Underline,
                                                fontStyle = FontStyle.Italic,
                                                color = Color.Blue,
                                            ),
                                        ),
                                    ),
                                )
                            } else {
                                Text(text = it)
                            }
                        }
                    }
                },
                onDismissRequest = { onDismissRequest?.invoke() },
                dismissButton = onDismissClick?.let {
                    {
                        TextButton(onClick = it) {
                            Text(text = dismissBtnText)
                        }
                    }
                },
                confirmButton = {
                    onConfirmClick?.let {
                        TextButton(onClick = it) {
                            Text(text = confirmBtnText)
                        }
                    }
                },
            )
        }
    }
}

private class ShortLorem : LoremIpsum(40)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_MessageDialog(@PreviewParameter(ShortLorem::class) text: String) {
    VapullaTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MessageDialog(
                visible = true,
                icon = Icons.Default.Gamepad,
                title = "Message Title",
                message = text,
                onDismissRequest = {},
                onDismissClick = {},
                onConfirmClick = {},
            )
        }
    }
}
