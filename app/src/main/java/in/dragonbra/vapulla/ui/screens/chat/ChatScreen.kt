package `in`.dragonbra.vapulla.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatEvents
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatInputBox
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatMessageItem
import `in`.dragonbra.vapulla.ui.screens.chat.components.DayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.FOLLOW_THRESHOLD
import `in`.dragonbra.vapulla.ui.screens.chat.components.FloatingDayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.ReactionPickerSheet
import `in`.dragonbra.vapulla.ui.screens.chat.components.reactionOf
import `in`.dragonbra.vapulla.ui.screens.chat.components.rememberImagePicker
import `in`.dragonbra.vapulla.ui.screens.chat.components.startsNewDay
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
import `in`.dragonbra.vapulla.ui.screens.home.components.friendDisplayName
import `in`.dragonbra.vapulla.ui.screens.home.components.statusText
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** How long to wait for a sent message to appear before giving up on scrolling to it. */
private val SENT_SCROLL_TIMEOUT = 3.seconds

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit, onProfile: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val emoticons by viewModel.emoticons.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val textFieldState = rememberTextFieldState()

    // Every edit that leaves something in the field counts as typing. Clearing the field
    // (sending, deleting everything) does not.
    LaunchedEffect(viewModel, textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { text -> if (text.isNotBlank()) viewModel.onTyping() }
    }

    // The chat counts as open only while it is actually on screen, so messages that arrive
    // with the app in the background still notify and stay unread.
    LifecycleResumeEffect(viewModel) {
        viewModel.onChatVisible(true)
        onPauseOrDispose { viewModel.onChatVisible(false) }
    }

    ChatEvents(viewModel.sendFailures, viewModel.notices, textFieldState, snackbarHostState)

    val pickImage = rememberImagePicker(onPicked = viewModel::sendImage)

    ChatScreenContent(
        uiState = uiState,
        messages = messages,
        textFieldState = textFieldState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onProfile = onProfile,
        onSendClick = {
            viewModel.sendMessage(textFieldState.text.toString())
            textFieldState.clearText()
        },
        emoticons = emoticons,
        onStickerClick = viewModel::sendSticker,
        onAttachClick = pickImage,
        onReact = viewModel::react,
    )
}

@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    messages: LazyPagingItems<ChatMessage>,
    textFieldState: TextFieldState,
    snackbarHostState: SnackbarHostState,
    emoticons: ImmutableList<Emoticon>,
    onBack: () -> Unit,
    onProfile: () -> Unit,
    onSendClick: () -> Unit,
    onStickerClick: (Emoticon) -> Unit,
    onAttachClick: () -> Unit,
    onReact: (messageId: Long, reaction: String, isSticker: Boolean, add: Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }

    // The message a reaction is being picked for, by its row ID; null while none is.
    var reactingTo by rememberSaveable { mutableStateOf<Long?>(null) }

    reactingTo?.let { messageId ->
        ReactionPickerSheet(
            emoticons = emoticons,
            onPick = { emoticon ->
                onReact(messageId, reactionOf(emoticon), emoticon.isSticker, true)
                reactingTo = null
            },
            onDismiss = { reactingTo = null },
        )
    }

    // Keeping up with the conversation. A new message is inserted at index 0, and the lazy
    // list holds on to the item it was showing, so the new one lands just out of view. When
    // the newest message changes, scroll down to it if we sent it, or if the list is still
    // within FOLLOW_THRESHOLD items of the bottom; further up, the reader is left alone.
    var scrollToSentMessage by remember { mutableStateOf(false) }
    val newestMessageId = if (messages.itemCount > 0) messages.peek(0)?.id else null

    LaunchedEffect(newestMessageId) {
        if (newestMessageId == null) return@LaunchedEffect

        // The insert has already pushed the visible items one index further from the bottom.
        val isNearBottom = listState.firstVisibleItemIndex <= FOLLOW_THRESHOLD
        if (scrollToSentMessage || isNearBottom) {
            scrollToSentMessage = false
            listState.animateScrollToItem(0)
        }
    }

    // A send that never reaches the list (not signed in, say) must not leave the flag set
    // and yank the list down on some later incoming message.
    LaunchedEffect(scrollToSentMessage) {
        if (scrollToSentMessage) {
            delay(SENT_SCROLL_TIMEOUT)
            scrollToSentMessage = false
        }
    }

    val showScrollToBottom by remember {
        derivedStateOf {
            val isAtBottom = listState.firstVisibleItemIndex == 0
            !isAtBottom
        }
    }

    val hasUnreadMessages by remember {
        derivedStateOf {
            messages.itemSnapshotList.items.any { it.unread && !it.fromLocal }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        FriendAvatar(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            size = 48.dp,
                            friend = uiState.friend ?: SteamFriend(id = 0),
                        )
                        Column {
                            Text(
                                text = uiState.friend?.let { friendDisplayName(it) }
                                    ?: AnnotatedString(""),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            // Typing replaces the presence line for as long as it lasts.
                            Text(
                                text = if (uiState.isFriendTyping) {
                                    "typing…"
                                } else {
                                    uiState.friend?.statusText.orEmpty()
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (uiState.isFriendTyping) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "View profile")
                    }
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (showScrollToBottom || hasUnreadMessages) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    content = {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                        )
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatInputBox(
                textFieldState = textFieldState,
                emoticons = emoticons,
                showEmojiPanel = showEmojiPanel,
                onEmojiPanelChange = { showEmojiPanel = it },
                onStickerClick = { sticker ->
                    onStickerClick(sticker)
                    scrollToSentMessage = true
                },
                isUploading = uiState.isUploading,
                onAttachClick = onAttachClick,
                onSendClick = {
                    onSendClick()
                    // The message is not in the list yet; follow it down once it lands.
                    scrollToSentMessage = true
                },
            )
        },
        content = { paddingValues ->
            // The day of the message at the top of the screen, for the floating header. The
            // list is reversed, so that is the last visible item.
            val topTimestamp by remember(messages) {
                derivedStateOf {
                    listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?.takeIf { it < messages.itemCount }
                        ?.let { messages.peek(it)?.timestamp }
                }
            }
            val historyComplete = messages.loadState.append.endOfPaginationReached

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        count = messages.itemCount,
                        key = messages.itemKey { it.id },
                    ) { index ->
                        messages[index]?.let { message ->
                            // peek() so that looking at the neighbour does not load a page. The
                            // oldest loaded message only heads its day once nothing older is
                            // left to load, or the header would jump as pages arrive.
                            val older = if (index + 1 < messages.itemCount) {
                                messages.peek(index + 1)
                            } else {
                                null
                            }
                            val showDay = if (older == null) {
                                historyComplete
                            } else {
                                startsNewDay(message.timestamp, older.timestamp)
                            }

                            Column {
                                if (showDay) {
                                    DayHeader(timestamp = message.timestamp)
                                }
                                ChatMessageItem(
                                    message = message,
                                    onReactionClick = { reaction ->
                                        // Tapping one of ours takes it back.
                                        onReact(
                                            message.id,
                                            reaction.reaction,
                                            reaction.isSticker,
                                            !reaction.mine,
                                        )
                                    },
                                    onPickReaction = { reactingTo = message.id },
                                )
                            }
                        }
                    }

                    // Shown at the top (the list is reversed) while history is being fetched.
                    if (messages.loadState.refresh is LoadState.Loading ||
                        messages.loadState.append is LoadState.Loading
                    ) {
                        item { LoadingBox(modifier = Modifier.fillMaxWidth()) }
                    }
                }

                FloatingDayHeader(
                    timestamp = topTimestamp,
                    visible = listState.isScrollInProgress,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    val messages = flowOf(
        PagingData.from(
            listOf(
                ChatMessage(
                    id = 1,
                    message = "Hey, how are you?",
                    timestamp = System.currentTimeMillis() - 3600000,
                    friendId = 123456789L,
                    fromLocal = false,
                    unread = false,
                    timestampConfirmed = true,
                ),
                ChatMessage(
                    id = 2,
                    message = "I'm doing great, thanks!",
                    timestamp = System.currentTimeMillis() - 3000000,
                    friendId = 123456789L,
                    fromLocal = true,
                    unread = false,
                    timestampConfirmed = true,
                ),
                ChatMessage(
                    id = 3,
                    message = "Want to play some games later?",
                    timestamp = System.currentTimeMillis() - 1800000,
                    friendId = 123456789L,
                    fromLocal = false,
                    unread = true,
                    timestampConfirmed = true,
                ),
            ),
        ),
    ).collectAsLazyPagingItems()

    VapullaTheme {
        ChatScreenContent(
            messages = messages,
            textFieldState = TextFieldState("Hello World!"),
            snackbarHostState = remember { SnackbarHostState() },
            emoticons = persistentListOf(),
            onBack = { },
            onProfile = { },
            onSendClick = { },
            onStickerClick = { },
            onAttachClick = { },
            onReact = { _, _, _, _ -> },
            uiState = ChatUiState(
                friend = SteamFriend(
                    id = 1L,
                    name = "In Game 1",
                    nickname = "One",
                    relation = EFriendRelationship.Friend,
                    state = EPersonaState.Online,
                    gameAppID = 730,
                    gameName = "Counter-Strike 2",
                ),
            ),
        )
    }
}
