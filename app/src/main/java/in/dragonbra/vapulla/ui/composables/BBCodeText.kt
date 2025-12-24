package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Composable that renders Steam BB Code formatted text
 * @param text The BB Code formatted text to parse and display
 * @param modifier Modifier for the composable
 * @param onUrlClick Callback for when a URL is clicked
 */
@Composable
fun BBCodeText(
    text: String,
    modifier: Modifier = Modifier,
    onUrlClick: ((String) -> Unit)? = null
) {
    val parsedContent = remember(text) {
        parseSteamBBCode(text)
    }

    Column(modifier = modifier) {
        parsedContent.forEach { element ->
            BBCodeElement(element = element, onUrlClick = onUrlClick)
        }
    }
}

@Composable
private fun BBCodeElement(
    element: BBElement,
    onUrlClick: ((String) -> Unit)?
) {
    when (element) {
        is BBElement.Text -> {
            BBCodeTextElement(element = element, onUrlClick = onUrlClick)
        }

        is BBElement.Header -> {
            Text(
                text = element.content,
                style = when (element.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.headlineSmall
                },
                fontWeight = FontWeight.Bold
            )
        }

        is BBElement.HorizontalRule -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        is BBElement.BBList -> {
            BBCodeList(element)
        }

        is BBElement.Quote -> {
            BBCodeQuote(element)
        }

        is BBElement.Code -> {
            BBCodeBlock(element.content)
        }
    }
}

@Composable
private fun BBCodeTextElement(
    element: BBElement.Text,
    onUrlClick: ((String) -> Unit)?
) {
    val uriHandler = LocalUriHandler.current
    val revealedSpoilers = remember { mutableStateMapOf<String, Boolean>() }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val inlineContent = buildInlineContentMap(element.segments)

    val annotatedString = buildAnnotatedString {
        appendStyledText(element, revealedSpoilers)
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        modifier = Modifier.pointerInput(annotatedString) {
            detectTapGestures { offset ->
                layoutResult.value?.let { layout ->
                    val position = layout.getOffsetForPosition(offset)

                    // Check for URL click first
                    annotatedString
                        .getStringAnnotations("URL", position, position)
                        .firstOrNull()
                        ?.let { annotation ->
                            val url = annotation.item
                            if (onUrlClick != null) {
                                onUrlClick(url)
                            } else {
                                // Ensure URL has a scheme
                                val fullUrl = if (!url.startsWith("http://") &&
                                    !url.startsWith("https://")
                                ) {
                                    "https://$url"
                                } else {
                                    url
                                }
                                try {
                                    uriHandler.openUri(fullUrl)
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }
                            }
                            return@detectTapGestures
                        }

                    // Check for spoiler click
                    annotatedString
                        .getStringAnnotations("spoiler", position, position)
                        .firstOrNull()
                        ?.let { annotation ->
                            revealedSpoilers[annotation.item] =
                                !(revealedSpoilers[annotation.item] ?: false)
                        }
                }
            }
        },
        onTextLayout = { layoutResult.value = it }
    )
}

@Composable
private fun BBCodeList(list: BBElement.BBList) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
        list.items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = if (list.ordered) "${index + 1}. " else "• ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BBCodeQuote(quote: BBElement.Quote) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (quote.author != null) {
                Text(
                    text = "Originally posted by ${quote.author}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = quote.content,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun BBCodeBlock(content: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color(0xFF1E1E1E),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = content,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFD4D4D4),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun buildInlineContentMap(
    segments: ImmutableList<TextSegment>
): Map<String, InlineTextContent> {
    return buildMap {
        segments.forEach { segment ->
            when (segment) {
                is TextSegment.Emoticon -> {
                    put(
                        segment.id, InlineTextContent(
                            placeholder = Placeholder(
                                width = 18.sp,
                                height = 18.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            CoilImage(
                                imageModel = { Utils.Constants.EMOTICON_URL + segment.name },
                                imageOptions = ImageOptions(
                                    contentDescription = segment.name,
                                    contentScale = ContentScale.Fit
                                ),
                                modifier = Modifier.size(18.dp),
                                loading = {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp))
                                },
                                failure = {
                                    Icon(
                                        imageVector = Icons.Default.QuestionMark,
                                        contentDescription = "Failed to load emoticon",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        })
                }

                is TextSegment.Sticker -> {
                    put(
                        segment.id, InlineTextContent(
                            placeholder = Placeholder(
                                width = 150.sp,
                                height = 150.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            CoilImage(
                                imageModel = { Utils.Constants.STICKER_URL + segment.type },
                                imageOptions = ImageOptions(
                                    contentDescription = segment.type,
                                    contentScale = ContentScale.Fit
                                ),
                                modifier = Modifier.size(150.dp),
                                loading = {
                                    CircularProgressIndicator(modifier = Modifier.size(50.dp))
                                },
                                failure = {
                                    Icon(
                                        imageVector = Icons.Default.QuestionMark,
                                        contentDescription = "Failed to load sticker",
                                        modifier = Modifier.size(150.dp)
                                    )
                                }
                            )
                        })
                }

                else -> { /* No inline content needed for other segment types */
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyledText(
    element: BBElement.Text,
    revealedSpoilers: Map<String, Boolean>
) {
    element.segments.forEach { segment ->
        when (segment) {
            is TextSegment.Plain -> append(segment.text)
            is TextSegment.Styled -> {
                withStyle(segment.style.toSpanStyle()) {
                    append(segment.text)
                }
            }

            is TextSegment.Link -> {
                withStyle(
                    SpanStyle(
                        color = Color(0xFF4A90E2),
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    pushStringAnnotation(tag = "URL", annotation = segment.url)
                    append(segment.text)
                    pop()
                }
            }

            is TextSegment.Spoiler -> {
                val spoilerId = "spoiler_${segment.text.hashCode()}"
                val isRevealed = revealedSpoilers[spoilerId] ?: false

                pushStringAnnotation("spoiler", spoilerId)
                withStyle(
                    SpanStyle(
                        background = if (isRevealed) Color.Unspecified else Color.Black,
                        color = if (isRevealed) Color.Unspecified else Color.Black
                    )
                ) {
                    append(segment.text)
                }
                pop()
            }

            is TextSegment.Emoticon -> {
                appendInlineContent(segment.id, "[emoticon]")
            }

            is TextSegment.Sticker -> {
                appendInlineContent(segment.id, "[sticker]")
            }
        }
    }
}

// Data classes representing parsed BB Code elements
sealed class BBElement {
    data class Text(val segments: ImmutableList<TextSegment>) : BBElement()
    data class Header(val level: Int, val content: String) : BBElement()
    data object HorizontalRule : BBElement()
    data class BBList(val items: ImmutableList<String>, val ordered: Boolean) : BBElement()
    data class Quote(val content: String, val author: String?) : BBElement()
    data class Code(val content: String) : BBElement()
}

sealed class TextSegment {
    data class Plain(val text: String) : TextSegment()
    data class Styled(val text: String, val style: BBStyle) : TextSegment()
    data class Link(val text: String, val url: String) : TextSegment()
    data class Spoiler(val text: String) : TextSegment()
    data class Emoticon(val name: String, val id: String) : TextSegment()
    data class Sticker(val type: String, val id: String) : TextSegment()
}

@Immutable
data class BBStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false
) {
    fun toSpanStyle(): SpanStyle {
        return SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                underline && strikethrough -> TextDecoration.combine(
                    listOf(TextDecoration.Underline, TextDecoration.LineThrough)
                )

                underline -> TextDecoration.Underline
                strikethrough -> TextDecoration.LineThrough
                else -> null
            }
        )
    }
}

// Parser function
private fun parseSteamBBCode(input: String): ImmutableList<BBElement> {
    val elements = mutableListOf<BBElement>()
    var remainingText = input

    while (remainingText.isNotEmpty()) {
        // Try to match block-level elements first
        when {
            remainingText.startsWith("[h1]") -> {
                val (content, rest) = extractTag(remainingText, "h1")
                if (content != null) {
                    elements.add(BBElement.Header(1, content))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[h2]") -> {
                val (content, rest) = extractTag(remainingText, "h2")
                if (content != null) {
                    elements.add(BBElement.Header(2, content))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[h3]") -> {
                val (content, rest) = extractTag(remainingText, "h3")
                if (content != null) {
                    elements.add(BBElement.Header(3, content))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[hr][/hr]") -> {
                elements.add(BBElement.HorizontalRule)
                remainingText = remainingText.substring("[hr][/hr]".length)
            }

            remainingText.startsWith("[list]") -> {
                val (content, rest) = extractTag(remainingText, "list")
                if (content != null) {
                    val items = content.split("[*]")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toImmutableList()
                    elements.add(BBElement.BBList(items, ordered = false))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[olist]") -> {
                val (content, rest) = extractTag(remainingText, "olist")
                if (content != null) {
                    val items = content.split("[*]")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toImmutableList()
                    elements.add(BBElement.BBList(items, ordered = true))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[quote") -> {
                val (content, rest, author) = extractQuote(remainingText)
                if (content != null) {
                    elements.add(BBElement.Quote(content, author))
                    remainingText = rest
                } else {
                    break
                }
            }

            remainingText.startsWith("[code]") -> {
                val (content, rest) = extractTag(remainingText, "code")
                if (content != null) {
                    elements.add(BBElement.Code(content))
                    remainingText = rest
                } else {
                    break
                }
            }

            else -> {
                // Parse inline text until we hit a block-level tag
                val nextBlockTag = findNextBlockTag(remainingText)
                val textContent = if (nextBlockTag != -1) {
                    remainingText.substring(0, nextBlockTag)
                } else {
                    remainingText
                }

                if (textContent.isNotEmpty()) {
                    val segments = parseInlineText(textContent)
                    elements.add(BBElement.Text(segments))
                }

                remainingText = if (nextBlockTag != -1) {
                    remainingText.substring(nextBlockTag)
                } else {
                    ""
                }
            }
        }
    }

    return elements.toImmutableList()
}

private fun findNextBlockTag(text: String): Int {
    val blockTags = listOf("[h1]", "[h2]", "[h3]", "[hr]", "[list]", "[olist]", "[quote", "[code]")
    return blockTags.mapNotNull { tag ->
        val index = text.indexOf(tag)
        if (index >= 0) index else null
    }.minOrNull() ?: -1
}

@Suppress("RegExpRedundantEscape")
private fun parseInlineText(text: String): ImmutableList<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    var remaining = text
    var currentStyle = BBStyle()

    while (remaining.isNotEmpty()) {
        when {
            // Emoticons with colon format: ːemoticonː
            remaining.startsWith("ː") -> {
                val endColon = remaining.indexOf("ː", startIndex = 1)
                if (endColon != -1) {
                    val emoticonName = remaining.substring(1, endColon)
                    val emoticonId = "emoticon_$emoticonName"

                    segments.add(TextSegment.Emoticon(emoticonName, emoticonId))

                    remaining = remaining.substring(endColon + 1)
                } else {
                    // No closing colon, treat as plain text
                    segments.add(TextSegment.Plain("ː"))
                    remaining = remaining.substring(1)
                }
            }

            // Emoticons with tag format: [emoticon]name[/emoticon]
            remaining.startsWith("[emoticon]") -> {
                val endTag = remaining.indexOf("[/emoticon]")
                if (endTag != -1) {
                    val emoticonName = remaining.substring(10, endTag)
                    val emoticonId = "emoticon_$emoticonName"

                    segments.add(TextSegment.Emoticon(emoticonName, emoticonId))

                    remaining = remaining.substring(endTag + 11)
                } else {
                    segments.add(TextSegment.Plain(remaining.take(1)))
                    remaining = remaining.drop(1)
                }
            }

            // Stickers: [sticker type="name"][/sticker]
            remaining.startsWith("[sticker") -> {
                val stickerMatch =
                    Regex("""\[sticker type="([^"]+)"[^\]]*\]\[/sticker\]""").find(remaining)
                if (stickerMatch != null) {
                    val stickerType = stickerMatch.groupValues[1]
                    val stickerId = "sticker_$stickerType"

                    segments.add(TextSegment.Sticker(stickerType, stickerId))

                    remaining = remaining.substring(stickerMatch.value.length)
                } else {
                    segments.add(TextSegment.Plain(remaining.take(1)))
                    remaining = remaining.drop(1)
                }
            }

            remaining.startsWith("[b]") -> {
                currentStyle = currentStyle.copy(bold = true)
                remaining = remaining.substring(3)
            }

            remaining.startsWith("[/b]") -> {
                currentStyle = currentStyle.copy(bold = false)
                remaining = remaining.substring(4)
            }

            remaining.startsWith("[i]") -> {
                currentStyle = currentStyle.copy(italic = true)
                remaining = remaining.substring(3)
            }

            remaining.startsWith("[/i]") -> {
                currentStyle = currentStyle.copy(italic = false)
                remaining = remaining.substring(4)
            }

            remaining.startsWith("[u]") -> {
                currentStyle = currentStyle.copy(underline = true)
                remaining = remaining.substring(3)
            }

            remaining.startsWith("[/u]") -> {
                currentStyle = currentStyle.copy(underline = false)
                remaining = remaining.substring(4)
            }

            remaining.startsWith("[strike]") -> {
                currentStyle = currentStyle.copy(strikethrough = true)
                remaining = remaining.substring(8)
            }

            remaining.startsWith("[/strike]") -> {
                currentStyle = currentStyle.copy(strikethrough = false)
                remaining = remaining.substring(9)
            }

            remaining.startsWith("[spoiler]") -> {
                val endTag = remaining.indexOf("[/spoiler]")
                if (endTag != -1) {
                    val spoilerText = remaining.substring(9, endTag)
                    segments.add(TextSegment.Spoiler(spoilerText))
                    remaining = remaining.substring(endTag + 10)
                } else {
                    segments.add(TextSegment.Plain(remaining))
                    remaining = ""
                }
            }

            remaining.startsWith("[url=") -> {
                val urlMatch = Regex("""\[url=([^\]]+)\]([^\[]+)\[/url\]""").find(remaining)
                if (urlMatch != null) {
                    val url = urlMatch.groupValues[1]
                    val linkText = urlMatch.groupValues[2]
                    segments.add(TextSegment.Link(linkText, url))
                    remaining = remaining.substring(urlMatch.value.length)
                } else {
                    segments.add(TextSegment.Plain(remaining.take(1)))
                    remaining = remaining.drop(1)
                }
            }

            remaining.startsWith("[noparse]") -> {
                val endTag = remaining.indexOf("[/noparse]")
                if (endTag != -1) {
                    val content = remaining.substring(9, endTag)
                    segments.add(TextSegment.Plain(content))
                    remaining = remaining.substring(endTag + 10)
                } else {
                    segments.add(TextSegment.Plain(remaining))
                    remaining = ""
                }
            }

            else -> {
                // Find next tag
                val nextTag = remaining.indexOfAny(
                    listOf(
                        "[b]", "[/b]", "[i]", "[/i]", "[u]", "[/u]",
                        "[strike]", "[/strike]", "[spoiler]", "[url=",
                        "[noparse]", "[emoticon]", "[sticker", "ː"
                    )
                )

                val textContent = if (nextTag != -1) {
                    remaining.take(nextTag)
                } else {
                    remaining
                }

                if (textContent.isNotEmpty()) {
                    if (currentStyle == BBStyle()) {
                        segments.add(TextSegment.Plain(textContent))
                    } else {
                        segments.add(TextSegment.Styled(textContent, currentStyle))
                    }
                }

                remaining = if (nextTag != -1) {
                    remaining.substring(nextTag)
                } else {
                    ""
                }
            }
        }
    }

    return segments.toImmutableList()
}

private fun extractTag(text: String, tagName: String): Pair<String?, String> {
    val openTag = "[$tagName]"
    val closeTag = "[/$tagName]"

    if (!text.startsWith(openTag)) {
        return null to text
    }

    val endIndex = text.indexOf(closeTag)
    if (endIndex == -1) {
        return null to text
    }

    val content = text.substring(openTag.length, endIndex)
    val remaining = text.substring(endIndex + closeTag.length)

    return content to remaining
}

@Suppress("RegExpRedundantEscape")
private fun extractQuote(text: String): Triple<String?, String, String?> {
    val simpleQuoteRegex = Regex("""\[quote\](.*?)\[/quote\]""", RegexOption.DOT_MATCHES_ALL)
    val authorQuoteRegex =
        Regex("""\[quote=([^\]]+)\](.*?)\[/quote\]""", RegexOption.DOT_MATCHES_ALL)

    val authorMatch = authorQuoteRegex.find(text)
    if (authorMatch != null) {
        val author = authorMatch.groupValues[1]
        val content = authorMatch.groupValues[2]
        val remaining = text.substring(authorMatch.value.length)
        return Triple(content, remaining, author)
    }

    val simpleMatch = simpleQuoteRegex.find(text)
    if (simpleMatch != null) {
        val content = simpleMatch.groupValues[1]
        val remaining = text.substring(simpleMatch.value.length)
        return Triple(content, remaining, null)
    }

    return Triple(null, text, null)
}

@Preview
@Composable
private fun Preview_BBCodeText() {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(AnimatedPngDecoder.Factory()) }
            .build()
    }

    CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
        VapullaTheme {
            Surface {
                Column {
                    Text("The BB Code Text")
                    BBCodeText(
                        text = """
                [h1]Header 1 text[/h1]
                [h2]Header 2 text[/h2]
                [h3]Header 3 text[/h3]
                [b]Bold text [/b]
                [u]Underlined text [/u]
                [i]Italic text [/i]
                [strike]Strikethrough text[/strike]
                [spoiler]Spoiler text[/spoiler]
                [noparse]Doesn't parse [b]tags[/b][/noparse]
                [hr][/hr]
                [url=store.steampowered.com] Website link [/url]
                https://www.youtube.com/watch?v=tax4e4hBBZc
                [quote=author]Quoted text[/quote]
                [code]Fixed-width font, preserves spaces[/code]
                Some ːsteamhappyː for ːsteamsadː testing.
                Hello World! [emoticon]steamhappy[/emoticon]
                    """.trimIndent(),
                    )

                    Spacer(Modifier.height(14.dp))

                    BBCodeText(text = "[sticker type=\"Winter2019JingleIntensifies\" limit=\"0\"][/sticker]")
                }
            }
        }
    }
}