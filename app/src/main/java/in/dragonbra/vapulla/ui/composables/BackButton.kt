package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** The top bar button of a screen that was opened from another; it goes back to that one. */
@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        content = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate Back",
            )
        },
    )
}

@Preview
@Composable
private fun Preview_BackButton() {
    VapullaTheme {
        Surface {
            BackButton(onClick = {})
        }
    }
}
