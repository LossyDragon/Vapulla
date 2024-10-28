package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallerCornerShape
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getLastMessageTime
import `in`.dragonbra.vapulla.compose.util.getStatusIcon
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.compose.util.getUnreadMessageCount
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
    val context = LocalContext.current

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

    val statusColor = remember { getStatusColor(friend) }
    ListItem(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        headlineContent = {
            Text(
                text = buildAnnotatedString {
                    append(friend.friendName)
                    append(" ")
                    appendInlineContent("icon", "[icon]")
                },
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                inlineContent = mapOf(
                    "icon" to InlineTextContent(
                        Placeholder(
                            width = 14.sp,
                            height = 14.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        getStatusIcon(friend)?.let {
                            Icon(imageVector = it, contentDescription = it.name)
                        }
                    }
                )
            )
        },
        supportingContent = {
            // TODO most recent chat msg
            Text(
                text = context.getStatusText(friend),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            CoilImage(
                modifier = Modifier
                    .size(56.dp)
                    .background(statusColor, iconSmallerCornerShape)
                    .padding(2.dp)
                    .clip(iconSmallerCornerShape),
                imageRequest = {
                    ImageRequest.Builder(context)
                        .data(getAvatarUrl(friend.avatar))
                        .crossfade(true)
                        .build()
                },
                previewPlaceholder = painterResource(R.drawable.vapulla),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.background
                    )
                }
            )
        },
        trailingContent = {
            if (friend.isUnread) {
                val count = remember { getUnreadMessageCount(friend.newMessageCount) }
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(text = count)
                }
            } else if (friend.isRequestRecipient) {
                Icon(
                    imageVector = Icons.Default.PersonAddAlt1,
                    contentDescription = null
                )
            } else if (friend.lastMessage != null) {
                val time = getLastMessageTime(friend)
                Text(text = time.toString())
            } else {
                null
            }
        }
    )
    HorizontalDivider()
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
