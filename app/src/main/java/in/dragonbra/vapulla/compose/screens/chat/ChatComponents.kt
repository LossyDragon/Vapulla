package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.util.StickerImage
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.data.entity.Emoticon

enum class EmojiStickerSelector {
    EMOJI, STICKER
}

enum class InputSelector {
    NONE, EMOJI,
}

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    onMessageSent: (String) -> Unit,
    onResetScroll: () -> Unit = {},
    emoticonList: List<Emoticon>
) {
    var currentInputSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    val dismissKeyboard = { currentInputSelector = InputSelector.NONE }

    // Intercept back navigation if there's a InputSelector visible
    if (currentInputSelector != InputSelector.NONE) {
        BackHandler(onBack = dismissKeyboard)
    }

    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = modifier.background(
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
            )
        ) {
            UserInputText(
                onSelectorChange = { currentInputSelector = it },
                currentInputSelector = currentInputSelector,
                sendMessageEnabled = textState.text.isNotBlank(),
                onMessageSent = {
                    onMessageSent(textState.text)
                    textState = TextFieldValue()
                    onResetScroll()
                    dismissKeyboard()
                },
                textFieldValue = textState,
                onTextChanged = { textState = it },
                keyboardShown = currentInputSelector == InputSelector.NONE && textFieldFocusState,
                onTextFieldFocused = { focused ->
                    if (focused) {
                        currentInputSelector = InputSelector.NONE
                        onResetScroll()
                    }
                    textFieldFocusState = focused
                },
                focusState = textFieldFocusState
            )
            SelectorExpanded(
                emoticonList = emoticonList,
                onTextAdded = { emote, isSticker ->
                    if (isSticker) {
                        onMessageSent("/sticker $emote")
                    } else {
                        textState = textState.addText(":$emote:")
                    }
                },
                currentSelector = currentInputSelector
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
    currentSelector: InputSelector,
    onTextAdded: (String, Boolean) -> Unit,
    emoticonList: List<Emoticon>
) {
    if (currentSelector == InputSelector.NONE) return

    var selected by remember { mutableStateOf(EmojiStickerSelector.EMOJI) }
    val focusRequester = FocusRequester()
    SideEffect {
        if (currentSelector == InputSelector.EMOJI) {
            focusRequester.requestFocus()
        }
    }

    Surface(tonalElevation = 8.dp) {
        when (currentSelector) {
            InputSelector.EMOJI -> EmojiSelector(
                selected = selected,
                onSelected = { selected = it },
                emoticonList = emoticonList,
                onTextAdded = onTextAdded,
                focusRequester = focusRequester
            )
            else -> throw NotImplementedError()
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun InputSelectorButton(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    selected: Boolean
) {
    val backgroundModifier = if (selected) {
        Modifier.background(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(8.dp)
        )
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier.then(backgroundModifier)
    ) {
        val tint = if (selected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.secondary
        }
        Icon(
            imageVector = icon,
            tint = tint,
            modifier = Modifier,
            contentDescription = description
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun UserInputText(
    onSelectorChange: (InputSelector) -> Unit,
    currentInputSelector: InputSelector,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    keyboardShown: Boolean,
    onTextFieldFocused: (Boolean) -> Unit,
    focusState: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(64.dp)
            .semantics { keyboardShownProperty = keyboardShown },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .weight(1f)
                .align(Alignment.Bottom)
        ) {
            var lastFocusState by remember { mutableStateOf(false) }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { onTextChanged(it) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .onFocusChanged { state ->
                        if (lastFocusState != state.isFocused) {
                            onTextFieldFocused(state.isFocused)
                        }
                        lastFocusState = state.isFocused
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Send
                ),
                maxLines = 3,
                cursorBrush = SolidColor(LocalContentColor.current),
                textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = friendOffline,
                                shape = RoundedCornerShape(size = 8.dp)
                            )
                            .padding(all = 0.dp), // inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InputSelectorButton(
                            onClick = {
                                when (currentInputSelector) {
                                    InputSelector.NONE -> onSelectorChange(InputSelector.EMOJI)
                                    InputSelector.EMOJI -> onSelectorChange(InputSelector.NONE)
                                }
                            },
                            icon = Icons.Outlined.Mood,
                            selected = currentInputSelector == InputSelector.EMOJI,
                            description = "Emoticon and Sticker selector"
                        )

                        Spacer(modifier = Modifier.width(width = 8.dp))
                        innerTextField()
                    }
                }
            )

            val disableContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            if (textFieldValue.text.isEmpty() && !focusState) {
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 56.dp),
                    text = "Type a message...",
                    style = MaterialTheme.typography.bodyLarge.copy(color = disableContentColor)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Send Button
        SendButton(
            modifier = Modifier,
            onMessageSent = onMessageSent,
            sendMessageEnabled = sendMessageEnabled
        )
    }
}

@Composable
private fun SendButton(
    modifier: Modifier = Modifier,
    onMessageSent: () -> Unit,
    sendMessageEnabled: Boolean
) {
    val border = if (!sendMessageEnabled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    } else {
        null
    }

    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val buttonColors = ButtonDefaults.buttonColors(
        disabledContainerColor = Color.Transparent,
        disabledContentColor = disabledContentColor
    )
    Button(
        modifier = modifier.height(36.dp),
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

@Composable
fun EmojiSelector(
    selected: EmojiStickerSelector,
    onSelected: (EmojiStickerSelector) -> Unit,
    emoticonList: List<Emoticon>,
    onTextAdded: (String, Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .focusTarget()
    ) {
        // Emoticon / Sticker selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            ExtendedSelectorInnerButton(
                text = "Emoticons",
                onClick = { onSelected(EmojiStickerSelector.EMOJI) },
                selected = selected == EmojiStickerSelector.EMOJI,
                modifier = Modifier.weight(1f)
            )
            ExtendedSelectorInnerButton(
                text = "Stickers",
                onClick = { onSelected(EmojiStickerSelector.STICKER) },
                selected = selected == EmojiStickerSelector.STICKER,
                modifier = Modifier.weight(1f)
            )
        }

        // Sticker Panel
        Row(modifier = Modifier.verticalScroll(rememberScrollState())) {
            EmojiTable(
                emoticonList = emoticonList,
                isSticker = selected == EmojiStickerSelector.STICKER,
                onTextAdded = { onTextAdded(it, selected == EmojiStickerSelector.STICKER) }
            )
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
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
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
    emoticonList: List<Emoticon>,
    isSticker: Boolean,
    onTextAdded: (String) -> Unit
) {
    var list by remember { mutableStateOf(listOf<Emoticon>()) }
    val columns: GridCells.Adaptive
    val size: Dp
    val baseUrl: String

    if (isSticker) {
        // Stickers
        baseUrl = Constants.STICKER_URL
        columns = GridCells.Adaptive(minSize = 75.dp)
        list = emoticonList.filter { it.isSticker }
        size = 75.dp
    } else {
        // Emoticons
        baseUrl = Constants.EMOTE_URL
        columns = GridCells.Adaptive(minSize = 54.dp)
        list = emoticonList.filter { !it.isSticker }
        size = 54.dp
    }

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth()
            .height(216.dp),
        columns = columns,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        content = {
            items(list) {
                StickerImage(
                    modifier = Modifier
                        .clickable { onTextAdded(it.name) }
                        .size(size)
                        .padding(4.dp),
                    url = baseUrl + it.name
                )
            }
        }
    )
}

@Preview
@Composable
fun Preview_ChatInputBox() {
    VapullaTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            UserInput(onMessageSent = {}, onResetScroll = {}, emoticonList = listOf())
        }
    }
}

@Preview
@Composable
fun Preview_EmojiTable() {
    VapullaTheme {
        EmojiSelector(
            selected = EmojiStickerSelector.EMOJI,
            onSelected = {},
            emoticonList = (1..64).map { Emoticon("emote", false, 0) }.toList(),
            onTextAdded = { _, _ -> },
            focusRequester = FocusRequester()
        )
    }
}

@Preview
@Composable
fun Preview_EmojiStickerTable() {
    VapullaTheme {
        EmojiSelector(
            selected = EmojiStickerSelector.STICKER,
            onSelected = {},
            emoticonList = (1..64).map { Emoticon("emote", true, 0) }.toList(),
            onTextAdded = { _, _ -> },
            focusRequester = FocusRequester()
        )
    }
}
