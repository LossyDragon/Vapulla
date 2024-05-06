package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.MarkUnreadChatAlt
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.components.PaperPlane
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getFriendName
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.model.FriendListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    friend: FriendListItem,
    onClickChat: () -> Unit,
    onClickProfile: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val onClick = remember {
        {
            if (!friend.isRequestRecipient) onClickChat()
        }
    }

    val onLongClick = remember {
        {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClickProfile()
        }
    }

    val statusColor = remember(friend) { getStatusColor(friend) }
    ListItem(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        leadingContent = {
            Surface(
                shape = iconSmallCornerShape,
                color = statusColor
            ) {
                val avatarUrl = remember(friend) { getAvatarUrl(friend.avatar) }
                StaticImage(
                    modifier = Modifier
                        .padding(1.dp)
                        .clip(iconSmallCornerShape)
                        .size(58.dp),
                    url = avatarUrl
                )
            }
        },
        headlineContent = {
            val friendName = remember(friend) { getFriendName(friend = friend) }
            val statusIcon = remember(friend) { getStatusIcon(friend) }
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
            if (friend.lastMessage != null && !friend.isRequestRecipient) {
                PaperPlane(
                    text = friend.lastMessage!!,
                    isPreviewMode = true,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val context = LocalContext.current
                val statusText = remember(friend) { context.getStatusText(friend) }
                Text(
                    text = statusText,
                    color = statusColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = {
            val icon = when {
                friend.isRequestRecipient -> Icons.Outlined.PersonAddAlt1
                friend.isUnread -> Icons.Outlined.MarkUnreadChatAlt
                else -> Icons.Outlined.ChatBubbleOutline
            }
            IconButton(onClick = onClickChat) {
                Icon(imageVector = icon, contentDescription = null)
            }
        }
    )
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
                    id = 0,
                    lastMessage = "Left 4 Dead 2 is so fun!",
                    name = "New Friend Request",
                    nickname = "New Friend Request",
                    relation = EFriendRelationship.RequestRecipient.code(),
                    state = EPersonaState.Offline.code()
                ),
                onClickChat = {},
                onClickProfile = {}
            )

            friendData.onEachIndexed { index, entry ->
                FriendItem(
                    friend = FriendListItem(
                        gameAppId = if (index < 3) 0 else index,
                        gameName = if (index < 3) null else "Team Fortress 2",
                        id = index.toLong(),
                        lastMessage = "Left 4 Dead 2 is so fun!",
                        name = entry.key,
                        newMessageCount = if (index == 0) 0 else 27.times(index + 1),
                        nickname = entry.key,
                        relation = EFriendRelationship.Friend.code(),
                        state = entry.value.code(),
                        stateFlags = 512.times(index + 1)
                    ),
                    onClickChat = {},
                    onClickProfile = {}
                )
            }
        }
    }
}
