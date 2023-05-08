package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import `in`.dragonbra.vapulla.compose.components.PaperPlane
import `in`.dragonbra.vapulla.compose.ui.theme.ChatBubbleFriendShape
import `in`.dragonbra.vapulla.compose.ui.theme.ChatBubbleMeShape
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.util.AnimatedPngDecoder
import `in`.dragonbra.vapulla.data.entity.ChatMessage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageItem(
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader,
    chatMessage: ChatMessage
) {
    val message = remember { chatMessage }

    var bubbleColor = MaterialTheme.colorScheme.primary
    var bubbleShape = remember { ChatBubbleFriendShape }
    var bubbleSide = remember { Alignment.CenterEnd }
    var bubbleTimeSide = remember { Alignment.End }

    if (!message.fromLocal) {
        bubbleColor = MaterialTheme.colorScheme.surfaceVariant
        bubbleShape = ChatBubbleMeShape
        bubbleSide = Alignment.CenterStart
        bubbleTimeSide = Alignment.Start
    }

    val clipboard = LocalClipboardManager.current
    val configuration = LocalConfiguration.current
    val haptics = LocalHapticFeedback.current
    val maxWidth = configuration.screenWidthDp
    Box(
        modifier = modifier
            .waterfallPadding()
            .fillMaxWidth()
            .padding(8.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    clipboard.setText(AnnotatedString(message.message))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ),
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
                PaperPlane(
                    modifier = Modifier.widthIn(64.dp),
                    imageLoader = imageLoader,
                    text = message.message
                )

                Text(
                    text = message.formattedChatTime(),
                    fontSize = 8.sp,
                    color = friendOffline
                )
            }
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
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.components {
            add(AnimatedPngDecoder.Factory())
        }.build()

    VapullaTheme {
        Column(Modifier.fillMaxWidth()) {
            ChatMessageItem(
                imageLoader = imageLoader,
                chatMessage = ChatMessage(
                    accountid = 1,
                    fromLocal = false,
                    isUnread = false,
                    message = randomMsg,
                    timestamp = (1_000_000..5_000_000).random().toLong()
                )
            )
            Spacer(Modifier.height(8.dp))
            ChatMessageItem(
                imageLoader = imageLoader,
                chatMessage = ChatMessage(
                    accountid = 1,
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
