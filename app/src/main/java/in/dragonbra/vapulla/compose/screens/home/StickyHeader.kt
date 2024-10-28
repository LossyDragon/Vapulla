package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.model.FriendListItem

@Composable
fun StickyHeaderItem(isCollapsed: Boolean, header: String, count: Int, onHeaderAction: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = "$header ($count)") },
        trailingContent = {
            val button = when (isCollapsed) {
                true -> Icons.Outlined.KeyboardArrowDown
                else -> Icons.Outlined.KeyboardArrowUp
            }
            IconButton(onClick = onHeaderAction) {
                Icon(imageVector = button, contentDescription = null)
            }
        }
    )
}

@Preview
@Composable
private fun Preview_StickyHeaderItem() {
    val friend = FriendListItem(
        id = 0,
        state = EPersonaState.Online.code(),
        gameAppId = 440,
        gameName = "Team Fortess 2",
        name = "Name The Game",
    )
    VapullaTheme {
        Column {
            StickyHeaderItem(isCollapsed = true, header = "Online", count = 60, onHeaderAction = {})
            FriendItem(
                friend = friend,
                onClickChat = {},
                onClickProfile = {}
            )
        }
    }
}
