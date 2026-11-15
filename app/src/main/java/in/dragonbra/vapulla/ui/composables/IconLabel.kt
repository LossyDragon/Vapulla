package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** The content of a button with an icon before its label, at Material's size and spacing. */
@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.IconLabel(icon: ImageVector, text: String) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(text = text)
}

@Preview
@Composable
private fun Preview_IconLabel() {
    VapullaTheme {
        Surface {
            Button(onClick = {}) {
                IconLabel(icon = Icons.Outlined.Edit, text = "Change name")
            }
        }
    }
}
