package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.service.api.MessageReaction
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.rememberExpandedSheetState
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Side of the picture in a reaction chip. */
private val REACTION_IMAGE = 18.dp

/** Height of the emoticon and sticker grid in the reaction picker. */
private val PICKER_HEIGHT = 360.dp

/**
 * What Steam calls [emoticon] as a reaction: an emoticon's name between colons, a
 * sticker's name as it is.
 */
internal fun reactionOf(emoticon: Emoticon): String =
    if (emoticon.isSticker) emoticon.name else ":${emoticon.name}:"

/** The reaction's name without the colons an emoticon's comes in. */
private val MessageReaction.name: String get() = reaction.trim(':')

/** Where Steam serves the reaction's picture from. */
private val MessageReaction.imageUrl: String
    get() = if (isSticker) {
        // Some sticker names contain spaces.
        Utils.Constants.STICKER_URL + Uri.encode(reaction)
    } else {
        Utils.Constants.EMOTICON_URL + name
    }

/**
 * The reactions under a message, each with how many gave it. Tapping one gives it too, or
 * takes it back when it is one of ours, which are the outlined ones.
 */
@Composable
internal fun ReactionRow(
    reactions: ImmutableList<MessageReaction>,
    onReactionClick: (MessageReaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { reaction ->
            ReactionChip(reaction = reaction, onClick = { onReactionClick(reaction) })
        }
    }
}

@Composable
private fun ReactionChip(reaction: MessageReaction, onClick: () -> Unit) {
    val people = if (reaction.count == 1) "1 person" else "${reaction.count} people"
    val description = "${reaction.name}, $people" + if (reaction.mine) ", you among them" else ""

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (reaction.mine) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (reaction.mine) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        border = if (reaction.mine) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                imageModel = { reaction.imageUrl },
                modifier = Modifier.size(REACTION_IMAGE),
                imageOptions = ImageOptions(contentScale = ContentScale.Fit),
            )
            Text(text = reaction.count.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The account's emoticons and stickers to pick a reaction from, in a sheet. Steam only
 * takes those as reactions, not the keyboard's emoji.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReactionPickerSheet(
    emoticons: ImmutableList<Emoticon>,
    onPick: (Emoticon) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(EmojiStickerSelector.EMOTICONS) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberExpandedSheetState()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "React with",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            EmojiStickerSelectorComponent(
                emoticons = emoticons,
                selectedTab = tab,
                onTabSelected = { tab = it },
                onEmoticonSelected = onPick,
                onStickerSelected = onPick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PICKER_HEIGHT),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            ReactionRow(
                reactions = persistentListOf(
                    MessageReaction(":steamhappy:", isSticker = false, count = 3, mine = true),
                    MessageReaction(":steamsad:", isSticker = false, count = 1, mine = false),
                ),
                onReactionClick = {},
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
