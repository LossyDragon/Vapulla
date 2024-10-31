package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.compose.components.PaperPlane
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.data.entity.ChatMessage

@Composable
fun ChatMessageItem(
    modifier: Modifier = Modifier,
    chatMessage: ChatMessage
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight(align = CenterVertically),
        horizontalAlignment = if (chatMessage.fromLocal) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 256.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (chatMessage.fromLocal) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            PaperPlane(
                modifier = Modifier.padding(8.dp),
                text = chatMessage.message
            )

            Text(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .align(if (chatMessage.fromLocal) Alignment.End else Alignment.Start),
                text = chatMessage.formattedTime,
                fontSize = 10.sp,
                color = friendOffline
            )
        }
    }
}

@Composable
fun ChatMessageDateHeader(
    isVisible: Boolean = true,
    dateStamp: String
) {
    val date = remember { dateStamp }
    if (!isVisible) {
        return
    }

    Row {
        val divider = @Composable {
            HorizontalDivider(
                modifier = Modifier
                    .weight(.4f)
                    .padding(horizontal = 8.dp)
                    .align(CenterVertically),
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
            text = date,
            textAlign = TextAlign.Center
        )
        divider()
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
        Column(Modifier.fillMaxWidth()) {
            ChatMessageItem(
                chatMessage = ChatMessage(
                    accountId = 1,
                    fromLocal = false,
                    isUnread = false,
                    message = randomMsg,
                    timestamp = (1_000_000..5_000_000).random().toLong()
                )
            )
            Spacer(Modifier.height(8.dp))
            ChatMessageItem(
                chatMessage = ChatMessage(
                    accountId = 1,
                    fromLocal = true,
                    isUnread = false,
                    message = randomMsg,
                    timestamp = (1_000_000..5_000_000).random().toLong()
                )
            )
        }
    }
}

@Preview
@Composable
private fun Preview_ChatMessageDateHeader() {
    VapullaTheme {
        ChatMessageDateHeader(isVisible = true, dateStamp = "Wednesday - January 5, 2023")
    }
}
