package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.ui.theme.colorAccent
import `in`.dragonbra.vapulla.util.Utils.toTimeString

@Composable
fun FriendListItem(
    modifier: Modifier = Modifier,
    friend: SteamFriend
) {
    val hasTrailingContent = friend.newMessageCount > 0 || friend.lastMessageTime > 0

    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = friend.statusColor,
            supportingColor = friend.statusColor,
        ),
        headlineContent = { FriendName(friend = friend) },
        supportingContent = { Text(text = friend.isPlayingGameName) },
        leadingContent = { FriendAvatar(friend = friend) },
        trailingContent = if (hasTrailingContent) {
            { FriendTrailingContent(friend = friend) }
        } else {
            null
        }
    )
}

@Composable
private fun FriendTrailingContent(friend: SteamFriend) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = friend.lastMessageTime.toTimeString())
        Spacer(modifier = Modifier.height(6.dp))
        if (friend.newMessageCount > 0) {
            Badge(
                containerColor = colorAccent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                content = { Text("${friend.newMessageCount}") }
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            FriendListItem(
                friend = SteamFriend(
                    id = 0,
                    name = "Actual Name",
                    nickname = "Nick Name",
                    state = EPersonaState.Away,
                    newMessageCount = 100,
                    lastMessage = "A Message",
                    lastMessageTime = 1760722982L,
                )
            )
        }
    }
}