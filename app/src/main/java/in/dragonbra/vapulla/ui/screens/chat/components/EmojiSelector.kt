package `in`.dragonbra.vapulla.ui.screens.chat.components

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.util.helpers.capitalize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

enum class EmojiStickerSelector {
    EMOTICONS,
    STICKERS,
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EmojiStickerSelectorComponent(
    emoticons: ImmutableList<Emoticon>,
    selectedTab: EmojiStickerSelector,
    onTabSelected: (EmojiStickerSelector) -> Unit,
    onEmoticonSelected: (Emoticon) -> Unit,
    onStickerSelected: (Emoticon) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                            EmojiStickerSelector.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        content = { Text(selector.name.lowercase().capitalize()) },
                    )
                }
            }

            HorizontalDivider()

            // Content based on selected tab
            when (selectedTab) {
                EmojiStickerSelector.EMOTICONS -> {
                    EmoticonGrid(
                        emoticons = emoticons.filter { !it.isSticker }.toPersistentList(),
                        onEmoticonSelected = onEmoticonSelected,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }

                EmojiStickerSelector.STICKERS -> {
                    EmoticonGrid(
                        emoticons = emoticons.filter { it.isSticker }.toPersistentList(),
                        onStickerSelected = onStickerSelected,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun EmoticonGrid(
    modifier: Modifier = Modifier,
    emoticons: ImmutableList<Emoticon>,
    onEmoticonSelected: (Emoticon) -> Unit = { },
    onStickerSelected: (Emoticon) -> Unit = { },
) {
    if (emoticons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No emoticons available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(emoticons) { emoticon ->
            EmoticonItem(
                emoticon = emoticon,
                onClick = { onEmoticonSelected(emoticon) },
            )
        }
    }
}

@Composable
fun EmoticonItem(emoticon: Emoticon, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(48.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Placeholder representation - you'd replace this with actual emoticon images
            Text(
                text = emoticon.name.take(2).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
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
                emoticons = persistentListOf(),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onEmoticonSelected = { /* Handle selection */ },
                onStickerSelected = { /* Handle selection */ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
