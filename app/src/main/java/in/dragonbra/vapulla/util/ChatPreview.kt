package `in`.dragonbra.vapulla.util

// A sticker, e.g. [sticker type="Sunny Scales" limit="0"][/sticker]
private val STICKER = Regex("""\[sticker\b[^\]]*?type="([^"]*)"[^\]]*]\s*\[/sticker]""")

// [emoticon]steamhappy[/emoticon]
private val EMOTICON = Regex("""\[emoticon]([^\[]*)\[/emoticon]""")

// An innermost tag pair: no other tag between its opening and closing tag.
// Groups: 1 the tag name, 2 its attributes if any, 3 what it wraps.
private val TAG_PAIR = Regex("""\[(\w+)([ =][^\]]*)?]([^\[]*)\[/\1]""")

// Anything tag-shaped that is left over, such as an opening tag without its partner.
private val STRAY_TAG = Regex("""\[/?\w+(?:[ =][^\]]*)?]""")

private val QUOTED_VALUE = Regex("""="([^"]*)"""")
private val BARE_VALUE = Regex("""=([^\s"\]]+)""")
private val WHITESPACE = Regex("""\s+""")

/** Tags whose content is formatting only, so the preview is simply the text they wrap. */
private val TEXT_TAGS =
    setOf("b", "i", "u", "strike", "url", "mention", "noparse", "h1", "h2", "h3")

/**
 * A one-line, plain-text preview of a Steam chat message, for places that do not render
 * BB Code such as the last message of a channel in the group list.
 *
 * Formatting tags give way to the text they wrap. A tag that stands for something other than
 * text is named instead, with its first attribute when it has one and what it wraps otherwise:
 * `[sticker type="Sunny Scales" limit="0"][/sticker]` becomes `Sticker: Sunny Scales`,
 * `[flip result="heads"][/flip]` becomes `Flip: heads`. Emoticons read `:name:`, an image is
 * just `Image`, and a spoiler stays hidden as `Spoiler`.
 */
fun String.toChatPreview(): String {
    if (indexOf('[') == -1 && indexOf('ː') == -1) {
        return trim()
    }

    var text = replace('ː', ':')
    text = STICKER.replace(text) { "Sticker: ${it.groupValues[1]} " }
    text = EMOTICON.replace(text) { ":${it.groupValues[1]}:" }

    // Innermost pairs first, repeated so nested tags unwrap from the inside out. The bound
    // only guards against a pattern that somehow never settles.
    repeat(MAX_NESTING) {
        val unwrapped = TAG_PAIR.replace(text) { match ->
            val (tag, attributes, content) = match.destructured
            when (val name = tag.lowercase()) {
                in TEXT_TAGS -> content.ifBlank { attributeValue(attributes) }

                // An address says nothing in a preview, and a spoiler must not be given away.
                "img" -> "Image "

                "spoiler" -> "Spoiler "

                else -> {
                    // A dice roll or coin flip is about its outcome, not its first attribute.
                    val detail = RESULT_VALUE.find(attributes)?.groupValues?.get(1)
                        ?: attributeValue(attributes).ifBlank { content.trim() }
                    val label = name.replaceFirstChar { it.uppercase() }
                    if (detail.isEmpty()) "$label " else "$label: $detail "
                }
            }
        }
        if (unwrapped == text) {
            return STRAY_TAG.replace(text, "").replace(WHITESPACE, " ").trim()
        }
        text = unwrapped
    }

    return STRAY_TAG.replace(text, "").replace(WHITESPACE, " ").trim()
}

private val RESULT_VALUE = Regex("""\bresult="([^"]*)"""")

private const val MAX_NESTING = 8

/** The value of the first attribute in a tag's attribute text (` type="x" limit="0"`, `=x`). */
private fun attributeValue(attributes: String): String =
    (QUOTED_VALUE.find(attributes) ?: BARE_VALUE.find(attributes))
        ?.groupValues?.get(1)?.trim().orEmpty()
