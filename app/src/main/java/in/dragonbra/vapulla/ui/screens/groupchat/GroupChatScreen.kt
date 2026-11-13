package `in`.dragonbra.vapulla.ui.screens.groupchat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.service.api.ChatRoomInfo
import `in`.dragonbra.vapulla.service.api.GroupChatMessage
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.screens.chat.components.ChatInputBox
import `in`.dragonbra.vapulla.ui.screens.chat.components.DayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.FloatingDayHeader
import `in`.dragonbra.vapulla.ui.screens.chat.components.startsNewDay
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** How many messages up from the bottom the list still follows incoming messages down. */
private const val FOLLOW_THRESHOLD = 5

/** How close to the oldest loaded message the list gets before the next page is fetched. */
private const val LOAD_OLDER_THRESHOLD = 8

@Composable
fun GroupChatScreen(
    viewModel: GroupChatViewModel,
    onBack: () -> Unit,
    onSwitchChannel: (ChatRoomInfo) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emoticons by viewModel.emoticons.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val textFieldState = rememberTextFieldState()

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

    GroupChatContent(
        uiState = uiState,
        textFieldState = textFieldState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSendClick = {
            viewModel.sendMessage(textFieldState.text.toString())
            textFieldState.clearText()
        },
        onAttachClick = {
            imagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        },
        onLoadOlder = viewModel::loadOlder,
        onSwitchChannel = onSwitchChannel,
        emoticons = emoticons,
        onStickerClick = viewModel::sendSticker,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GroupChatContent(
    uiState: GroupChatUiState,
    textFieldState: TextFieldState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    onLoadOlder: () -> Unit,
    onSwitchChannel: (ChatRoomInfo) -> Unit = {},
    emoticons: ImmutableList<Emoticon> = persistentListOf(),
    onStickerClick: (Emoticon) -> Unit = {},
) {
    val listState = rememberLazyListState()
    var showEmojiPanel by rememberSaveable { mutableStateOf(false) }

    // Follow the conversation down when a new message lands, unless the reader has scrolled
    // up; our own message always scrolls to itself.
    val newest = uiState.messages.firstOrNull()
    LaunchedEffect(newest?.key) {
        if (newest == null) return@LaunchedEffect
        if (newest.fromLocal || listState.firstVisibleItemIndex <= FOLLOW_THRESHOLD) {
            listState.animateScrollToItem(0)
        }
    }

    // The list is reversed, so its last items are the oldest messages.
    val nearOldest by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_OLDER_THRESHOLD
        }
    }
    LaunchedEffect(nearOldest, uiState.messages.size, uiState.hasOlder) {
        if (nearOldest && uiState.hasOlder) onLoadOlder()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ChannelTitle(
                        uiState = uiState,
                        onSwitchChannel = onSwitchChannel,
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatInputBox(
                textFieldState = textFieldState,
                onSendClick = onSendClick,
                onAttachClick = onAttachClick,
                isUploading = uiState.isUploading,
                emoticons = emoticons,
                showEmojiPanel = showEmojiPanel,
                onEmojiPanelChange = { showEmojiPanel = it },
                onStickerClick = onStickerClick,
                placeholder = "Message # ${uiState.roomName.channelName()}",
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.error != null -> SearchResultMessage(
                message = uiState.error,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            uiState.messages.isEmpty() -> SearchResultMessage(
                message = "No messages in this channel yet",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                // The day of the message at the top of the screen, for the floating header.
                // The list is reversed, so that is the last visible item.
                val currentMessages by rememberUpdatedState(uiState.messages)
                val topTimestamp by remember {
                    derivedStateOf {
                        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            ?.let { currentMessages.getOrNull(it)?.timestamp }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items = uiState.messages, key = { _, it ->
                        it.key
                    }) { index, message ->
                        // The oldest loaded message only heads its day once nothing older is left
                        // to load, or the header would jump as pages arrive.
                        val older = uiState.messages.getOrNull(index + 1)
                        val showDay = if (older == null) {
                            !uiState.hasOlder
                        } else {
                            startsNewDay(message.timestamp, older.timestamp)
                        }

                        Column {
                            if (showDay) {
                                DayHeader(timestamp = message.timestamp)
                            }
                            if (message.isServerMessage) {
                                ServerMessageRow(message)
                            } else {
                                GroupMessageRow(message)
                            }
                        }
                    }

                    if (uiState.isLoadingOlder) {
                        item(key = "loading-older") {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                LoadingIndicator()
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
        }
    }
}

/**
 * The channel and group names. In a group with several channels the title is a button that
 * opens the list of them, so another channel can be opened without going back first.
 */
@Composable
private fun ChannelTitle(uiState: GroupChatUiState, onSwitchChannel: (ChatRoomInfo) -> Unit) {
    val canSwitch = uiState.rooms.size > 1
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = canSwitch, onClickLabel = "Switch channel") {
                    menuOpen = true
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "# ${uiState.roomName.channelName()}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uiState.groupName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (canSwitch) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            uiState.rooms.forEach { room ->
                val isCurrent = room.chatId == uiState.chatId

                DropdownMenuItem(
                    text = { Text(text = "# ${room.name.channelName()}") },
                    trailingIcon = if (isCurrent) {
                        { Icon(Icons.Default.Check, contentDescription = "Current channel") }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        if (!isCurrent) onSwitchChannel(room)
                    },
                )
            }
        }
    }
}

/** A group's first channel has no name of its own; Steam calls it Home. */
private fun String.channelName(): String = ifEmpty { "Home" }

/** A member's message: avatar and name for others, a plain right-aligned bubble for ours. */
@Composable
private fun GroupMessageRow(message: GroupChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromLocal) {
            Arrangement.End
        } else {
            Arrangement.spacedBy(8.dp, Alignment.Start)
        },
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!message.fromLocal) {
            CoilImage(
                imageModel = { message.senderAvatarUrl },
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.medium),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                failure = { Icon(Icons.Default.Person, contentDescription = null) },
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (message.fromLocal) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!message.fromLocal) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BBCodeText(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.fromLocal) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                Text(
                    text = message.timestamp.toTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.fromLocal) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

/** Someone joined or left, and the like: a quiet centred line instead of a bubble. */
@Composable
private fun ServerMessageRow(message: GroupChatMessage) {
    Text(
        text = "${message.message} · ${message.timestamp.toTimeString()}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun Preview() {
    fun message(ordinal: Int, text: String, local: Boolean = false, server: Boolean = false) =
        GroupChatMessage(
            groupId = 1,
            chatId = 2,
            senderId = 3,
            senderName = "Gamer",
            senderAvatarUrl = "",
            message = text,
            timestamp = 1_760_722_982_000 - ordinal * 60_000L,
            ordinal = ordinal,
            fromLocal = local,
            isServerMessage = server,
        )

    VapullaTheme {
        GroupChatContent(
            uiState = GroupChatUiState(
                groupName = "JavaSteam",
                roomName = "",
                isLoading = false,
                messages = persistentListOf(
                    message(0, "Sounds good to me", local = true),
                    message(1, "Anyone up for [b]MvM[/b] tonight?"),
                    message(2, "Gamer joined", server = true),
                ),
            ),
            textFieldState = TextFieldState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSendClick = {},
            onAttachClick = {},
            onLoadOlder = {},
        )
    }
}
