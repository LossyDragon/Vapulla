package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.StoredReaction
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.service.api.MessageReaction
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * One chat bubble, aligned right for messages we sent and left for the friend's, with the
 * reactions given to it underneath.
 *
 * @param onReactionClick a reaction under the message was tapped, to give it too or take it
 *   back.
 * @param onPickReaction the bubble was held, to pick a reaction for it. Not offered for a
 *   message Steam has not confirmed yet.
 */
@Composable
internal fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onReactionClick: (MessageReaction) -> Unit = {},
    onPickReaction: () -> Unit = {},
) {
    // A friend chat has two people in it, so whoever reacted and is not the friend is us.
    val reactions = remember(message.reactions, message.friendId) {
        val friendAccountId = message.friendId.toInt()

        message.reactions.map { stored ->
            MessageReaction(
                reaction = stored.reaction,
                isSticker = stored.isSticker,
                count = stored.reactors.size,
                mine = stored.reactors.any { it != friendAccountId },
            )
        }.toImmutableList()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromLocal) Arrangement.End else Arrangement.Start,
    ) {
        MessageBubble(
            text = message.message,
            timestamp = message.timestamp,
            isFromLocal = message.fromLocal,
            reactions = reactions,
            onReactionClick = onReactionClick,
            onLongPress = onPickReaction.takeIf { message.timestampConfirmed },
        )
    }
}

/**
 * The bubble itself, shared by friend and group chats: the message with its time underneath,
 * [senderName] on top where a chat has more than one other person in it, and the reactions
 * given to it below.
 *
 * @param highlighted the message calls on us, so it is set apart from the others.
 * @param onLongPress the bubble was held; null when holding it does nothing.
 */
@Composable
internal fun MessageBubble(
    text: String,
    timestamp: Long,
    isFromLocal: Boolean,
    modifier: Modifier = Modifier,
    senderName: String? = null,
    highlighted: Boolean = false,
    reactions: ImmutableList<MessageReaction> = persistentListOf(),
    onReactionClick: (MessageReaction) -> Unit = {},
    onLongPress: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.widthIn(max = 280.dp),
        horizontalAlignment = if (isFromLocal) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Bubble(
            text = text,
            timestamp = timestamp,
            isFromLocal = isFromLocal,
            senderName = senderName,
            highlighted = highlighted,
            onLongPress = onLongPress,
        )

        if (reactions.isNotEmpty()) {
            ReactionRow(reactions = reactions, onReactionClick = onReactionClick)
        }
    }
}

@Composable
private fun Bubble(
    text: String,
    timestamp: Long,
    isFromLocal: Boolean,
    senderName: String?,
    highlighted: Boolean,
    onLongPress: (() -> Unit)?,
) {
    // Held anywhere but on a link or a picture, which take their own taps. No ripple: a tap
    // on the bubble does nothing.
    val holdModifier = if (onLongPress == null) {
        Modifier
    } else {
        Modifier.combinedClickable(
            interactionSource = null,
            indication = null,
            onLongClickLabel = "React",
            onLongClick = onLongPress,
            onClick = {},
        )
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = when {
            isFromLocal -> MaterialTheme.colorScheme.primaryContainer
            highlighted -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = holdModifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (senderName != null) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val textColor = when {
                isFromLocal -> MaterialTheme.colorScheme.onPrimaryContainer
                highlighted -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }

            if (LocalAppSettings.current.rawBBCode) {
                // The message exactly as Steam delivered it; see Settings > Chat.
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                )
            } else {
                BBCodeText(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
            }

            Text(
                text = timestamp.toTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isFromLocal || highlighted) {
                    textColor.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun message(text: String, local: Boolean) = ChatMessage(
        message = text,
        timestamp = 1_760_722_982_000,
        friendId = 1,
        fromLocal = local,
        unread = false,
        timestampConfirmed = true,
        reactions = listOfNotNull(
            StoredReaction(":steamhappy:", reactors = listOf(1)).takeIf { local },
        ),
    )

    VapullaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ChatMessageItem(message("Want to play some games later?", local = false))
                ChatMessageItem(message("Sure, [b]Team Fortress 2[/b]?", local = true))
            }
        }
    }
}
