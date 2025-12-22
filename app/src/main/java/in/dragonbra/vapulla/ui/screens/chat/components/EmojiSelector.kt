package `in`.dragonbra.vapulla.ui.screens.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.ui.screens.login.components.QrCodeStateProvider

enum class EmojiStickerSelector {
    EMOTICONS,
    STICKERS,
}

@Composable
fun EmojiStickerSelectorComponent(
    emoticons: List<Emoticon>,
    selectedTab: EmojiStickerSelector,
    onTabSelected: (EmojiStickerSelector) -> Unit,
    onEmoticonSelected: (Emoticon) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        Column {
            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ExtendedSelectorInnerButton(
                    text = "Emoticons",
                    onClick = { onTabSelected(EmojiStickerSelector.EMOTICONS) },
                    selected = selectedTab == EmojiStickerSelector.EMOTICONS,
                    modifier = Modifier.weight(1f)
                )
                ExtendedSelectorInnerButton(
                    text = "Stickers",
                    onClick = { onTabSelected(EmojiStickerSelector.STICKERS) },
                    selected = selectedTab == EmojiStickerSelector.STICKERS,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider()

            // Content based on selected tab
            when (selectedTab) {
                EmojiStickerSelector.EMOTICONS -> {
                    EmoticonGrid(
                        emoticons = emoticons,
                        onEmoticonSelected = onEmoticonSelected,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
                EmojiStickerSelector.STICKERS -> {
                    // StickerGrid(
                    //     stickers = stickers,
                    //     onStickerSelected = onEmoticonSelected,
                    //     modifier = Modifier
                    //         .fillMaxSize()
                    //         .padding(16.dp)
                    // )
                }
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
        containerColor = if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        else Color.Transparent,
        disabledContainerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
    )
    TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .height(36.dp),
        colors = colors,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun EmoticonGrid(
    emoticons: List<Emoticon>,
    onEmoticonSelected: (Emoticon) -> Unit,
    modifier: Modifier = Modifier
) {
    if (emoticons.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No emoticons available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(emoticons) { emoticon ->
            EmoticonItem(
                emoticon = emoticon,
                onClick = { onEmoticonSelected(emoticon) }
            )
        }
    }
}

@Composable
fun EmoticonItem(
    emoticon: Emoticon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(48.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder representation - you'd replace this with actual emoticon images
            Text(
                text = emoticon.name.take(2).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1
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
    @PreviewParameter(EmojiSelectorStateProvider::class) value: EmojiStickerSelector
) {
    MaterialTheme {
        EmojiStickerSelectorComponent(
            emoticons = listOf(),
            selectedTab = value,
            onTabSelected = {  },
            onEmoticonSelected = { /* Handle selection */ },
            modifier = Modifier
                .fillMaxSize()
        )
    }
}
