package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import `in`.dragonbra.vapulla.data.entity.SteamFriend

@Composable
fun FriendListItem(
    modifier: Modifier = Modifier,
    friend: SteamFriend
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = friend.statusColor,
            supportingColor = friend.statusColor,
        ),
        headlineContent = {
            FriendName(friend= friend)
        },
        supportingContent = {
            Text(text = friend.isPlayingGameName)
        },
        leadingContent = {
            FriendAvatar(friend = friend)
        }
    )
}