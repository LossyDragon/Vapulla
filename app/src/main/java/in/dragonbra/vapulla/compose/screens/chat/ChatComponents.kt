package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.data.entity.Emoticon

/**
 * Heavily referenced from Jetpack Compose sample: JetChat
 */

enum class EmojiStickerSelector(val value: Int) {
    NONE(-1),
    EMOJI(0),
    STICKER(1)
}

@Preview
@Composable
fun UserInputPreview() {
    VapullaTheme {
        UserInput(
            emoticonData = listOf(),
            onMessageSent = {},
            onTextChanged = {}
        )
    }
}

@Preview
@Composable
fun EmojiPreview() {
    VapullaTheme {
        SelectorExpanded(
            emoticonData = List(50) { idx ->
                Emoticon("A$idx", (idx % 2) == 0, idx.times(100))
            },
            currentSelector = EmojiStickerSelector.EMOJI,
            onSelectorChange = {},
            onTextAdded = {},
            onStickerAdded = {},
        )
    }
}

@Preview
@Composable
fun StickerPreview() {
    VapullaTheme {
        SelectorExpanded(
            emoticonData = List(50) { idx ->
                Emoticon("A$idx", (idx % 2) == 0, idx.times(100))
            },
            currentSelector = EmojiStickerSelector.STICKER,
            onSelectorChange = {},
            onTextAdded = {},
            onStickerAdded = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    emoticonData: List<Emoticon>,
    onMessageSent: (String) -> Unit,
    onTextChanged: () -> Unit,
    resetScroll: () -> Unit = {},
) {
    var currentInputSelector by rememberSaveable { mutableStateOf(EmojiStickerSelector.NONE) }
    val dismissKeyboard = { currentInputSelector = EmojiStickerSelector.NONE }

    // Intercept back navigation if there's a InputSelector visible
    if (currentInputSelector != EmojiStickerSelector.NONE) {
        BackHandler(onBack = dismissKeyboard)
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp, contentColor = MaterialTheme.colorScheme.secondary) {
        Column(modifier = modifier) {
            UserInputText(
                textFieldValue = textState,
                onTextChanged = {
                    textState = it
                    onTextChanged()
                },
                // Only show the keyboard if there's no input selector and text field has focus
                keyboardShown = currentInputSelector == EmojiStickerSelector.NONE && textFieldFocusState,
                // Close extended selector if text field receives focus
                onTextFieldFocused = { focused ->
                    if (focused) {
                        currentInputSelector = EmojiStickerSelector.NONE
                        resetScroll()
                    }
                    textFieldFocusState = focused
                },
                onMessageSent = {
                    onMessageSent(textState.text)
                    // Reset text field and close keyboard
                    textState = TextFieldValue()
                    // Move scroll to bottom
                    resetScroll()
                },
                onSelectorChange = { currentInputSelector = it },
//                onMessageStickerSent = {
//                    onMessageSent(textState.text)
//                    // Reset text field and close keyboard
//                    textState = TextFieldValue()
//                    // Move scroll to bottom
//                    resetScroll()
//                    dismissKeyboard()
//                },
                currentInputSelector = currentInputSelector
            )
            SelectorExpanded(
                emoticonData = emoticonData,
                currentSelector = currentInputSelector,
                onSelectorChange = {
                    if (it == EmojiStickerSelector.NONE) {
                        dismissKeyboard()
                    } else {
                        currentInputSelector = it
                    }
                },
                onTextAdded = { textState = textState.addText(it) },
                onStickerAdded = onMessageSent,
            )
        }
    }
}

private fun TextFieldValue.addText(newString: String): TextFieldValue {
    val newText = this.text.replaceRange(
        this.selection.start,
        this.selection.end,
        newString
    )
    val newSelection = TextRange(
        start = newText.length,
        end = newText.length
    )

    return this.copy(text = newText, selection = newSelection)
}

@Composable
private fun SelectorExpanded(
    emoticonData: List<Emoticon>,
    currentSelector: EmojiStickerSelector,
    onSelectorChange: (EmojiStickerSelector) -> Unit,
    onTextAdded: (String) -> Unit,
    onStickerAdded: (String) -> Unit
) {
    if (currentSelector == EmojiStickerSelector.NONE) return

    // Request focus to force the TextField to lose it
    val focusRequester = FocusRequester()
    // If the selector is shown, always request focus to trigger a TextField.onFocusChange.
    SideEffect {
        if (currentSelector == EmojiStickerSelector.EMOJI) {
            focusRequester.requestFocus()
        }
    }

    Surface(tonalElevation = 8.dp) {
        when (currentSelector) {
            EmojiStickerSelector.EMOJI,
            EmojiStickerSelector.STICKER -> EmojiSelector(
                selected = currentSelector,
                emoticonData = emoticonData,
                onSelectorChange = onSelectorChange,
                onEmojiAdded = onTextAdded,
                onStickerAdded = onStickerAdded,
                focusRequester = focusRequester
            )

            else -> {
                throw NotImplementedError()
            }
        }
    }
}

@Composable
private fun UserInputSelector(
    onSelectorChange: (EmojiStickerSelector) -> Unit,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    currentInputSelector: EmojiStickerSelector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(72.dp)
            .wrapContentHeight()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InputSelectorButton(
            onClick = { onSelectorChange(EmojiStickerSelector.EMOJI) },
            icon = Icons.Outlined.Mood,
            selected = currentInputSelector == EmojiStickerSelector.EMOJI,
            description = "Show Emoji selector"
        )

        val border = if (!sendMessageEnabled) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        } else {
            null
        }
        Spacer(modifier = Modifier.weight(1f))

        val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        val buttonColors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor
        )

        // Send button
        Button(
            modifier = Modifier.height(36.dp),
            enabled = sendMessageEnabled,
            onClick = onMessageSent,
            colors = buttonColors,
            border = border,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Send",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun InputSelectorButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundModifier = if (selected) {
        Modifier.background(
            color = LocalContentColor.current,
            shape = RoundedCornerShape(14.dp)
        )
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = modifier.then(backgroundModifier)
    ) {
        val tint = if (selected) {
            contentColorFor(backgroundColor = LocalContentColor.current)
        } else {
            LocalContentColor.current
        }
        Icon(
            icon,
            tint = tint,
            modifier = Modifier
                .padding(8.dp)
                .size(56.dp),
            contentDescription = description
        )
    }
}

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@ExperimentalFoundationApi
@Composable
private fun UserInputText(
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    onMessageSent: (String) -> Unit,
    onSelectorChange: (EmojiStickerSelector) -> Unit,
    currentInputSelector: EmojiStickerSelector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(Modifier.fillMaxSize()) {
            UserInputTextField(
                modifier = Modifier.semantics {
                    contentDescription = "Text input"
                    keyboardShownProperty = keyboardShown
                },
                textFieldValue = textFieldValue,
                onTextChanged = onTextChanged,
                onTextFieldFocused = onTextFieldFocused,
                keyboardType = keyboardType,
                onMessageSent = onMessageSent,
                onSelectorChange = onSelectorChange,
                currentInputSelector = currentInputSelector,
            )
        }
    }
}

@Composable
private fun BoxScope.UserInputTextField(
    modifier: Modifier = Modifier,
    textFieldValue: TextFieldValue,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    onMessageSent: (String) -> Unit,
    onSelectorChange: (EmojiStickerSelector) -> Unit,
    currentInputSelector: EmojiStickerSelector,
) {
    var lastFocusState by remember { mutableStateOf(false) }
    val backgroundModifier = if (currentInputSelector == EmojiStickerSelector.EMOJI) {
        Modifier
            .background(
                color = LocalContentColor.current,
                shape = RoundedCornerShape(8.dp),
            )
            .size(40.dp)
    } else {
        Modifier
    }
    val tint = if (currentInputSelector == EmojiStickerSelector.EMOJI) {
        contentColorFor(backgroundColor = LocalContentColor.current)
    } else {
        LocalContentColor.current
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = onTextChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .align(Alignment.CenterStart)
            .onFocusChanged { state ->
                if (lastFocusState != state.isFocused) {
                    onTextFieldFocused(state.isFocused)
                }
                lastFocusState = state.isFocused
            },
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions {
            if (textFieldValue.text.isNotBlank()) onMessageSent(textFieldValue.text)
        },
        maxLines = 3,
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
        leadingIcon = {
            IconButton(
                modifier = backgroundModifier,
                onClick = {
                    if (currentInputSelector != EmojiStickerSelector.EMOJI) {
                        onSelectorChange(EmojiStickerSelector.EMOJI)
                    } else {
                        onSelectorChange(EmojiStickerSelector.NONE)
                    }
                },
                content = {
                    Icon(Icons.Default.Mood, null, tint = tint)
                }
            )
        },
        trailingIcon = {
            val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            val buttonColors = IconButtonDefaults.iconButtonColors(
                disabledContainerColor = Color.Transparent,
                disabledContentColor = disabledContentColor
            )
            IconButton(
                enabled = textFieldValue.text.isNotBlank(),
                colors = buttonColors,
                onClick = { onMessageSent(textFieldValue.text) },
                content = { Icon(Icons.AutoMirrored.Filled.Send, null) }
            )
        },
        placeholder = {
            val disableContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = "Message #composers",
                style = MaterialTheme.typography.bodyLarge.copy(color = disableContentColor)
            )
        }
    )
}

@Composable
fun EmojiSelector(
    selected: EmojiStickerSelector,
    emoticonData: List<Emoticon>,
    onSelectorChange: (EmojiStickerSelector) -> Unit,
    onEmojiAdded: (String) -> Unit,
    onStickerAdded: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val pagerState = rememberPagerState(pageCount = { EmojiStickerSelector.entries.size })
    val emojiScrollState = rememberScrollState()

    LaunchedEffect(selected) {
        emojiScrollState.scrollTo(0)
        pagerState.animateScrollToPage(selected.value)
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester) // Requests focus when the Emoji selector is displayed
            // Make the emoji selector focusable so it can steal focus from TextField
            .focusTarget()
            .semantics { contentDescription = "Emoji selector" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            ExtendedSelectorInnerButton(
                text = "Emojis",
                onClick = { onSelectorChange(EmojiStickerSelector.EMOJI) },
                selected = selected == EmojiStickerSelector.EMOJI,
                modifier = Modifier.weight(1f)
            )
            ExtendedSelectorInnerButton(
                text = "Stickers",
                onClick = { onSelectorChange(EmojiStickerSelector.STICKER) },
                selected = selected == EmojiStickerSelector.STICKER,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                0 -> EmojiTable(
                    modifier = Modifier
                        .height(256.dp)
                        .padding(8.dp),
                    list = emoticonData.filter { !it.isSticker },
                    onTextAdded = onEmojiAdded
                )

                1 -> StickerTable(
                    modifier = Modifier
                        .height(256.dp)
                        .padding(8.dp),
                    list = emoticonData.filter { it.isSticker },
                    onTextAdded = onStickerAdded
                )
            }
        }
    }
}

@Composable
fun ExtendedSelectorInnerButton(
    text: String,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        disabledContainerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
    )
    TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .height(36.dp),
        colors = colors,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun EmojiTable(
    modifier: Modifier = Modifier,
    list: List<Emoticon>,
    onTextAdded: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        modifier = modifier.then(Modifier.fillMaxHeight()),
        columns = GridCells.Adaptive(54.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        items(list, key = { it.name }) { emote ->
            CoilImage(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onTextAdded(":${emote.name}: ") },
                imageRequest = {
                    ImageRequest.Builder(context)
                        .data(Constants.EMOTE_URL + emote.name)
                        .crossfade(true)
                        .build()
                },
                previewPlaceholder = painterResource(id = R.mipmap.ic_launcher_foreground),
                imageOptions = ImageOptions(contentScale = ContentScale.Fit)
            )
        }
    }
}

@Composable
private fun StickerTable(
    modifier: Modifier = Modifier,
    list: List<Emoticon>,
    onTextAdded: (String) -> Unit,
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        modifier = modifier.then(Modifier.fillMaxHeight()),
        columns = GridCells.Adaptive(75.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        items(list, key = { it.name }) { emote ->
            CoilImage(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onTextAdded("/sticker ${emote.name}") },
                imageRequest = {
                    ImageRequest.Builder(context)
                        .data(Constants.STICKER_URL + emote.name)
                        .crossfade(true)
                        .build()
                },
                previewPlaceholder = painterResource(id = R.mipmap.ic_launcher_foreground),
                imageOptions = ImageOptions(contentScale = ContentScale.Fit)
            )
        }
    }
}
