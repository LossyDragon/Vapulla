package `in`.dragonbra.vapulla.compose.components

import android.webkit.URLUtil
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.StickerImage
import `in`.dragonbra.vapulla.core.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

private val stickerPattern = "\\[sticker type=\"(.*?)\" limit=\"0\"]\\[/sticker]".toRegex()
private val emoticonPattern = "\\[emoticon](.*?)\\[/emoticon]".toRegex()
private val steamUrlPattern = "](.*?)\\[".toRegex()
private val domainNamePattern = "^(?:https?://)?(?:[^@/\\n]+@)?(?:www\\.)?([^:/\\n]+)".toRegex()

data class OpenGraphData(
    val url: String,
    val ogTitle: String?,
    val ogDescription: String?,
    val ogImage: String?
)

fun parseOpenGraphData(url: String, html: String?): OpenGraphData? {
    if (html == null) return null

    var title: String? = null
    var description: String? = null
    var imageUrl: String? = null
    Jsoup.parse(html).getElementsByTag("meta").forEach { tag ->
        when (tag.attr("property")) {
            "og:title" -> title = tag.attr("content")
            "og:description" -> description = tag.attr("content")
            "og:image" -> imageUrl = tag.attr("content")
        }
    }

    return OpenGraphData(url, title, description, imageUrl)
}

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
        StickerImage(
            modifier = Modifier.size(150.dp),
            url = Constants.STICKER_URL + sticker
        )
        return
    }

    // Rich Previews -- Open Graph Meta
    // This is probably a terrible way to do this, but I want to see the concept.
    val urlMatchResult = steamUrlPattern.find(text)
    if (urlMatchResult?.groupValues?.isNotEmpty() == true) {
        val capturedUrl = urlMatchResult.groupValues[1]
        val isValidUrl = remember { URLUtil.isValidUrl(capturedUrl) }
        var openGraphData: OpenGraphData? by remember { mutableStateOf(null) }
        if (isValidUrl) {
            val scope = rememberCoroutineScope()
            var client: OkHttpClient? = remember { OkHttpClient() }
            var request: Request? = remember { Request.Builder().url(capturedUrl).build() }
            DisposableEffect(capturedUrl) {
                scope.launch(Dispatchers.IO) {
                    val response = client?.newCall(request!!)?.execute()
                    val htmlBody = response?.body?.string()
                    openGraphData = parseOpenGraphData(capturedUrl, htmlBody)
                }

                onDispose {
                    client = null
                    request = null
                }
            }
        }

        openGraphData?.let {
            val uriHandler = LocalUriHandler.current

            if (isPreviewMode) {
                Text(
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { uriHandler.openUri(it.url) },
                    text = buildAnnotatedString {
                        val style = SpanStyle(color = friendOnline, fontStyle = FontStyle.Italic)
                        withStyle(style = style) {
                            append(it.url)
                            addStringAnnotation(
                                tag = "URL",
                                annotation = it.url,
                                start = 0,
                                end = it.url.length
                            )
                        }
                    }
                )
                return
            }

            Card(modifier = Modifier.clickable { uriHandler.openUri(it.url) }) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    it.ogImage?.let {
                        StaticImage(
                            modifier = Modifier.size(height = 126.dp, width = 240.dp),
                            contentScale = ContentScale.Fit,
                            url = it
                        )
                    }
                    Text(
                        modifier = Modifier.padding(start = 0.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        text = domainNamePattern.find(it.url)!!.groupValues[1].uppercase()
                    )
                    Text(
                        modifier = Modifier.padding(start = 0.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = it.ogTitle ?: ""
                    )
                }
            }

            return
        }
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
                        url = Constants.EMOTE_URL + emoticonName
                    )
                }
            }
            appendInlineContent(emoticonName)
            lastIndex = matchResult.range.last + 1
        }

        append(text.substring(lastIndex, text.length))
    }

    // TODO text still gets rendered even though we captured an item above
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
    val input = "https://github.com/Longi94/Vapulla"
    VapullaTheme {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PaperPlane(
                text = "Left [emoticon]health[/emoticon] 4 [emoticon]missing[/emoticon] Dead 2!"
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(text = "No Emojis, but Left 4 Dead 2!")
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(text = "[sticker type=\"Winter2019HappyFire\" limit=\"0\"][/sticker]")
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(
                isPreviewMode = false,
                text = "[sticker type=\"Steam Pal\" limit=\"0\"][/sticker]"
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(
                isPreviewMode = true,
                text = "[sticker type=\"Winter2019JingleIntensifies\" limit=\"0\"][/sticker]"
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(isPreviewMode = true, text = input)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
            PaperPlane(isPreviewMode = false, text = input)
        }
    }
}
