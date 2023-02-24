package `in`.dragonbra.vapulla.compose.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.icons.VR
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendAwayOrSnooze
import `in`.dragonbra.vapulla.compose.ui.theme.friendInGame
import `in`.dragonbra.vapulla.compose.ui.theme.friendInGameAwayOrSnooze
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.friendOnline
import `in`.dragonbra.vapulla.util.Utils
import timber.log.Timber
import java.text.DateFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FriendItem(
    modifier: Modifier = Modifier,
    friend: FriendListItem,
    onClickChat: (friend: FriendListItem) -> Unit,
    onClickProfile: (friend: FriendListItem) -> Unit,
    onClickAccept: (friend: FriendListItem) -> Unit,
    onClickIgnore: (friend: FriendListItem) -> Unit,
    onClickBlock: (friend: FriendListItem) -> Unit
) {
    if (friend.isRequestRecipient()) {
        // TODO display a friend request
        return
    }

    // Order is important
    val color = when {
        friend.isOffline() -> friendOffline
        friend.isInGameAwayOrSnooze() -> friendInGameAwayOrSnooze
        friend.isAwayOrSnooze() -> friendAwayOrSnooze
        friend.isInGame() -> friendInGame
        friend.isOnline() -> friendOnline
        else -> friendOffline
    }
    Timber.w("Friend ${friend.name} is $color")

    val context = LocalContext.current
    Column(
        modifier = modifier
            .combinedClickable(
                onClick = { onClickChat(friend) },
                onLongClick = { onClickProfile(friend) }
            )
    ) {
        val colorState by remember(friend.id) { mutableStateOf(color) }
        ListItem(
            headlineText = {
                val hasNickname = !Strings.isNullOrEmpty(friend.nickname)
                val name = if (hasNickname) {
                    // TODO Make * Offline Grey
                    friend.nickname + "*"
                } else {
                    friend.name
                }

                Text(
                    text = name!!,
                    color = colorState,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingText = {
                val statusText: String
                val messageText: String?
                val state = friend.state.let { EPersonaState.from(it) }

                val isTypingFromLastMessage = friend.typingTs > (friend.lastMessageTime ?: 0)
                val isTyping = friend.typingTs > (System.currentTimeMillis() - 20000L)

                if ((isTypingFromLastMessage) && isTyping) {
                    // TODO animate 'Typing' text?
                    statusText = stringResource(id = R.string.statusTyping)
                } else {
                    if (friend.state == EPersonaState.Offline.code()) {
                        val offlineDate = DateUtils.getRelativeTimeSpanString(
                            friend.lastLogOff,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        )

                        statusText = stringResource(id = R.string.statusOffline, offlineDate)
                    } else {
                        val status = Utils.getStatusText(
                            context,
                            state,
                            friend.gameAppId,
                            friend.gameName,
                            friend.lastLogOff
                        )
                        statusText = status
                    }
                }

                // TODO: Messages + PaperPlane for URL, Stickers, and Emojis
                messageText = friend.lastMessage

                Column {
                    Text(
                        text = statusText,
                        color = colorState,
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
                val newMessages = friend.newMessageCount ?: 0
                if (newMessages > 0) {
                    // New Messages
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                        Text(
                            modifier = Modifier.padding(6.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            text = newMessages.toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    // Read Messages, show last time
                    val time = DateUtils.formatSameDayTime(
                        friend.lastMessageTime ?: 0,
                        System.currentTimeMillis(),
                        DateFormat.SHORT,
                        DateFormat.SHORT
                    )
                    Text(
                        text = time.toString(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingContent = {
                Surface(
                    modifier = Modifier.size(58.dp),
                    shape = RectangleShape,
                    color = colorState
                ) {
                    Box {
                        CoilImage(
                            modifier = Modifier.size(58.dp),
                            imageRequest = {
                                ImageRequest.Builder(context)
                                    .data(Utils.getAvatarUrl(friend.avatar))
                                    .placeholder(R.drawable.vapulla)
                                    .crossfade(true)
                                    .build()
                            },
                            previewPlaceholder = R.mipmap.ic_launcher_foreground,
                            imageOptions = ImageOptions(
                                requestSize = IntSize(56, 56)
                            )
                        )

                        val state = friend.state.let { EPersonaState.from(it) }
                        val flags = EPersonaStateFlag.from(friend.stateFlags)
                        val icon = if (state == EPersonaState.Away) {
                            Icons.Default.Bedtime
                        } else if (flags.contains(EPersonaStateFlag.ClientTypeVR)) {
                            Icons.Default.VR
                        } else if (flags.contains(EPersonaStateFlag.LaunchTypeGamepad)) {
                            Icons.Default.Gamepad
                        } else if (flags.contains(EPersonaStateFlag.ClientTypeMobile)) {
                            Icons.Default.Smartphone
                        } else if (flags.contains(EPersonaStateFlag.ClientTypeWeb)) {
                            Icons.Default.Web
                        } else {
                            null
                        }

                        icon?.let {
                            Icon(
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd),
                                imageVector = icon,
                                contentDescription = icon.name
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
    VapullaTheme {
        fun friendItem(
            id: Long,
            personaName: String,
            personaState: EPersonaState,
            gameAppId: Int = 0,
            gameName: String = ""
        ): FriendListItem {
            return FriendListItem(
                avatar = null,
                gameAppId = gameAppId,
                gameName = gameName,
                id = id,
                lastLogOff = 0,
                lastLogOn = 0,
                lastMessage = "Sup, wanna play?",
                lastMessageTime = 0,
                name = personaName,
                newMessageCount = 50,
                nickname = null,
                relation = EFriendRelationship.Friend.code(),
                state = personaState.code(),
                stateFlags = 512,
                typingTs = 0
            )
        }

        VapullaTheme {
            Surface {
                Column {
                    FriendItem(
                        friend = friendItem(1L, "Friend Online", EPersonaState.Online),
                        onClickChat = {},
                        onClickProfile = {},
                        onClickAccept = {},
                        onClickIgnore = {},
                        onClickBlock = {}
                    )
                    FriendItem(
                        friend = friendItem(2L, "Friend Away", EPersonaState.Away),
                        onClickChat = {},
                        onClickProfile = {},
                        onClickAccept = {},
                        onClickIgnore = {},
                        onClickBlock = {}
                    )
                    FriendItem(
                        friend = friendItem(3L, "Friend Offline", EPersonaState.Offline),
                        onClickChat = {},
                        onClickProfile = {},
                        onClickAccept = {},
                        onClickIgnore = {},
                        onClickBlock = {}
                    )
                    FriendItem(
                        friend = friendItem(
                            4L,
                            "Friend In Game",
                            EPersonaState.Online,
                            440,
                            "Team Fortress 2"
                        ),
                        onClickChat = {},
                        onClickProfile = {},
                        onClickAccept = {},
                        onClickIgnore = {},
                        onClickBlock = {}
                    )
                    FriendItem(
                        friend = friendItem(
                            5L,
                            "Friend Away In Game",
                            EPersonaState.Away,
                            440,
                            "Team Fortress 2"
                        ),
                        onClickChat = {},
                        onClickProfile = {},
                        onClickAccept = {},
                        onClickIgnore = {},
                        onClickBlock = {}
                    )
                }
            }
        }
    }
}
