package `in`.dragonbra.vapulla.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.AvatarImage
import `in`.dragonbra.vapulla.util.Utils

val stickerPattern = "\\[sticker type=\"(.*?)\" limit=0]\\[sticker]".toRegex()

// TODO: Move this to it's correct package, maybe compose.utils?
// TODO: Mature this so that stickers can be rendered, APNG though.
// TODO: Maybe this can also be versatile enough to use as the Emoji Picker?
@Composable
fun PaperPlane(
    modifier: Modifier = Modifier,
    text: String,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val contentMap = mutableMapOf<String, InlineTextContent>()

    val annotatedString = buildAnnotatedString {
        val pattern = "\\[emoticon](.*?)\\[/emoticon]".toRegex()
        var lastIndex = 0

        pattern.findAll(text).forEach { matchResult ->
            val (emoticonName) = matchResult.destructured
            append(text.substring(lastIndex, matchResult.range.first))
            contentMap.getOrPut(emoticonName) {
                InlineTextContent(Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)) {
                    AvatarImage(
                        modifier = Modifier.size(20.dp),
                        avatarUrl = "${Utils.EMOTE_URL}$emoticonName"
                    )
                }
            }
            appendInlineContent(emoticonName)
            lastIndex = matchResult.range.last + 1
        }

        append(text.substring(lastIndex, text.length))
    }

    Text(
        modifier = modifier,
        text = annotatedString,
        inlineContent = contentMap,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Preview
@Composable
private fun Preview_PaperPlane() {
    VapullaTheme {
        Column {
            PaperPlane(text = "Left [emoticon]health[/emoticon] 4 [emoticon]missing[/emoticon] Dead 2!")
            Spacer(modifier = Modifier.height(10.dp))
            PaperPlane(text = "No Emojis, but Left 4 Dead 2!")
        }
    }
}
