package `in`.dragonbra.vapulla.ui.composables.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** One destination in the drawer. The icon is not described: the label beside it says it all. */
@Composable
internal fun DrawerItem(
    text: String,
    imageVector: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(text = text) },
        icon = { Icon(imageVector = imageVector, contentDescription = null) },
        selected = selected,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            DrawerItem(
                text = stringResource(R.string.app_name),
                imageVector = Icons.Default.Add,
                onClick = { },
            )
        }
    }
}
