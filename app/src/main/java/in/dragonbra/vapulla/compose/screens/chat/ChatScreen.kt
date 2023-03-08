package `in`.dragonbra.vapulla.compose.screens.chat

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.getStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getFriendName
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.compose.util.getStatusText
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {
    val state by viewModel.state.collectAsState()
    val activity = LocalActivity.current
    val context = LocalContext.current

    val onProfileClicked = remember<() -> Unit> {
        {
            val steamID = state.currentChatSteamID!!.convertToUInt64()
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.INTENT_STEAM_ID, steamID)
            }.also { context.startActivity(it) }
        }
    }

    ChatScreenContent(
        state = state,
        onBackPressed = { activity.finish() },
        onProfileClicked = onProfileClicked,
        onChatMessage = viewModel::sendMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChatScreenContent(
    state: ChatState,
    onBackPressed: () -> Unit,
    onProfileClicked: () -> Unit,
    onChatMessage: (String) -> Unit
) {
    val messages = state.messages.collectAsLazyPagingItems().itemSnapshotList.items
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    Surface(
        modifier = Modifier.windowInsetsPadding(
            WindowInsets
                .navigationBars
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        reverseLayout = true,
                        state = scrollState,
                        contentPadding = WindowInsets.statusBars.add(WindowInsets(top = 90.dp))
                            .asPaddingValues(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // NOTE: This should be in the VM, but some refacoring will be needed.
                        val groupedMessages = messages.groupBy { it.formattedTs }

                        groupedMessages.forEach { (header, items) ->
                            items(items, key = { it.id }) { msg ->
                                ChatMessageItem(
                                    modifier = Modifier.animateItemPlacement(),
                                    message = msg
                                )
                            }
                            stickyHeader(contentType = header) {
                                ChatMessageDateHeader(dateStamp = header)
                            }
                        }
                    }
                }

                UserInput(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                    emoticonList = state.emoticonData,
                    onMessageSent = onChatMessage,
                    onResetScroll = {
                        scope.launch {
                            scrollState.scrollToItem(0)
                        }
                    }
                )
            }

            // Empty Conversation Text
            if (messages.isEmpty()) {
                val name = state.friend?.friendName ?: "this friend."
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Center),
                    color = friendOffline,
                    text = "You have no recent messages with $name",
                    textAlign = TextAlign.Center
                )
            }

            // Top App Bar
            CenterAlignedTopAppBar(
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onProfileClicked) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Go to profile"
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.padding(start = 0.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val borderStroke = BorderStroke(1.dp, getStatusColor(state.friend))
                        StaticImage(
                            modifier = Modifier
                                .size(48.dp)
                                .border(borderStroke, iconSmallCornerShape)
                                .clip(iconSmallCornerShape),
                            url = getAvatarUrl(state.friend?.avatar)
                        )

                        Column(modifier = Modifier.padding(start = 6.dp)) {
                            val color = getStatusColor(state.friend)
                            Text(
                                color = color,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                text = getFriendName(state.friend)
                            )

                            // Get Game or Status
                            // TODO should make a timer to do the "is typing" work, timeout ~20 unless we're notified again
                            val lastMsg = state.friend?.lastMessage == null
                            val typingTs =
                                (state.friend?.typingTs ?: 0) > (state.friend?.lastMessageTime ?: 0)
                            val currentTs =
                                (state.friend?.typingTs ?: 0) > System.currentTimeMillis() - 15000L
                            val status = if ((lastMsg || typingTs) && currentTs) {
                                stringResource(id = R.string.statusTyping)
                            } else {
                                val isOnline = state.friend?.isInGame() == true
                                val isAway = state.friend?.isInGameAwayOrSnooze() == true
                                if (isOnline || isAway) {
                                    val gameName = state.friend?.gameName ?: "a game."
                                    stringResource(id = R.string.statusPlaying, gameName)
                                } else {
                                    val context = LocalContext.current
                                    context.getStatusText(state.friend)
                                }
                            }

                            Text(
                                color = color.copy(alpha = .60f),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                text = status
                            )
                        }
                    }
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ChatScreenContent() {
    val messages = mutableListOf<ChatMessage>()
    repeat(100) {
        val currentTime = System.currentTimeMillis()
        val randomTime = currentTime - Random.nextLong(currentTime)
        val time = if (it < 75) randomTime else if (it < 95) 1677647978791 else 1699999998791
        messages.add(
            ChatMessage(
                id = it.toLong(),
                message = "Sup\nBro $it",
                timestamp = time,
                accountid = 1,
                fromLocal = it.mod(2) == 0,
                isUnread = false
            )
        )
    }

    val state = ChatState(
        messages = flowOf(PagingData.from(messages)),
        friend = FriendListItem(
            avatar = "17683cb013b8f4cd6ef1d1b1aa47036da2413d8e",
            gameAppId = 100,
            gameName = "A Very Long Game Name that Should Ellipse At The End",
            name = "Lu",
            newMessageCount = 50,
            nickname = "A Very Long Friend Name that Should Ellipse",
            relation = EFriendRelationship.Friend.code(),
            state = EPersonaState.Online.code(),
            stateFlags = 512
        )
    )
    VapullaTheme {
        ChatScreenContent(
            state = state,
            onBackPressed = {},
            onProfileClicked = {},
            onChatMessage = {}
        )
    }
}
