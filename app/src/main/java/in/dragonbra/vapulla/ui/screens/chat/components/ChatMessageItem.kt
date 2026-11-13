package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.content.res.Configuration
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.ui.composables.BBCodeText
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeString

/** One chat bubble, aligned right for messages we sent and left for the friend's. */
@Composable
internal fun ChatMessageItem(
    message: ChatMessage,
    isFromLocal: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromLocal) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isFromLocal) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                BBCodeText(
                    text = message.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isFromLocal) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )

                Text(
                    text = message.timestamp.toTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFromLocal) {
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
    )

    VapullaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ChatMessageItem(
                    message = message("Want to play some games later?", local = false),
                    isFromLocal = false,
                )
                ChatMessageItem(
                    message = message("Sure, [b]Team Fortress 2[/b]?", local = true),
                    isFromLocal = true,
                )
            }
        }
    }
}
