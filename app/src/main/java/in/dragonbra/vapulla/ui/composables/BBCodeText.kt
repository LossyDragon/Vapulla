package `in`.dragonbra.vapulla.ui.composables

import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Renders Steam BB Code: chat messages and profile summaries.
 *
 * The text is parsed once into a [BBDocument] whose paragraphs are ready-made
 * [AnnotatedString]s, and the result is cached in [BBCode], so a row scrolling back into view
 * costs a cache lookup. A message that is a single paragraph, which is nearly all of chat,
 * is one [Text] with no extra layout around it.
 *
 * @param style Base text style; headers and code blocks derive from it.
 * @param color Text colour, when it should differ from [style] and the content colour.
 */
@Composable
fun BBCodeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val document = remember(text) { BBCode.parse(text) }

    ProvideTextStyle(if (color.isSpecified) style.copy(color = color) else style) {
        val only = document.blocks.singleOrNull()
        if (only is BBBlock.Paragraph) {
            BBParagraph(paragraph = only, modifier = modifier)
        } else {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BBBlocks(document.blocks)
            }
        }
    }
}

@Composable
private fun BBBlocks(blocks: ImmutableList<BBBlock>) {
    blocks.forEach { block ->
        when (block) {
            is BBBlock.Paragraph -> BBParagraph(block)

            is BBBlock.Header -> ProvideTextStyle(
                when (block.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    else -> MaterialTheme.typography.headlineSmall
                }.copy(fontWeight = FontWeight.Bold),
            ) {
                BBParagraph(block.content)
            }

            BBBlock.Rule -> HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            is BBBlock.ItemList -> BBList(block)

            is BBBlock.Quote -> BBQuote(block)

            is BBBlock.Code -> BBCodeBlock(block.content)

            is BBBlock.Image -> BBImage(block)

            is BBBlock.Sticker -> RemoteImage(
                imageModel = { Utils.Constants.STICKER_URL + Uri.encode(block.type) },
                imageOptions = ImageOptions(
                    contentDescription = block.type,
                    contentScale = ContentScale.Fit,
                ),
                modifier = Modifier.size(128.dp),
            )
        }
    }
}

@Composable
private fun BBParagraph(paragraph: BBBlock.Paragraph, modifier: Modifier = Modifier) {
    val inlineContent = if (paragraph.emoticons.isEmpty()) {
        NO_INLINE_CONTENT
    } else {
        remember(paragraph) { emoticonContent(paragraph.emoticons) }
    }

    Text(
        text = if (paragraph.spoilerCount == 0) paragraph.text else spoilerText(paragraph),
        modifier = modifier,
        inlineContent = inlineContent,
    )
}

/**
 * [paragraph] with its spoilers blacked out until tapped. Only paragraphs that contain a
 * spoiler pay for this; everything else shows the string built by the parser as it is.
 */
@Composable
private fun spoilerText(paragraph: BBBlock.Paragraph): AnnotatedString {
    var revealed by remember(paragraph) { mutableStateOf(emptySet<String>()) }

    return remember(paragraph, revealed) {
        buildAnnotatedString {
            append(paragraph.text)

            paragraph.text
                .getStringAnnotations(TAG_SPOILER, 0, paragraph.text.length)
                .forEach { spoiler ->
                    if (spoiler.item !in revealed) {
                        addStyle(HIDDEN_SPOILER_STYLE, spoiler.start, spoiler.end)
                    }
                    addLink(
                        clickable = LinkAnnotation.Clickable(tag = spoiler.item) {
                            revealed = if (spoiler.item in revealed) {
                                revealed - spoiler.item
                            } else {
                                revealed + spoiler.item
                            }
                        },
                        start = spoiler.start,
                        end = spoiler.end,
                    )
                }
        }
    }
}

/** Inline images for the emoticons of one paragraph, sized to follow the text. */
private fun emoticonContent(names: ImmutableList<String>): Map<String, InlineTextContent> =
    names.associate { name ->
        emoticonId(name) to InlineTextContent(
            placeholder = Placeholder(
                width = EMOTICON_SIZE,
                height = EMOTICON_SIZE,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
            ),
        ) {
            RemoteImage(
                imageModel = { Utils.Constants.EMOTICON_URL + name },
                imageOptions = ImageOptions(
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                ),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

@Composable
private fun BBList(list: BBBlock.ItemList) {
    Column(modifier = Modifier.padding(start = 12.dp)) {
        list.items.forEachIndexed { index, item ->
            Row {
                Text(text = if (list.ordered) "${index + 1}. " else "• ")
                BBParagraph(item)
            }
        }
    }
}

@Composable
private fun BBQuote(quote: BBBlock.Quote) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quote.author != null) {
                Text(
                    text = "Originally posted by ${quote.author}:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // The surface sets its own content colour; drop the caller's so it stays legible.
            ProvideTextStyle(
                LocalTextStyle.current.copy(
                    color = Color.Unspecified,
                    fontStyle = FontStyle.Italic,
                ),
            ) {
                BBBlocks(quote.blocks)
            }
        }
    }
}

@Composable
private fun BBCodeBlock(content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

/** An image posted to chat. Shows the thumbnail and opens the full image when tapped. */
@Composable
private fun BBImage(image: BBBlock.Image) {
    val uriHandler = LocalUriHandler.current

    // Steam sends the dimensions, so the bubble has its final size before the image loads
    // and the list does not jump. Without them a fixed-height crop is used.
    val sizeModifier = if (image.aspectRatio != null) {
        Modifier
            .fillMaxWidth()
            .aspectRatio(image.aspectRatio)
    } else {
        Modifier
            .fillMaxWidth()
            .height(180.dp)
    }

    RemoteImage(
        imageModel = { image.thumbnailUrl ?: image.url },
        imageOptions = ImageOptions(
            contentDescription = "Image",
            contentScale = if (image.aspectRatio != null) ContentScale.Fit else ContentScale.Crop,
        ),
        modifier = sizeModifier
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                try {
                    uriHandler.openUri(image.url)
                } catch (e: Exception) {
                    Timber.e(e, "Could not open image")
                }
            },
        failure = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.BrokenImage, contentDescription = null)
            }
        },
    )
}

// region Model

/** A parsed BB Code text: block-level elements, top to bottom. */
@Immutable
class BBDocument(val blocks: ImmutableList<BBBlock>)

@Immutable
sealed interface BBBlock {
    /**
     * A run of inline text, already styled. Links are [LinkAnnotation]s, spoilers are string
     * annotations tagged for [spoilerText], and [emoticons] names the inline images in it.
     */
    data class Paragraph(
        val text: AnnotatedString,
        val emoticons: ImmutableList<String> = persistentListOf(),
        val spoilerCount: Int = 0,
    ) : BBBlock

    data class Header(val level: Int, val content: Paragraph) : BBBlock

    data object Rule : BBBlock

    data class ItemList(val items: ImmutableList<Paragraph>, val ordered: Boolean) : BBBlock

    data class Quote(val author: String?, val blocks: ImmutableList<BBBlock>) : BBBlock

    data class Code(val content: String) : BBBlock

    /** [aspectRatio] is width over height, when Steam sent the dimensions. */
    data class Image(val url: String, val thumbnailUrl: String?, val aspectRatio: Float?) :
        BBBlock

    data class Sticker(val type: String) : BBBlock
}

// endregion

// region Parser

/**
 * Parses and caches Steam BB Code. Parsing is a pure function of the text and safe on any
 * thread, so callers that know what is about to be shown (the chat pager) can warm the cache
 * off the main thread with [parse]; the composable then only looks the result up.
 */
object BBCode {
    private const val CACHE_SIZE = 512

    private val cache = LruCache<String, BBDocument>(CACHE_SIZE)

    /** False for plain text, which needs neither parsing nor a cache entry. */
    fun hasMarkup(text: String): Boolean = text.indexOf('[') != -1 ||
        text.indexOf(EMOTICON_DELIMITER) != -1 ||
        text.contains("http")

    fun parse(text: String): BBDocument {
        if (!hasMarkup(text)) {
            return plain(text)
        }

        cache.get(text)?.let { return it }

        val blocks = parseBlocks(text, 0, text.length, depth = 0)
        // Markup that came to nothing (say, only an unknown empty tag) still shows as typed.
        val document = if (blocks.isEmpty()) plain(text) else BBDocument(blocks)

        cache.put(text, document)
        return document
    }

    private fun plain(text: String) =
        BBDocument(persistentListOf(BBBlock.Paragraph(AnnotatedString(text))))
}

private const val TAG_SPOILER = "spoiler"

// U+02D0, the delimiter Steam uses for emoticons (ːsteamhappyː)
private const val EMOTICON_DELIMITER = 'ː'

private const val TRAILING_PUNCTUATION = ".,;:!?)"

/** Quotes nested deeper than this are shown as text instead of being parsed further. */
private const val MAX_QUOTE_DEPTH = 4

private val EMOTICON_SIZE = 1.4.em

private val NO_INLINE_CONTENT = emptyMap<String, InlineTextContent>()

private val LINK_STYLES = TextLinkStyles(
    style = SpanStyle(color = Color(0xFF4A90E2), textDecoration = TextDecoration.Underline),
)

private val HIDDEN_SPOILER_STYLE = SpanStyle(color = Color.Black, background = Color.Black)

private fun emoticonId(name: String) = "emoticon_$name"

/** Inline formatting switches. Unset ones add nothing, so the surrounding style shows through. */
private data class BBStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
) {
    val isPlain: Boolean
        get() = !bold && !italic && !underline && !strikethrough

    fun toSpanStyle(): SpanStyle = SpanStyle(
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else null,
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

/**
 * A `[name ...]content[/name]` pair. [attributes] is whatever sits between the name and the
 * closing bracket of the opening tag, e.g. `=author` or ` src=... width=10`.
 */
private class Tag(val attributes: String, val contentStart: Int, val contentEnd: Int, val end: Int)

private class BlockMatch(val block: BBBlock, val end: Int)

/**
 * Matches the tag [name] opening at the `[` at [pos], together with its closing tag, inside
 * `[pos, limit)`. Nested tags of the same name are paired up, so a quote inside a quote ends
 * at the right place. Null when it is some other tag or is never closed.
 */
private fun matchTag(text: String, pos: Int, name: String, limit: Int): Tag? {
    val afterName = pos + 1 + name.length
    if (afterName >= limit || !text.startsWith(name, pos + 1) || !isTagEnd(text[afterName])) {
        return null
    }

    val openEnd = text.indexOf(']', afterName)
    if (openEnd == -1 || openEnd >= limit) {
        return null
    }

    val closeTag = "[/$name]"
    var depth = 1
    var scan = openEnd + 1

    while (true) {
        val close = text.indexOf(closeTag, scan)
        if (close == -1 || close + closeTag.length > limit) {
            return null
        }

        val nested = indexOfOpenTag(text, name, scan, close)
        if (nested != -1) {
            depth++
            scan = nested + 1 + name.length
        } else {
            depth--
            if (depth == 0) {
                return Tag(
                    attributes = text.substring(afterName, openEnd),
                    contentStart = openEnd + 1,
                    contentEnd = close,
                    end = close + closeTag.length,
                )
            }
            scan = close + closeTag.length
        }
    }
}

private fun isTagEnd(c: Char) = c == ']' || c == '=' || c == ' '

/** First opening tag [name] in `[from, before)`, or -1. */
private fun indexOfOpenTag(text: String, name: String, from: Int, before: Int): Int {
    var index = from
    while (true) {
        index = text.indexOf('[', index)
        if (index == -1 || index >= before) {
            return -1
        }
        val afterName = index + 1 + name.length
        if (afterName < before && text.startsWith(name, index + 1) && isTagEnd(text[afterName])) {
            return index
        }
        index++
    }
}

/** Reads `key=value key2="value 2"` pairs out of a tag's attributes. */
private fun parseAttributes(attributes: String): Map<String, String> {
    val result = HashMap<String, String>()
    var i = 0

    while (i < attributes.length) {
        val equals = attributes.indexOf('=', i)
        if (equals == -1) {
            break
        }

        val key = attributes.substring(i, equals).trim()
        i = equals + 1

        val value: String
        if (i < attributes.length && attributes[i] == '"') {
            val quoteEnd = attributes.indexOf('"', i + 1)
            val close = if (quoteEnd == -1) attributes.length else quoteEnd
            value = attributes.substring(i + 1, close)
            i = close + 1
        } else {
            var valueEnd = i
            while (valueEnd < attributes.length && !attributes[valueEnd].isWhitespace()) {
                valueEnd++
            }
            value = attributes.substring(i, valueEnd)
            i = valueEnd
        }

        result[key] = value
    }

    return result
}

private fun isHttpUrl(text: String, index: Int = 0): Boolean =
    text.startsWith("http://", index) || text.startsWith("https://", index)

/**
 * The address a link may open, or null if it should stay plain text. Messages come from
 * other people, so only web links are followed: a bare host gets `https://`, and any other
 * scheme (`steam://`, `intent://`, ...) is refused.
 */
private fun linkTarget(raw: String): String? {
    val url = raw.trim()
    return when {
        url.isEmpty() -> null
        isHttpUrl(url) -> url
        url.contains("://") -> null
        else -> "https://$url"
    }
}

/**
 * Parses `[start, end)` of [input] into block-level elements.
 *
 * Single forward pass — the cursor always advances, so malformed input (unclosed tags, a
 * bare `hr` tag, etc.) degrades to literal text instead of dropping content or looping.
 */
private fun parseBlocks(input: String, start: Int, end: Int, depth: Int): ImmutableList<BBBlock> {
    val blocks = mutableListOf<BBBlock>()
    var inlineStart = start
    var pos = start

    fun flushInlineUpTo(upTo: Int) {
        parseInline(input, inlineStart, upTo)?.let(blocks::add)
    }

    while (pos < end) {
        val bracket = input.indexOf('[', pos)
        if (bracket == -1 || bracket >= end) {
            break
        }

        // Tags inside [noparse] are text, so do not look for blocks in there.
        val noparse = matchTag(input, bracket, "noparse", end)
        if (noparse != null) {
            pos = noparse.end
            continue
        }

        val match = parseBlock(input, bracket, end, depth)
        if (match != null) {
            flushInlineUpTo(bracket)
            blocks.add(match.block)
            inlineStart = match.end
            pos = match.end
        } else {
            // Not a block tag here; leave it for the inline parser.
            pos = bracket + 1
        }
    }

    flushInlineUpTo(end)

    return blocks.toImmutableList()
}

/** The block-level tag opening at the `[` at [pos], if there is one. */
private fun parseBlock(text: String, pos: Int, limit: Int, depth: Int): BlockMatch? {
    if (pos + 1 >= limit) {
        return null
    }

    // Dispatch on the first letter so ordinary inline tags cost one comparison.
    return when (text[pos + 1]) {
        'h' -> parseRule(text, pos, limit)
            ?: parseHeader(text, pos, limit, "h1", 1)
            ?: parseHeader(text, pos, limit, "h2", 2)
            ?: parseHeader(text, pos, limit, "h3", 3)

        'l' -> matchTag(text, pos, "list", limit)?.let { tag ->
            BlockMatch(BBBlock.ItemList(parseListItems(text, tag), ordered = false), tag.end)
        }

        'o' -> matchTag(text, pos, "olist", limit)?.let { tag ->
            BlockMatch(BBBlock.ItemList(parseListItems(text, tag), ordered = true), tag.end)
        }

        'q' -> matchTag(text, pos, "quote", limit)?.let { tag ->
            // Steam writes the author as "=name" or "=name;postid".
            val author = tag.attributes
                .removePrefix("=")
                .substringBefore(';')
                .trim()
                .trim('"')
                .ifEmpty { null }

            val content = if (depth < MAX_QUOTE_DEPTH) {
                parseBlocks(text, tag.contentStart, tag.contentEnd, depth + 1)
            } else {
                listOfNotNull(parseInline(text, tag.contentStart, tag.contentEnd))
                    .toImmutableList()
            }

            BlockMatch(BBBlock.Quote(author, content), tag.end)
        }

        'c' -> parseCode(text, pos, limit, "code")

        'p' -> parseCode(text, pos, limit, "pre")

        'i' -> matchTag(text, pos, "img", limit)?.let { tag ->
            parseImage(text, tag)?.let { BlockMatch(it, tag.end) }
        }

        's' -> matchTag(text, pos, "sticker", limit)?.let { tag ->
            parseAttributes(tag.attributes)["type"]
                ?.takeIf { it.isNotEmpty() }
                ?.let { BlockMatch(BBBlock.Sticker(it), tag.end) }
        }

        else -> null
    }
}

/** Accepts both `[hr][/hr]` and a bare `[hr]`. */
private fun parseRule(text: String, pos: Int, limit: Int): BlockMatch? {
    if (!text.startsWith("[hr]", pos) || pos + 4 > limit) {
        return null
    }
    val end = if (text.startsWith("[/hr]", pos + 4) && pos + 9 <= limit) pos + 9 else pos + 4
    return BlockMatch(BBBlock.Rule, end)
}

private fun parseHeader(text: String, pos: Int, limit: Int, name: String, level: Int): BlockMatch? {
    val tag = matchTag(text, pos, name, limit) ?: return null
    val content = parseInline(text, tag.contentStart, tag.contentEnd, trimSpaces = true)
        ?: return null
    return BlockMatch(BBBlock.Header(level, content), tag.end)
}

private fun parseCode(text: String, pos: Int, limit: Int, name: String): BlockMatch? {
    val tag = matchTag(text, pos, name, limit) ?: return null
    val content = text.substring(tag.contentStart, tag.contentEnd).trim('\n', '\r')
    return BlockMatch(BBBlock.Code(content), tag.end)
}

private fun parseListItems(text: String, tag: Tag): ImmutableList<BBBlock.Paragraph> {
    val items = mutableListOf<BBBlock.Paragraph>()
    var itemStart = tag.contentStart

    while (true) {
        val marker = text.indexOf("[*]", itemStart)
        val itemEnd = if (marker == -1 || marker >= tag.contentEnd) tag.contentEnd else marker

        parseInline(text, itemStart, itemEnd, trimSpaces = true)?.let(items::add)

        if (itemEnd == tag.contentEnd) {
            break
        }
        itemStart = itemEnd + 3
    }

    return items.toImmutableList()
}

/**
 * Steam chat sends `[img src=URL thumbnail_src=URL width=N height=N]...[/img]`; the classic
 * form is `[img]URL[/img]`. Anything that is not a web image is left as text.
 */
private fun parseImage(text: String, tag: Tag): BBBlock.Image? {
    val attributes = parseAttributes(tag.attributes)

    val url = attributes["src"]
        ?: text.substring(tag.contentStart, tag.contentEnd).trim()

    if (!isHttpUrl(url)) {
        return null
    }

    val width = attributes["width"]?.toFloatOrNull()
    val height = attributes["height"]?.toFloatOrNull()

    return BBBlock.Image(
        url = url,
        thumbnailUrl = attributes["thumbnail_src"]?.takeIf(::isHttpUrl),
        aspectRatio = if (width != null && height != null && width > 0f && height > 0f) {
            // Keep a freak ratio from producing a sliver or a tower.
            (width / height).coerceIn(0.25f, 4f)
        } else {
            null
        },
    )
}

/**
 * Parses the inline BB Code in `[rangeStart, rangeEnd)` of [text] into one styled paragraph.
 * Line breaks at either edge are dropped, as they only separate the paragraph from the
 * blocks around it; with [trimSpaces] all surrounding whitespace is. Null if nothing is left.
 */
private fun parseInline(
    text: String,
    rangeStart: Int,
    rangeEnd: Int,
    trimSpaces: Boolean = false,
): BBBlock.Paragraph? {
    fun isTrimmed(c: Char) = if (trimSpaces) c.isWhitespace() else c == '\n' || c == '\r'

    var start = rangeStart
    var end = rangeEnd
    while (start < end && isTrimmed(text[start])) start++
    while (end > start && isTrimmed(text[end - 1])) end--

    var hasContent = false
    for (i in start until end) {
        if (!text[i].isWhitespace()) {
            hasContent = true
            break
        }
    }
    if (!hasContent) {
        return null
    }

    val out = AnnotatedString.Builder(end - start)
    val buffer = StringBuilder()
    val emoticons = mutableListOf<String>()
    var style = BBStyle()
    var spoilerCount = 0
    var pos = start

    fun flushBuffer() {
        if (buffer.isNotEmpty()) {
            if (style.isPlain) {
                out.append(buffer)
            } else {
                out.withStyle(style.toSpanStyle()) { append(buffer) }
            }
            buffer.clear()
        }
    }

    // Buffered text was appended under the old style, so flush before switching.
    fun updateStyle(newStyle: BBStyle) {
        flushBuffer()
        style = newStyle
    }

    fun appendLink(target: String, label: String) {
        flushBuffer()
        val url = linkTarget(target)
        if (url == null) {
            out.append(label)
        } else {
            out.withLink(LinkAnnotation.Url(url, LINK_STYLES)) { append(label) }
        }
    }

    fun appendEmoticon(name: String) {
        flushBuffer()
        if (name !in emoticons) {
            emoticons.add(name)
        }
        out.appendInlineContent(emoticonId(name), ":$name:")
    }

    fun appendStyled(spanStyle: SpanStyle, content: String) {
        flushBuffer()
        out.withStyle(spanStyle) { append(content) }
    }

    /**
     * Handles the paired inline tag opening at [at]. Returns the index just past it, or -1
     * if it is not a tag known here, in which case the bracket is kept as text.
     */
    fun parsePairedTag(at: Int): Int {
        if (at + 1 >= end) {
            return -1
        }

        when (text[at + 1]) {
            'u' -> matchTag(text, at, "url", end)?.let { tag ->
                val label = text.substring(tag.contentStart, tag.contentEnd)
                // [url=target]label[/url], or [url]target[/url]
                val target = if (tag.attributes.startsWith("=")) {
                    tag.attributes.substring(1).trim().trim('"')
                } else {
                    label
                }
                appendLink(target, label)
                return tag.end
            }

            's' -> matchTag(text, at, "spoiler", end)?.let { tag ->
                flushBuffer()
                out.pushStringAnnotation(TAG_SPOILER, "spoiler_${spoilerCount++}")
                out.append(text, tag.contentStart, tag.contentEnd)
                out.pop()
                return tag.end
            }

            'n' -> matchTag(text, at, "noparse", end)?.let { tag ->
                buffer.append(text, tag.contentStart, tag.contentEnd)
                return tag.end
            }

            'e' -> matchTag(text, at, "emoticon", end)?.let { tag ->
                appendEmoticon(text.substring(tag.contentStart, tag.contentEnd))
                return tag.end
            }

            // [mention=accountid]@Name[/mention]
            'm' -> matchTag(text, at, "mention", end)?.let { tag ->
                appendStyled(
                    SpanStyle(fontWeight = FontWeight.Bold),
                    text.substring(tag.contentStart, tag.contentEnd),
                )
                return tag.end
            }

            // The /random and /flip chat commands
            'r' -> matchTag(text, at, "random", end)?.let { tag ->
                val attributes = parseAttributes(tag.attributes)
                val result = attributes["result"] ?: return -1
                val range = "${attributes["min"] ?: "?"}-${attributes["max"] ?: "?"}"
                appendStyled(SpanStyle(fontStyle = FontStyle.Italic), "rolled $result ($range)")
                return tag.end
            }

            'f' -> matchTag(text, at, "flip", end)?.let { tag ->
                val result = parseAttributes(tag.attributes)["result"] ?: return -1
                appendStyled(SpanStyle(fontStyle = FontStyle.Italic), "flipped a coin: $result")
                return tag.end
            }
        }

        return -1
    }

    while (pos < end) {
        val c = text[pos]

        // Bare http(s):// URLs become clickable links.
        if (c == 'h' && isHttpUrl(text, pos)) {
            var urlEnd = pos
            while (urlEnd < end && !isUrlTerminator(text[urlEnd])) {
                urlEnd++
            }
            // Don't swallow sentence punctuation trailing the URL.
            while (urlEnd > pos && text[urlEnd - 1] in TRAILING_PUNCTUATION) {
                urlEnd--
            }
            val url = text.substring(pos, urlEnd)
            appendLink(url, url)
            pos = urlEnd
            continue
        }

        // Fast path: copy the run of ordinary characters in one append.
        if (c != '[' && c != EMOTICON_DELIMITER) {
            var next = pos + 1
            while (next < end &&
                text[next] != '[' &&
                text[next] != EMOTICON_DELIMITER &&
                !(text[next] == 'h' && isHttpUrl(text, next))
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
                appendEmoticon(text.substring(pos + 1, closeIndex))
                pos = closeIndex + 1
            } else {
                buffer.append(c)
                pos++
            }
            continue
        }

        // c == '[' — style switches first, then paired tags; unknown tags stay literal text.
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

            else -> {
                val tagEnd = parsePairedTag(pos)
                if (tagEnd != -1) {
                    pos = tagEnd
                } else {
                    buffer.append(c)
                    pos++
                }
            }
        }
    }

    flushBuffer()

    return BBBlock.Paragraph(
        text = out.toAnnotatedString(),
        emoticons = emoticons.toImmutableList(),
        spoilerCount = spoilerCount,
    )
}

private fun isUrlTerminator(c: Char): Boolean =
    c.isWhitespace() || c == '[' || c == EMOTICON_DELIMITER || c == '"'

// endregion

@Preview
@Composable
private fun Preview_BBCodeText() {
    VapullaTheme {
        Surface {
            Column {
                Text("The BB Code Text")
                BBCodeText(
                    text = """
            [h1]Header 1 text[/h1]
            [h2]Header 2 [i]text[/i][/h2]
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
            [list][*]First [b]item[/b][*]Second item[/list]
            [quote=author]Quoted [b]text[/b][quote]and a nested one[/quote][/quote]
            [code]Fixed-width font, preserves spaces[/code]
            Some ːsteamhappyː for ːsteamsadː testing.
            Hello World! [emoticon]steamhappy[/emoticon]
            [random min="1" max="100" result="42"][/random]
                    """.trimIndent(),
                )

                Spacer(Modifier.height(14.dp))

                BBCodeText(
                    text = "[sticker type=\"Winter2019JingleIntensifies\" limit=\"0\"][/sticker]",
                )

                BBCodeText(
                    text = "[img src=https://example.com/a.png thumbnail_src=" +
                        "https://example.com/t.png width=735 height=561][/img]",
                )
            }
        }
    }
}
