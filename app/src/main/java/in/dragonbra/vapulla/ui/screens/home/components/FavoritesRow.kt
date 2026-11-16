package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val AVATAR_SIZE = 60.dp

/**
 * The favourite friends in a row that scrolls sideways: the avatar, and under it the name
 * in the colour of what the friend is doing. A tap opens the chat and a long press the
 * profile, as on a row of the list below.
 *
 * @param contentPadding lets the row run to the screen's edges while its first avatar lines
 *   up with the list.
 */
@Composable
fun FavoritesRow(
    favorites: ImmutableList<SteamFriend>,
    onClick: (SteamFriend) -> Unit,
    onLongClick: (SteamFriend) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(favorites, key = { it.id }) { friend ->
            Column(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .combinedClickable(
                        onClick = { onClick(friend) },
                        onLongClick = { onLongClick(friend) },
                    )
                    .padding(6.dp)
                    .width(AVATAR_SIZE + 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FriendAvatar(friend = friend, size = AVATAR_SIZE)
                Text(
                    text = friend.nickname.ifBlank { friend.name },
                    style = MaterialTheme.typography.labelMedium,
                    color = friend.statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            FavoritesRow(
                favorites = persistentListOf(
                    SteamFriend(id = 1, name = "Gabe", state = EPersonaState.Online),
                    SteamFriend(id = 2, name = "A rather long name", state = EPersonaState.Away),
                    SteamFriend(id = 3, name = "Offline friend"),
                    SteamFriend(
                        id = 4,
                        name = "In game",
                        state = EPersonaState.Online,
                        gameAppID = 440,
                    ),
                ),
                onClick = {},
                onLongClick = {},
                contentPadding = PaddingValues(horizontal = 16.dp),
            )
        }
    }
}
