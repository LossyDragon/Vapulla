package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Height of the emoticon and sticker panel, about that of a soft keyboard. */
private val EMOJI_PANEL_HEIGHT = 300.dp

/**
 * The message bar at the bottom of a chat: an optional emoticon toggle, the text field with
 * an optional attach-image button inside it, and send. Shared by friend and group chats.
 *
 * With [emoticons] given, the toggle opens a panel of the account's Steam emoticons and
 * stickers in place of the keyboard. An emoticon is typed into the field as `:name:`, the
 * way the Steam client writes it; a sticker is a message of its own, handed to
 * [onStickerClick].
 *
 * @param emoticons the account's emoticons and stickers; null hides the toggle.
 * @param showEmojiPanel whether the panel is open; the caller owns this so it can close it.
 * @param onAttachClick shows the attach-image button when given.
 * @param isUploading an image is on its way; shows a progress line and holds further attaches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatInputBox(
    textFieldState: TextFieldState,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoticons: ImmutableList<Emoticon>? = null,
    showEmojiPanel: Boolean = false,
    onEmojiPanelChange: (Boolean) -> Unit = {},
    onStickerClick: (Emoticon) -> Unit = {},
    onAttachClick: (() -> Unit)? = null,
    isUploading: Boolean = false,
    placeholder: String = "Type a message...",
) {
    val panelOpen = showEmojiPanel && emoticons != null
    val keyboard = LocalSoftwareKeyboardController.current
    var selectedTab by rememberSaveable { mutableStateOf(EmojiStickerSelector.EMOTICONS) }

    // The panel and the keyboard take turns in the same spot: opening the panel puts the
    // keyboard away, and the keyboard coming back (the field was tapped) closes the panel.
    LaunchedEffect(panelOpen) {
        if (panelOpen) keyboard?.hide()
    }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && panelOpen) onEmojiPanelChange(false)
    }
    BackHandler(enabled = panelOpen) { onEmojiPanelChange(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = if (panelOpen) 4.dp else 0.dp,
    ) {
        // The bar's background runs to the screen edge; only its content is lifted clear of
        // the gesture pill, or of the keyboard when that is open (whichever is taller).
        Column(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.navigationBars
                    .union(WindowInsets.ime)
                    .only(WindowInsetsSides.Bottom),
            ),
        ) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (emoticons != null) {
                    FilledIconButton(
                        onClick = { onEmojiPanelChange(!panelOpen) },
                        modifier = Modifier.padding(end = 8.dp),
                        shape = CONTROL_SHAPE,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (panelOpen) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            contentColor = if (panelOpen) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEmotions,
                            contentDescription = "Emoticons and stickers",
                        )
                    }
                }

                TextField(
                    modifier = Modifier.weight(1f),
                    state = textFieldState,
                    placeholder = { Text(placeholder) },
                    trailingIcon = onAttachClick?.let { onAttach ->
                        {
                            IconButton(onClick = onAttach, enabled = !isUploading) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "Send an image",
                                )
                            }
                        }
                    },
                    // Taller than the buttons, so it takes the avatar's own shape.
                    shape = MaterialTheme.shapes.large,
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
                    shape = CONTROL_SHAPE,
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

            if (panelOpen && emoticons != null) {
                EmojiStickerSelectorComponent(
                    emoticons = emoticons,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onEmoticonSelected = { emoticon ->
                        // Inserted at the cursor, replacing whatever is selected, and kept
                        // apart from its neighbours: a space goes in before it when it would
                        // touch the text in front ("hello" + emoticon), and after it when
                        // text follows directly. The cursor ends up after what was inserted.
                        textFieldState.edit {
                            val start = selection.min
                            val end = selection.max
                            val text = asCharSequence()
                            val touchesBefore = text.getOrNull(start - 1)?.isWhitespace() == false
                            val touchesAfter = text.getOrNull(end)?.isWhitespace() == false

                            val inserted = (if (touchesBefore) " " else "") +
                                ":${emoticon.name}:" +
                                (if (touchesAfter) " " else "")
                            replace(start, end, inserted)
                            selection = TextRange(start + inserted.length)
                        }
                    },
                    onStickerSelected = { sticker ->
                        onStickerClick(sticker)
                        onEmojiPanelChange(false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(EMOJI_PANEL_HEIGHT),
                )
            }
        }
    }
}

/**
 * Shape of the bar's two buttons: a rounded square echoing the friends list avatar, like the
 * unread counter. The avatar is 52dp with the large shape; these are 40dp, and the medium
 * shape keeps the same proportion of corner to side.
 */
private val CONTROL_SHAPE: Shape
    @Composable get() = MaterialTheme.shapes.medium

private val previewEmoticons = persistentListOf(
    Emoticon(name = "steamhappy", appID = 0, isSticker = false),
    Emoticon(name = "steamsad", appID = 0, isSticker = false),
    Emoticon(name = "Winter2019JingleIntensifies", appID = 0, isSticker = true),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        ChatInputBox(
            textFieldState = TextFieldState("Hello World!"),
            onSendClick = {},
            emoticons = previewEmoticons,
            onAttachClick = {},
            isUploading = true,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_EmojiPanel() {
    VapullaTheme {
        ChatInputBox(
            textFieldState = TextFieldState(),
            onSendClick = {},
            emoticons = previewEmoticons,
            showEmojiPanel = true,
            onAttachClick = {},
        )
    }
}
