package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.compose.components.PaperPlane
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getFriendName
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getLastMessageTime
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.compose.util.getUnreadMessageCount

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    friend: FriendListItem,
    onClickChat: () -> Unit,
    onClickProfile: () -> Unit,
    onClickAccept: () -> Unit,
    onClickIgnore: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val onClick = remember {
        {
            if (!friend.isRequestRecipient()) onClickChat()
        }
    }

    val onLongClick = remember {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClickProfile()
        }
    }

    Column(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        val context = LocalContext.current

        val avatarUrl = remember(friend) { getAvatarUrl(friend.avatar) }
        val friendName = remember(friend) { getFriendName(friend = friend) }
        val statusColor = remember(friend) { getStatusColor(friend) }
        val statusIcon = remember(friend) { getStatusIcon(friend) }
        val statusText = remember(friend) { context.getStatusText(friend) }

        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f, false),
                        text = friendName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    statusIcon?.let {
                        Icon(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(12.dp),
                            imageVector = it,
                            contentDescription = it.name
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (friend.lastMessage != null && !friend.isRequestRecipient()) {
                        PaperPlane(
                            text = friend.lastMessage!!,
                            isPreviewMode = true,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            trailingContent = {
                if (friend.isRequestRecipient()) {
                    Row {
                        IconButton(onClick = onClickAccept) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept"
                            )
                        }
                        IconButton(onClick = onClickIgnore) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ignore"
                            )
                        }
                    }
                } else if ((friend.newMessageCount ?: 0) > 0) {
                    // New Messages
                    val msgCount = remember(friend) {
                        getUnreadMessageCount(friend.newMessageCount)
                    }

                    Surface(
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            text = msgCount,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Read Messages, show last time
                    val time = remember(friend) { getLastMessageTime(friend = friend).toString() }
                    Text(
                        text = time,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Surface(
                    shape = iconSmallCornerShape,
                    color = statusColor
                ) {
                    StaticImage(
                        modifier = Modifier
                            .padding(1.dp)
                            .clip(iconSmallCornerShape)
                            .size(58.dp),
                        url = avatarUrl
                    )
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
                    lastMessage = "Left 4 Dead 2 is so fun!",
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
