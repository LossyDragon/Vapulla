package `in`.dragonbra.vapulla.ui.screens.chat

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.FriendMessage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatMessageItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.getAvatarURL
import `in`.dragonbra.vapulla.util.Utils.toAvatarURL
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()

    val textFieldState = rememberTextFieldState(initialText = uiState.messageText)
    LaunchedEffect(textFieldState.text.toString()) {
        viewModel.updateMessageText(textFieldState.text.toString())
    }

    ChatScreenContent(
        uiState = uiState,
        messages = messages,
        textFieldState = textFieldState,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreenContent(
    uiState: ChatViewModel.Companion.ChatUiState,
    messages: LazyPagingItems<ChatMessage>,
    textFieldState: TextFieldState,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
                                .clip(CircleShape)
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
                            // TODO CompositionLocal of status color
                            Text(
                                text = uiState.friend?.nameOrNickname.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Online",
                                style = MaterialTheme.typography.titleSmall,
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
        bottomBar = {
            ChatInputBox(
                textFieldState = textFieldState,
                showEmojiKeyboard = uiState.showEmojiKeyboard,
                onSendClick = { },
                onEmojiClick = { },
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                        ChatMessageItem(
                            message = message,
                            isFromLocal = message.fromLocal,
                        )
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
        },
    )
}

@Composable
private fun ChatInputBox(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState,
    onSendClick: () -> Unit,
    onEmojiClick: () -> Unit,
    showEmojiKeyboard: Boolean = false,
    placeholder: String = "Type a message...",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = if (showEmojiKeyboard) 4.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onEmojiClick,
                modifier = Modifier.padding(end = 8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (showEmojiKeyboard) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (showEmojiKeyboard) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji selector",
                )
            }

            TextField(
                modifier = Modifier.weight(1f),
                state = textFieldState,
                placeholder = { Text(placeholder) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4),
            )

            FilledIconButton(
                onClick = onSendClick,
                enabled = textFieldState.text.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                )
            }
        }
    }
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
            uiState = ChatViewModel.Companion.ChatUiState(
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
