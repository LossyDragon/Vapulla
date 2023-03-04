package `in`.dragonbra.vapulla.compose.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOnline
import `in`.dragonbra.vapulla.util.Utils

private val stickerPattern = "\\[sticker type=\"(.*?)\" limit=\"0\"]\\[/sticker]".toRegex()
private val emoticonPattern = "\\[emoticon](.*?)\\[/emoticon]".toRegex()
private val publishedFilePattern = (
    "publishedfile\\surl=\"([^\"]+)\"\\s[^>]*" +
        "preview_url=\"([^\"]+)\"\\s[^>]*title=\"([^\"]+)\""
    ).toRegex()
private val domainNamePattern = "^(?:https?://)?(?:[^@/\\n]+@)?(?:www\\.)?([^:/\\n]+)".toRegex()

// TODO: Maybe this can also be versatile enough to use as the Emoji Picker?
@Composable
fun PaperPlane(
    modifier: Modifier = Modifier,
    text: String,
    isPreviewMode: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // Stickers
    if (stickerPattern.matches(text)) {
        if (isPreviewMode) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                        append("Sent a sticker")
                    }
                }
            )
            return
        }

        val (sticker) = stickerPattern.find(text)!!.destructured
        StickerImage(modifier = Modifier.size(150.dp), stickerUrl = (Utils.STICKER_URL + sticker))
        return
    }

    // Rich Previews
    publishedFilePattern.find(text)?.let {
        val uriHandler = LocalUriHandler.current
        val (url, preview, title) = it.destructured
        if (isPreviewMode) {
            Text(
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { uriHandler.openUri(url) },
                text = buildAnnotatedString {
                    val style = SpanStyle(color = friendOnline, fontStyle = FontStyle.Italic)
                    withStyle(style = style) {
                        append(url)
                        addStringAnnotation(
                            tag = "URL",
                            annotation = url,
                            start = 0,
                            end = url.length
                        )
                    }
                }
            )
            return
        }

        Card(Modifier.clickable { uriHandler.openUri(url) }) {
            Column(modifier = Modifier.padding(6.dp)) {
                StaticImage(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .size(256.dp),
                    avatarUrl = preview
                )
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp,
                    text = domainNamePattern.find(url)!!.groupValues[1].uppercase()
                )
                Text(
                    modifier = Modifier.padding(start = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = title
                )
            }
        }
        return
    }

    // Emoticons
    val contentMap = mutableMapOf<String, InlineTextContent>()
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        emoticonPattern.findAll(text).forEach { matchResult ->
            val (emoticonName) = matchResult.destructured
            append(text.substring(lastIndex, matchResult.range.first))
            contentMap.getOrPut(emoticonName) {
                InlineTextContent(Placeholder(20.sp, 20.sp, PlaceholderVerticalAlign.Center)) {
                    StaticImage(
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
        color = Color.White,
        modifier = modifier,
        text = annotatedString,
        inlineContent = contentMap,
        maxLines = maxLines,
        overflow = overflow
    )
}

@Preview
@Composable
private fun Preview_PaperPlane() {
    val input = """
        [publishedfile url="https://steamcommunity.com/sharedfiles/filedetails/?id=2862951508" 
        fileid="2862951508" 
        preview_url="https://steamuserimages-a.akamaihd.net/ugc/
        1803152413041139370/F6EC1CD906D49354DAF85F2C0D3A2AF913C95916/" 
        creator="76561197997988385" 
        creator_appid="766" 
        num_comments_public="1" 
        title="Dead Before Dawn (Uncut)" 
        description=""BrInG mE a BeEr, h'I nEeD a BeEeEeR, mY FaVoRiTe BrAnD iS mUdWiSeR" (c) 
        Hank Kowalski This is a very first version of this campaign ever released before even 
        Left 4 Dead 2 came out. It has cut from further versions tasks, randomizations, 
        voicelines, overa" 
        votes_up="134" 
        votes_down="25" 
        file_type="2" 
        file_type="2"]
        https://steamcommunity.com/sharedfiles/filedetails/?id=2862951508
        [/publishedfile]
    """.trimIndent()
    VapullaTheme {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PaperPlane(
                text = "Left [emoticon]health[/emoticon] 4 [emoticon]missing[/emoticon] Dead 2!"
            )
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(text = "No Emojis, but Left 4 Dead 2!")
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(text = "[sticker type=\"Winter2019HappyFire\" limit=\"0\"][/sticker]")
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(
                isPreviewMode = false,
                text = "[sticker type=\"Steam Pal\" limit=\"0\"][sticker]"
            )
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(
                isPreviewMode = true,
                text = "[sticker type=\"Winter2019JingleIntensifies\" limit=\"0\"][/sticker]"
            )
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(isPreviewMode = true, text = input)
            Spacer(modifier = Modifier.height(30.dp))
            PaperPlane(isPreviewMode = false, text = input)
        }
    }
}
