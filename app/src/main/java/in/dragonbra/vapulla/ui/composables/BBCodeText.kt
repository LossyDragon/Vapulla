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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.decoders.AnimatedPngDecoder
import kotlinx.collections.immutable.ImmutableList
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
    onUrlClick: ((String) -> Unit)? = null,
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
private fun BBCodeElement(element: BBElement, onUrlClick: ((String) -> Unit)?) {
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
                fontWeight = FontWeight.Bold,
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
private fun BBCodeTextElement(element: BBElement.Text, onUrlClick: ((String) -> Unit)?) {
    val uriHandler = LocalUriHandler.current
    val revealedSpoilers = remember(element) { mutableStateMapOf<String, Boolean>() }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    val inlineContent = remember(element) { buildInlineContentMap(element.segments) }

    // Most chat messages have no links or spoilers; skip tap handling entirely for those.
    val isInteractive = remember(element) {
        element.segments.any { it is TextSegment.Link || it is TextSegment.Spoiler }
    }

    // derivedStateOf tracks reads of revealedSpoilers, so the string is rebuilt
    // only when a spoiler is toggled, not on every recomposition.
    val annotatedString by remember(element) {
        derivedStateOf {
            buildAnnotatedString { appendStyledText(element, revealedSpoilers) }
        }
    }

    val tapModifier = if (isInteractive) {
        // Keyed on element (not annotatedString) so spoiler toggles don't cancel
        // the gesture detector; annotation ranges are unaffected by the toggle.
        Modifier.pointerInput(element, onUrlClick) {
            detectTapGestures { offset ->
                val layout = layoutResult.value ?: return@detectTapGestures
                val position = layout.getOffsetForPosition(offset)

                // Check for URL click first
                annotatedString
                    .getStringAnnotations(TAG_URL, position, position)
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
                    .getStringAnnotations(TAG_SPOILER, position, position)
                    .firstOrNull()
                    ?.let { annotation ->
                        revealedSpoilers[annotation.item] =
                            !(revealedSpoilers[annotation.item] ?: false)
                    }
            }
        }
    } else {
        Modifier
    }

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        modifier = tapModifier,
        onTextLayout = { layoutResult.value = it },
    )
}

@Composable
private fun BBCodeList(list: BBElement.BBList) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)) {
        list.items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = if (list.ordered) "${index + 1}. " else "• ",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
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
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (quote.author != null) {
                Text(
                    text = "Originally posted by ${quote.author}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = quote.content,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
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
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = content,
            modifier = Modifier.padding(12.dp),
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFD4D4D4),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun buildInlineContentMap(
    segments: ImmutableList<TextSegment>,
): Map<String, InlineTextContent> = buildMap {
    segments.forEach { segment ->
        when (segment) {
            is TextSegment.Emoticon -> {
                put(
                    segment.id,
                    InlineTextContent(
                        placeholder = Placeholder(
                            width = 18.sp,
                            height = 18.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        CoilImage(
                            imageModel = { Utils.Constants.EMOTICON_URL + segment.name },
                            imageOptions = ImageOptions(
                                contentDescription = segment.name,
                                contentScale = ContentScale.Fit,
                            ),
                            modifier = Modifier.size(18.dp),
                            loading = {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp))
                            },
                            failure = {
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "Failed to load emoticon",
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    },
                )
            }

            is TextSegment.Sticker -> {
                put(
                    segment.id,
                    InlineTextContent(
                        placeholder = Placeholder(
                            width = 150.sp,
                            height = 150.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                    ) {
                        CoilImage(
                            imageModel = { Utils.Constants.STICKER_URL + segment.type },
                            imageOptions = ImageOptions(
                                contentDescription = segment.type,
                                contentScale = ContentScale.Fit,
                            ),
                            modifier = Modifier.size(150.dp),
                            loading = {
                                CircularProgressIndicator(modifier = Modifier.size(50.dp))
                            },
                            failure = {
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "Failed to load sticker",
                                    modifier = Modifier.size(150.dp),
                                )
                            },
                        )
                    },
                )
            }

            else -> {
                /* No inline content needed for other segment types */
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyledText(
    element: BBElement.Text,
    revealedSpoilers: Map<String, Boolean>,
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
                        color = LINK_COLOR,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) {
                    pushStringAnnotation(tag = TAG_URL, annotation = segment.url)
                    append(segment.text)
                    pop()
                }
            }

            is TextSegment.Spoiler -> {
                val isRevealed = revealedSpoilers[segment.id] ?: false

                pushStringAnnotation(TAG_SPOILER, segment.id)
                withStyle(
                    SpanStyle(
                        background = if (isRevealed) Color.Unspecified else Color.Black,
                        color = if (isRevealed) Color.Unspecified else Color.Black,
                    ),
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
    data class Spoiler(val text: String, val id: String) : TextSegment()
    data class Emoticon(val name: String, val id: String) : TextSegment()
    data class Sticker(val type: String, val id: String) : TextSegment()
}

@Immutable
data class BBStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
) {
    val isPlain: Boolean
        get() = !bold && !italic && !underline && !strikethrough

    fun toSpanStyle(): SpanStyle = SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            underline && strikethrough -> TextDecoration.combine(
                listOf(TextDecoration.Underline, TextDecoration.LineThrough),
            )

            underline -> TextDecoration.Underline

            strikethrough -> TextDecoration.LineThrough

            else -> null
        },
    )
}

private const val TAG_URL = "URL"
private const val TAG_SPOILER = "spoiler"
private val LINK_COLOR = Color(0xFF4A90E2)

// U+02D0, the delimiter Steam uses for emoticons (ːsteamhappyː)
private const val EMOTICON_DELIMITER = 'ː'

// Compiled once; matching is anchored with matchAt() during parsing.
@Suppress("RegExpRedundantEscape")
private val STICKER_REGEX = Regex("""\[sticker type="([^"]+)"[^\]]*\]\[/sticker\]""")

/**
 * Parses Steam BB Code into a list of block-level elements.
 *
 * Single forward pass over the input — the cursor always advances, so malformed
 * input (unclosed tags, a bare `hr` tag, etc.) degrades to literal text instead
 * of dropping content or looping.
 */
private fun parseSteamBBCode(input: String): ImmutableList<BBElement> {
    val elements = mutableListOf<BBElement>()
    var inlineStart = 0
    var pos = 0

    fun flushInlineUpTo(end: Int) {
        if (end > inlineStart) {
            val segments = parseInlineText(input, inlineStart, end)
            if (segments.isNotEmpty()) {
                elements.add(BBElement.Text(segments))
            }
        }
    }

    while (pos < input.length) {
        val bracket = input.indexOf('[', pos)
        if (bracket == -1) {
            break
        }

        val block = tryParseBlock(input, bracket)
        if (block != null) {
            flushInlineUpTo(bracket)
            elements.add(block.element)
            inlineStart = block.endIndex
            pos = block.endIndex
        } else {
            // Not a valid block tag here; leave it for the inline parser.
            pos = bracket + 1
        }
    }

    flushInlineUpTo(input.length)

    return elements.toImmutableList()
}

private class BlockMatch(val element: BBElement, val endIndex: Int)

private fun tryParseBlock(text: String, pos: Int): BlockMatch? = when {
    text.startsWith("[h1]", pos) -> extractTagBlock(text, pos, "[h1]", "[/h1]") {
        BBElement.Header(1, it)
    }

    text.startsWith("[h2]", pos) -> extractTagBlock(text, pos, "[h2]", "[/h2]") {
        BBElement.Header(2, it)
    }

    text.startsWith("[h3]", pos) -> extractTagBlock(text, pos, "[h3]", "[/h3]") {
        BBElement.Header(3, it)
    }

    text.startsWith("[hr]", pos) -> {
        // Accept both "[hr][/hr]" and a bare "[hr]"
        val end = if (text.startsWith("[hr][/hr]", pos)) pos + 9 else pos + 4
        BlockMatch(BBElement.HorizontalRule, end)
    }

    text.startsWith("[list]", pos) -> extractTagBlock(text, pos, "[list]", "[/list]") {
        BBElement.BBList(splitListItems(it), ordered = false)
    }

    text.startsWith("[olist]", pos) -> extractTagBlock(text, pos, "[olist]", "[/olist]") {
        BBElement.BBList(splitListItems(it), ordered = true)
    }

    text.startsWith("[quote", pos) -> extractQuote(text, pos)

    text.startsWith("[code]", pos) -> extractTagBlock(text, pos, "[code]", "[/code]") {
        BBElement.Code(it)
    }

    else -> null
}

private inline fun extractTagBlock(
    text: String,
    pos: Int,
    openTag: String,
    closeTag: String,
    build: (String) -> BBElement,
): BlockMatch? {
    val contentStart = pos + openTag.length
    val closeIndex = text.indexOf(closeTag, contentStart)
    if (closeIndex == -1) {
        return null
    }

    val content = text.substring(contentStart, closeIndex)
    return BlockMatch(build(content), closeIndex + closeTag.length)
}

private fun splitListItems(content: String): ImmutableList<String> = content
    .split("[*]")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toImmutableList()

private const val TRAILING_PUNCTUATION = ".,;:!?)"

private fun isUrlStart(text: String, index: Int): Boolean =
    text.startsWith("http://", index) || text.startsWith("https://", index)

private fun isUrlTerminator(c: Char): Boolean =
    c.isWhitespace() || c == '[' || c == EMOTICON_DELIMITER || c == '"'

/**
 * Parses a quote tag, with or without an author attribute, anchored at [pos].
 */
private fun extractQuote(text: String, pos: Int): BlockMatch? {
    val author: String?
    val contentStart: Int

    when {
        text.startsWith("[quote]", pos) -> {
            author = null
            contentStart = pos + 7
        }

        text.startsWith("[quote=", pos) -> {
            val bracketEnd = text.indexOf(']', pos + 7)
            if (bracketEnd == -1) {
                return null
            }
            author = text.substring(pos + 7, bracketEnd)
            contentStart = bracketEnd + 1
        }

        else -> return null
    }

    val closeIndex = text.indexOf("[/quote]", contentStart)
    if (closeIndex == -1) {
        return null
    }

    val content = text.substring(contentStart, closeIndex)
    return BlockMatch(BBElement.Quote(content, author), closeIndex + 8)
}

/**
 * Parses inline BB Code within [start, end) of [text] into segments.
 * Consecutive same-styled characters are coalesced into a single segment.
 */
private fun parseInlineText(text: String, start: Int, end: Int): ImmutableList<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    val buffer = StringBuilder()
    var style = BBStyle()
    var spoilerCount = 0
    var pos = start

    fun flushBuffer() {
        if (buffer.isNotEmpty()) {
            val run = buffer.toString()
            segments.add(
                if (style.isPlain) TextSegment.Plain(run) else TextSegment.Styled(run, style),
            )
            buffer.clear()
        }
    }

    // Buffered text was appended under the old style, so flush before switching.
    fun updateStyle(newStyle: BBStyle) {
        flushBuffer()
        style = newStyle
    }

    // Returns the index of `target` if it fits entirely inside [pos, end), else -1.
    fun indexOfWithin(target: String, from: Int): Int {
        val index = text.indexOf(target, from)
        return if (index != -1 && index + target.length <= end) index else -1
    }

    while (pos < end) {
        val c = text[pos]

        // Bare http(s):// URLs become clickable links.
        if (c == 'h' && isUrlStart(text, pos)) {
            var urlEnd = pos
            while (urlEnd < end && !isUrlTerminator(text[urlEnd])) {
                urlEnd++
            }
            // Don't swallow sentence punctuation trailing the URL.
            while (urlEnd > pos && text[urlEnd - 1] in TRAILING_PUNCTUATION) {
                urlEnd--
            }
            flushBuffer()
            val url = text.substring(pos, urlEnd)
            segments.add(TextSegment.Link(url, url))
            pos = urlEnd
            continue
        }

        // Fast path: copy the run of ordinary characters in one append.
        if (c != '[' && c != EMOTICON_DELIMITER) {
            var next = pos + 1
            while (next < end &&
                text[next] != '[' &&
                text[next] != EMOTICON_DELIMITER &&
                !(text[next] == 'h' && isUrlStart(text, next))
            ) {
                next++
            }
            buffer.append(text, pos, next)
            pos = next
            continue
        }

        // Emoticons with colon format: ːemoticonː
        if (c == EMOTICON_DELIMITER) {
            val closeIndex = text.indexOf(EMOTICON_DELIMITER, pos + 1)
            if (closeIndex != -1 && closeIndex < end) {
                flushBuffer()
                val name = text.substring(pos + 1, closeIndex)
                segments.add(TextSegment.Emoticon(name, "emoticon_$name"))
                pos = closeIndex + 1
            } else {
                buffer.append(c)
                pos++
            }
            continue
        }

        // c == '[' — try each inline tag; unknown tags fall through as literal text.
        when {
            text.startsWith("[b]", pos) -> {
                updateStyle(style.copy(bold = true))
                pos += 3
            }

            text.startsWith("[/b]", pos) -> {
                updateStyle(style.copy(bold = false))
                pos += 4
            }

            text.startsWith("[i]", pos) -> {
                updateStyle(style.copy(italic = true))
                pos += 3
            }

            text.startsWith("[/i]", pos) -> {
                updateStyle(style.copy(italic = false))
                pos += 4
            }

            text.startsWith("[u]", pos) -> {
                updateStyle(style.copy(underline = true))
                pos += 3
            }

            text.startsWith("[/u]", pos) -> {
                updateStyle(style.copy(underline = false))
                pos += 4
            }

            text.startsWith("[strike]", pos) -> {
                updateStyle(style.copy(strikethrough = true))
                pos += 8
            }

            text.startsWith("[/strike]", pos) -> {
                updateStyle(style.copy(strikethrough = false))
                pos += 9
            }

            text.startsWith("[spoiler]", pos) -> {
                val closeIndex = indexOfWithin("[/spoiler]", pos + 9)
                if (closeIndex != -1) {
                    flushBuffer()
                    val spoilerText = text.substring(pos + 9, closeIndex)
                    segments.add(TextSegment.Spoiler(spoilerText, "spoiler_${spoilerCount++}"))
                    pos = closeIndex + 10
                } else {
                    buffer.append(c)
                    pos++
                }
            }

            text.startsWith("[url=", pos) -> {
                val urlEnd = indexOfWithin("]", pos + 5)
                val closeIndex = if (urlEnd != -1) indexOfWithin("[/url]", urlEnd + 1) else -1
                if (closeIndex != -1) {
                    flushBuffer()
                    val url = text.substring(pos + 5, urlEnd)
                    val linkText = text.substring(urlEnd + 1, closeIndex)
                    segments.add(TextSegment.Link(linkText, url))
                    pos = closeIndex + 6
                } else {
                    buffer.append(c)
                    pos++
                }
            }

            text.startsWith("[noparse]", pos) -> {
                val closeIndex = indexOfWithin("[/noparse]", pos + 9)
                if (closeIndex != -1) {
                    buffer.append(text, pos + 9, closeIndex)
                    pos = closeIndex + 10
                } else {
                    buffer.append(c)
                    pos++
                }
            }

            // Emoticons with tag format: [emoticon]name[/emoticon]
            text.startsWith("[emoticon]", pos) -> {
                val closeIndex = indexOfWithin("[/emoticon]", pos + 10)
                if (closeIndex != -1) {
                    flushBuffer()
                    val name = text.substring(pos + 10, closeIndex)
                    segments.add(TextSegment.Emoticon(name, "emoticon_$name"))
                    pos = closeIndex + 11
                } else {
                    buffer.append(c)
                    pos++
                }
            }

            // Stickers: [sticker type="name"][/sticker]
            text.startsWith("[sticker", pos) -> {
                val match = STICKER_REGEX.matchAt(text, pos)
                if (match != null && match.range.last < end) {
                    flushBuffer()
                    val type = match.groupValues[1]
                    segments.add(TextSegment.Sticker(type, "sticker_$type"))
                    pos = match.range.last + 1
                } else {
                    buffer.append(c)
                    pos++
                }
            }

            else -> {
                buffer.append(c)
                pos++
            }
        }
    }

    flushBuffer()

    return segments.toImmutableList()
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

                    BBCodeText(
                        text = "[sticker type=\"Winter2019JingleIntensifies\" limit=\"0\"][/sticker]",
                    )
                }
            }
        }
    }
}
