package `in`.dragonbra.vapulla.ui.screens.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatInputBox
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatMessageItem
import `in`.dragonbra.vapulla.ui.screens.chat.components.DayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.FloatingDayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.startsNewDay
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toAvatarURL
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** How many messages up from the bottom the list still follows incoming messages down. */
private const val FOLLOW_THRESHOLD = 5

/** How long to wait for a sent message to appear before giving up on scrolling to it. */
private val SENT_SCROLL_TIMEOUT_MS = 3.seconds

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val emoticons by viewModel.emoticons.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    val textFieldState = rememberTextFieldState(initialText = uiState.messageText)
    LaunchedEffect(textFieldState.text.toString()) {
        viewModel.updateMessageText(textFieldState.text.toString())
    }

    // The chat counts as open only while it is actually on screen, so messages that arrive
    // with the app in the background still notify and stay unread.
    LifecycleResumeEffect(viewModel) {
        viewModel.onChatVisible(true)
        onPauseOrDispose { viewModel.onChatVisible(false) }
    }

    LaunchedEffect(viewModel) {
        viewModel.sendFailures.collect { text ->
            // Hand the text back unless something new has been typed in the meantime.
            if (textFieldState.text.isEmpty()) {
                textFieldState.setTextAndPlaceCursorAtEnd(text)
            }
            snackbarHostState.showSnackbar("Message not sent")
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.notices.collect { snackbarHostState.showSnackbar(it) }
    }

    // The system photo picker needs no storage permission; it hands back one image.
    val imagePicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.sendImage(uri)
        }
    }

    ChatScreenContent(
        uiState = uiState,
        messages = messages,
        textFieldState = textFieldState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSendClick = {
            viewModel.sendMessage(textFieldState.text.toString())
            textFieldState.clearText()
        },
        emoticons = emoticons,
        onEmojiPanelChange = { viewModel.toggleEmojiKeyboard(it) },
        onStickerClick = viewModel::sendSticker,
        onAttachClick = {
            imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
private fun ChatScreenContent(
    uiState: ChatUiState,
    messages: LazyPagingItems<ChatMessage>,
    textFieldState: TextFieldState,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onSendClick: () -> Unit = { },
    emoticons: ImmutableList<Emoticon> = persistentListOf(),
    onEmojiPanelChange: (Boolean) -> Unit = { },
    onStickerClick: (Emoticon) -> Unit = { },
    onAttachClick: () -> Unit = { },
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
            delay(SENT_SCROLL_TIMEOUT_MS)
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
                        // TODO add Profile Equiped items around PFP.
                        CoilImage(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                // The same rounded square as in the friends list.
                                .clip(MaterialTheme.shapes.large)
                                .background(Color.DarkGray)
                                .size(48.dp),
                            imageModel = { uiState.friend?.avatar?.toAvatarURL() },
                            imageOptions = ImageOptions(
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                            ),
                            loading = { CircularProgressIndicator() },
                            failure = { Icon(Icons.Filled.QuestionMark, null) },
                            previewPlaceholder = painterResource(R.drawable.vapulla_background),
                        )
                        Column {
                            Text(
                                text = uiState.friend?.nameOrNickname.orEmpty(),
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
                showEmojiPanel = uiState.showEmojiKeyboard,
                onEmojiPanelChange = onEmojiPanelChange,
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
                                    isFromLocal = message.fromLocal,
                                )
                            }
                        }
                    }

                    when {
                        messages.loadState.refresh is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }

                        messages.loadState.append is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
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
            onBack = { },
            messages = messages,
            textFieldState = TextFieldState("Hello World!"),
            uiState = ChatUiState(
                messageText = "Hello World!",
                showEmojiKeyboard = true,
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
