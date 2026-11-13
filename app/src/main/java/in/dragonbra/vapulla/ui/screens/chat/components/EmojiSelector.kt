package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.helpers.capitalize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

enum class EmojiStickerSelector {
    EMOTICONS,
    STICKERS,
}

/** Side of an emoticon button; emoticons are small pixel art. */
private val EMOTICON_CELL = 56.dp

/** Side of a sticker button; stickers are detailed and need the room. */
private val STICKER_CELL = 104.dp

/** Space between a button's edge and its image. */
private val IMAGE_INSET = 6.dp

/**
 * The account's Steam emoticons and stickers, as delivered by `EmoticonListCallback`, on two
 * tabs. Shown in place of the keyboard by [ChatInputBox].
 */
@Composable
fun EmojiStickerSelectorComponent(
    emoticons: ImmutableList<Emoticon>,
    selectedTab: EmojiStickerSelector,
    onTabSelected: (EmojiStickerSelector) -> Unit,
    onEmoticonSelected: (Emoticon) -> Unit,
    onStickerSelected: (Emoticon) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One list holds both kinds; split it once per list rather than on every recomposition.
    val (stickers, emotes) = remember(emoticons) {
        emoticons.partition { it.isSticker }
            .let { (stickers, emotes) -> stickers.toPersistentList() to emotes.toPersistentList() }
    }

    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        contentColor = MaterialTheme.colorScheme.secondary,
    ) {
        Column {
            // Tab selector
            Row(
                Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween,
                ),
            ) {
                EmojiStickerSelector.entries.forEachIndexed { index, selector ->
                    ToggleButton(
                        checked = selectedTab == selector,
                        onCheckedChange = { onTabSelected(selector) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()

                            EmojiStickerSelector.entries.lastIndex ->
                                ButtonGroupDefaults.connectedTrailingButtonShapes()

                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        content = { Text(selector.name.lowercase().capitalize()) },
                    )
                }
            }

            HorizontalDivider()

            // Content based on selected tab
            when (selectedTab) {
                EmojiStickerSelector.EMOTICONS -> EmoticonGrid(
                    emoticons = emotes,
                    cellSize = EMOTICON_CELL,
                    emptyText = "You have no Steam emoticons",
                    onEmoticonSelected = onEmoticonSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                )

                EmojiStickerSelector.STICKERS -> EmoticonGrid(
                    emoticons = stickers,
                    cellSize = STICKER_CELL,
                    emptyText = "You have no Steam stickers",
                    onEmoticonSelected = onStickerSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/**
 * Grid of emoticons or stickers as square buttons of exactly [cellSize], as many per row as
 * fit, centred. The size is fixed rather than stretched to the width so that a button is the
 * same size whether its image has loaded, is loading, or failed;
 * [onEmoticonSelected] fires for whichever was tapped.
 */
@Composable
fun EmoticonGrid(
    emoticons: ImmutableList<Emoticon>,
    cellSize: Dp,
    emptyText: String,
    onEmoticonSelected: (Emoticon) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (emoticons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.FixedSize(cellSize),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        items(items = emoticons, key = { it.name }) { emoticon ->
            EmoticonItem(
                emoticon = emoticon,
                size = cellSize,
                onClick = { onEmoticonSelected(emoticon) },
            )
        }
    }
}

/** One emoticon or sticker, drawn from Steam's economy image CDN by its name. */
@Composable
fun EmoticonItem(
    emoticon: Emoticon,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The image area is given its size outright, like the button, so nothing about the
    // image (not yet loaded, larger than the button, missing) can change the layout.
    val imageSize = size - IMAGE_INSET * 2

    Surface(
        modifier = modifier.size(size),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        CoilImage(
            imageModel = {
                if (emoticon.isSticker) {
                    // Some sticker names contain spaces.
                    Utils.Constants.STICKER_URL + Uri.encode(emoticon.name)
                } else {
                    Utils.Constants.EMOTICON_URL + emoticon.name
                }
            },
            modifier = Modifier
                .padding(IMAGE_INSET)
                .size(imageSize),
            imageOptions = ImageOptions(
                contentScale = ContentScale.Fit,
                contentDescription = emoticon.name,
            ),
            // The name stands in until the image arrives, and for good if it never does.
            loading = { EmoticonName(emoticon, imageSize) },
            failure = { EmoticonName(emoticon, imageSize) },
        )
    }
}

@Composable
private fun EmoticonName(emoticon: Emoticon, size: Dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            text = emoticon.name.take(if (emoticon.isSticker) 12 else 3),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

class EmojiSelectorStateProvider : PreviewParameterProvider<EmojiStickerSelector> {
    override val values = sequenceOf(EmojiStickerSelector.EMOTICONS, EmojiStickerSelector.STICKERS)
}

@Preview(showBackground = true, heightDp = 300, widthDp = 360)
@Composable
fun EmojiStickerSelectorOnlyPreview(
    @PreviewParameter(EmojiSelectorStateProvider::class) value: EmojiStickerSelector,
) {
    var selectedTab by remember { mutableStateOf(value) }
    MaterialTheme {
        Box(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
        ) {
            EmojiStickerSelectorComponent(
                emoticons = persistentListOf(
                    Emoticon(name = "steamhappy", appID = 0, isSticker = false),
                    Emoticon(name = "steamsad", appID = 0, isSticker = false),
                    Emoticon(name = "Winter2019JingleIntensifies", appID = 0, isSticker = true),
                ),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onEmoticonSelected = { /* Handle selection */ },
                onStickerSelected = { /* Handle selection */ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
