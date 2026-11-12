package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeString

/**
 * One friend row, styled as a Material 3 Expressive grouped list item: a tonal container whose
 * [shape] the caller picks by position, presence shown by the avatar dot and the status line.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendListItem(
    friend: SteamFriend,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FriendAvatar(friend = friend)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // The name stays neutral; the status line alone carries the presence colour,
                // which keeps a row from turning into a block of green or blue.
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                ) {
                    FriendName(friend = friend)
                }

                Text(
                    text = friend.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (friend.isOffline && !friend.isBlocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        friend.statusColor.copy(alpha = 1f)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FriendTrailingContent(friend = friend)
        }
    }
}

@Composable
private fun FriendTrailingContent(friend: SteamFriend) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (friend.lastMessageTime > 0) {
            Text(
                text = friend.lastMessageTime.toTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (friend.newMessageCount > 0) {
            // A rounded square echoing the avatar: half its size, so half its corner radius
            // (the avatar is 52dp with the large shape, this is 26dp with the small one).
            // Material's Badge is a fixed pill meant for sitting on an icon, hence a Surface.
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 26.dp, minHeight = 26.dp)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${friend.newMessageCount}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FriendListItem(
                    friend = SteamFriend(
                        id = 0,
                        name = "Actual Name",
                        nickname = "Nick Name",
                        state = EPersonaState.Away,
                        newMessageCount = 100,
                        lastMessage = "A Message",
                        lastMessageTime = 1760722982L,
                    ),
                    onClick = {},
                    onLongClick = {},
                )
                FriendListItem(
                    friend = SteamFriend(
                        id = 1,
                        name = "Gamer",
                        state = EPersonaState.Online,
                        gameAppID = 440,
                        gameName = "Team Fortress 2",
                    ),
                    onClick = {},
                    onLongClick = {},
                )
            }
        }
    }
}
