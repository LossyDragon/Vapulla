package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** The top bar button of a top-level screen; it opens the navigation drawer. */
@Composable
fun MenuButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "Open navigation")
    }
}

@Preview
@Composable
private fun Preview_MenuButton() {
    VapullaTheme {
        Surface {
            MenuButton(onClick = {})
        }
    }
}
