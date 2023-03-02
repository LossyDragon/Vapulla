package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.util.AvatarImage
import `in`.dragonbra.vapulla.compose.util.friendNameBuilder
import `in`.dragonbra.vapulla.compose.util.getLastMessageTime
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.compose.util.getUnreadMessageCount
import `in`.dragonbra.vapulla.util.Utils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    friend: FriendListItem,
    onClickChat: (friend: FriendListItem) -> Unit,
    onClickProfile: (friend: FriendListItem) -> Unit,
    onClickAccept: (friend: FriendListItem) -> Unit,
    onClickIgnore: (friend: FriendListItem) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickChat(friend) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClickProfile(friend)
                }
            )
    ) {
        ListItem(
            headlineText = {
                Text(
                    text = friendNameBuilder(friend = friend),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingText = {
                // TODO: Messages + PaperPlane for URL, Stickers, and Emojis
                val messageText: String? = friend.lastMessage

                Column {
                    Text(
                        text = getStatusText(friend),
                        color = getStatusColor(friend),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    messageText?.let {
                        Text(
                            text = messageText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            trailingContent = {
                if (friend.isRequestRecipient()) {
                    Row {
                        IconButton(onClick = { onClickAccept(friend) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept"
                            )
                        }
                        IconButton(onClick = { onClickIgnore(friend) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ignore"
                            )
                        }
                    }
                } else if ((friend.newMessageCount ?: 0) > 0) {
                    // New Messages
                    Surface(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            text = getUnreadMessageCount(friend.newMessageCount),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Read Messages, show last time
                    Text(
                        text = getLastMessageTime(friend = friend).toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RectangleShape,
                    color = getStatusColor(friend)
                ) {
                    Box {
                        AvatarImage(
                            modifier = Modifier.size(58.dp),
                            avatarUrl = Utils.getAvatarUrl(friend.avatar)
                        )

                        getStatusIcon(friend)?.let {
                            Icon(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd),
                                imageVector = it,
                                contentDescription = it.name
                            )
                        }
                    }
                }
            }
        )
        Divider()
    }
}

@Preview
@Composable
private fun Preview_FriendListItem() {
    val friendData = mapOf(
        "Friend Online" to EPersonaState.Online,
        "Friend Away" to EPersonaState.Away,
        "Friend Offline" to EPersonaState.Offline,
        "Friend In Game" to EPersonaState.Online,
        "Friend Away In Game" to EPersonaState.Away
    )
    VapullaTheme {
        Column {
            FriendItem(
                friend = FriendListItem(
                    avatar = null,
                    gameAppId = 0,
                    gameName = null,
                    id = 0,
                    lastLogOff = 0,
                    lastLogOn = 0,
                    lastMessage = null,
                    lastMessageTime = null,
                    name = "New Friend Request",
                    newMessageCount = null,
                    nickname = "New Friend Request",
                    relation = EFriendRelationship.RequestRecipient.code(),
                    state = EPersonaState.Offline.code(),
                    stateFlags = 0,
                    typingTs = 0
                ),
                onClickChat = {},
                onClickProfile = {},
                onClickAccept = {},
                onClickIgnore = {}
            )

            friendData.onEachIndexed { index, entry ->
                FriendItem(
                    friend = FriendListItem(
                        avatar = null,
                        gameAppId = index - 2,
                        gameName = if (index - 2 < 0) null else "Team Fortress 2",
                        id = index.toLong(),
                        lastLogOff = 0,
                        lastLogOn = 0,
                        lastMessage = "Left 4 Dead 2 is so fun!",
                        lastMessageTime = 0,
                        name = entry.key,
                        newMessageCount = 27.times(index + 1),
                        nickname = entry.key,
                        relation = EFriendRelationship.Friend.code(),
                        state = entry.value.code(),
                        stateFlags = 512.times(index + 1),
                        typingTs = 0
                    ),
                    onClickChat = {},
                    onClickProfile = {},
                    onClickAccept = {},
                    onClickIgnore = {}
                )
            }
        }
    }
}
