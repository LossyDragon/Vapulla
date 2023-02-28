package `in`.dragonbra.vapulla.compose.screens.chat

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.compose.ui.theme.ChatBubbleFriendShape
import `in`.dragonbra.vapulla.compose.ui.theme.ChatBubbleMeShape
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import java.util.Calendar

enum class EmojiStickerSelector {
    RECENT, EMOJI, STICKER
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

// Heavily derived from JetChat sample
@Composable
fun ChatInputBox(
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    onResetScroll: () -> Unit = {}
) {
    var isEmoticonsOpen by rememberSaveable { mutableStateOf(false) }
    val dismissKeyboard = { isEmoticonsOpen = false }

    // Intercept back navigation if there's a ChatSelector visible
    if (isEmoticonsOpen) {
        BackHandler(onBack = dismissKeyboard)
    }

    var textFieldFocusState by remember { mutableStateOf(false) }
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = modifier
                .imePadding()
                .waterfallPadding()
        ) {
            ChatInputSelector(
                textFieldValue = textState,
                onTextChanged = { textState = it },
                onTextFieldFocused = { focused ->
                    if (focused) {
                        dismissKeyboard()
                        onResetScroll()
                    }
                    textFieldFocusState = focused
                },
                focusState = textFieldFocusState,
                onEmoticonClick = {
                    isEmoticonsOpen = !isEmoticonsOpen
                },
                sendMessageEnabled = textState.text.isNotBlank(),
                onMessageSent = {
                    onMessage(textState.text)
                    textState = TextFieldValue()
                    onResetScroll()
                    dismissKeyboard()
                },
                currentInputSelector = isEmoticonsOpen
            )
            ChatExpanded(
                onTextAdded = { textState = textState.addText(it) },
                currentSelector = isEmoticonsOpen
            )
        }
    }
}

@Composable
private fun ChatInputSelector(
    modifier: Modifier = Modifier,
    currentInputSelector: Boolean,
    focusState: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onMessageSent: () -> Unit,
    onEmoticonClick: () -> Unit,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    sendMessageEnabled: Boolean,
    textFieldValue: TextFieldValue
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var lastFocusState by remember { mutableStateOf(false) }
        BasicTextField(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .fillMaxSize()
                .onFocusChanged { state ->
                    if (lastFocusState != state.isFocused) {
                        onTextFieldFocused(state.isFocused)
                    }
                    lastFocusState = state.isFocused
                },
            cursorBrush = SolidColor(LocalContentColor.current),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Send
            ),
            maxLines = 1,
            onValueChange = { onTextChanged(it) },
            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
            value = textFieldValue,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = friendOffline,
                            shape = RoundedCornerShape(size = 16.dp)
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InputSelectorButton(
                        onClick = onEmoticonClick,
                        selected = currentInputSelector
                    )

                    Spacer(modifier = Modifier.width(width = 8.dp))

                    Box {
                        val disableContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        if (textFieldValue.text.isEmpty() && !focusState) {
                            Text(
                                text = "Enter a message...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = disableContentColor
                                )
                            )
                        }

                        innerTextField()
                    }
                }
            }
        )

        // Send button
        val borderModifier: Modifier? = null
        if (sendMessageEnabled) {
            Modifier.border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                CircleShape
            )
        }

        IconButton(
            modifier = Modifier
                .weight(.1f)
                .height(36.dp)
                .then(borderModifier ?: Modifier),
            enabled = sendMessageEnabled,
            onClick = onMessageSent,
            colors = IconButtonDefaults.iconButtonColors(
                disabledContainerColor = Color.Transparent,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Send")
        }
    }
}

@Composable
private fun InputSelectorButton(
    onClick: () -> Unit,
    selected: Boolean
) {
    val backgroundModifier = if (selected) {
        Modifier.background(
            color = MaterialTheme.colorScheme.secondary,
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        )
    } else {
        Modifier
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .height(72.dp)
            .then(backgroundModifier)
    ) {
        val tint = if (selected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.secondary
        }
        Icon(
            imageVector = Icons.Outlined.Mood,
            contentDescription = "Emoticon Sticker Button",
            tint = tint
        )
    }
}

@Composable
private fun ChatExpanded(
    currentSelector: Boolean,
    onTextAdded: (String) -> Unit
) {
    if (!currentSelector) return

    val focusRequester = FocusRequester()
    SideEffect {
        focusRequester.requestFocus()
    }

    Surface(tonalElevation = 8.dp) {
        EmojiSelector(onTextAdded, focusRequester)
    }
}

@Composable
fun EmojiSelector(
    onTextAdded: (String) -> Unit,
    focusRequester: FocusRequester
) {
    var selected by remember { mutableStateOf(EmojiStickerSelector.RECENT) }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusTarget()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            ExtendedSelectorInnerButton(
                text = "Recent",
                onClick = { selected = EmojiStickerSelector.RECENT },
                selected = selected == EmojiStickerSelector.RECENT,
                modifier = Modifier.weight(1f)
            )
            ExtendedSelectorInnerButton(
                text = "Emoticons",
                onClick = { selected = EmojiStickerSelector.EMOJI },
                selected = selected == EmojiStickerSelector.EMOJI,
                modifier = Modifier.weight(1f)
            )
            ExtendedSelectorInnerButton(
                text = "Stickers",
                onClick = { selected = EmojiStickerSelector.STICKER },
                selected = selected == EmojiStickerSelector.STICKER,
                modifier = Modifier.weight(1f)
            )
        }

        // TODO slide between each view if choosing recent, emoji, or stickers
        Row(modifier = Modifier.verticalScroll(rememberScrollState())) {
            EmojiTable(onTextAdded)
        }
    }
}

@Composable
fun EmojiTable(
    onTextAdded: (String) -> Unit
) {
    // TODO display emojis
    Text("Hello Gordon!")
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
fun ChatMessageItem(
    isChatMessageUs: Boolean,
    message: AnnotatedString,
    messageTime: Long
) {
    var bubbleColor = MaterialTheme.colorScheme.primary
    var bubbleShape = ChatBubbleFriendShape
    var bubbleSide: Alignment = Alignment.CenterEnd
    var bubbleTimeSide: Alignment.Horizontal = Alignment.End

    if (isChatMessageUs) {
        bubbleColor = MaterialTheme.colorScheme.surfaceVariant
        bubbleShape = ChatBubbleMeShape
        bubbleSide = Alignment.CenterStart
        bubbleTimeSide = Alignment.Start
    }

    val timestamp by remember {
        // TODO, this should be done in the VM
        derivedStateOf {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = messageTime
            val formattedTime = DateFormat.format("h:mm a", calendar).toString()
            mutableStateOf(formattedTime)
        }
    }

    val configuration = LocalConfiguration.current
    val maxWidth = configuration.screenWidthDp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentAlignment = bubbleSide
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = maxWidth.times(.85).dp)
                .width(IntrinsicSize.Max),
            color = bubbleColor,
            shape = bubbleShape
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = bubbleTimeSide
            ) {
                Text(
                    modifier = Modifier.widthIn(64.dp),
                    color = Color.White,
                    text = message
                )

                Text(
                    text = timestamp.value,
                    fontSize = 8.sp,
                    color = friendOffline
                )
            }
        }
    }
}

@Composable
private fun ChatMessageDateHeader(
    isVisible: Boolean,
    dateStamp: String
) {
    if (!isVisible) {
        return
    }

    Row {
        val divider = @Composable {
            Divider(
                modifier = Modifier
                    .weight(.4f)
                    .padding(horizontal = 8.dp)
                    .align(Alignment.CenterVertically),
                color = friendOffline.copy(alpha = 0.60f)
            )
        }

        divider()
        Text(
            modifier = Modifier
                .weight(.6f)
                .padding(vertical = 4.dp, horizontal = 0.dp),
            color = friendOffline.copy(alpha = 0.95f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            text = dateStamp,
            textAlign = TextAlign.Center
        )
        divider()
    }
}

@Preview
@Composable
fun Preview_ChatInputBox() {
    VapullaTheme {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            ChatInputBox(onMessage = {})
        }
    }
}

@Preview
@Composable
private fun Preview_ChatMessageItem() {
    val randomMsg = """
        Just a car? Just a car!? That's like saying the Mona Lisa is just a sculpture or shit, 
        man; that's like saying Jimmy Gibbs is just a driver; that's like saying the girl 
        on the bridge is just a little purty―she is an AN-GEL.
    """.trimIndent()
    VapullaTheme {
        Surface {
            Column(Modifier.fillMaxWidth()) {
                ChatMessageItem(
                    true,
                    buildAnnotatedString { append("Hello?") },
                    1677606163515
                )
                Spacer(Modifier.height(8.dp))
                ChatMessageItem(
                    false,
                    buildAnnotatedString { append(randomMsg) },
                    1677610722814
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_ChatMessageDateHeader() {
    VapullaTheme {
        Surface {
            ChatMessageDateHeader(isVisible = true, dateStamp = "Wednesday - January 5, 2023")
        }
    }
}
