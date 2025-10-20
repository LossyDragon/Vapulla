package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun FriendListHeader(
    modifier: Modifier = Modifier,
    isCollapsed: Boolean,
    @StringRes header: Int,
    count: Int,
    onHeaderAction: () -> Unit,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onHeaderAction),
        headlineContent = { Text(text = stringResource(header) + " ($count)") },
        trailingContent = {
            IconButton(onClick = onHeaderAction) {
                Icon(
                    imageVector = if (isCollapsed) {
                        Icons.Outlined.KeyboardArrowDown
                    } else {
                        Icons.Outlined.KeyboardArrowUp
                    },
                    contentDescription = null
                )
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_StickyHeaderItem() {
    VapullaTheme {
        Column {
            FriendListHeader(
                isCollapsed = true,
                header = R.string.headerFriendInGame,
                count = 60,
                onHeaderAction = {}
            )
            FriendListItem(
                friend = SteamFriend(
                    id = 0,
                    state = EPersonaState.Online,
                    gameAppID = 440,
                    gameName = "Team Fortress 2",
                    name = "Name The Game",
                ),
            )
        }
    }
}