package `in`.dragonbra.vapulla.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** [toChatPreview] turns a chat message with Steam's BB Code into one plain line. */
class ChatPreviewTest {

    @Test
    fun `plain text is only trimmed`() {
        assertEquals("hello there", "  hello there  ".toChatPreview())
    }

    @Test
    fun `formatting tags give way to their text`() {
        assertEquals("bold and italic", "[b]bold[/b] and [i]italic[/i]".toChatPreview())
    }

    @Test
    fun `nested tags unwrap from the inside out`() {
        assertEquals("deep", "[b][i][u]deep[/u][/i][/b]".toChatPreview())
    }

    @Test
    fun `a link shows its label, or its address when it has none`() {
        assertEquals("the store", "[url=https://a.example]the store[/url]".toChatPreview())
        assertEquals("https://a.example", "[url=https://a.example][/url]".toChatPreview())
    }

    @Test
    fun `a sticker is named`() {
        val message = "[sticker type=\"Sunny Scales\" limit=\"0\"][/sticker]"

        assertEquals("Sticker: Sunny Scales", message.toChatPreview())
    }

    @Test
    fun `emoticons read as their name between colons`() {
        assertEquals(":steamhappy:", "[emoticon]steamhappy[/emoticon]".toChatPreview())
        assertEquals("hi :steamhappy:", "hi ːsteamhappyː".toChatPreview())
    }

    @Test
    fun `an image is just called an image`() {
        val message = "[img src=https://example.com/a.png width=10 height=10][/img]"

        assertEquals("Image", message.toChatPreview())
    }

    @Test
    fun `a spoiler is not given away`() {
        assertEquals("Spoiler", "[spoiler]the butler did it[/spoiler]".toChatPreview())
    }

    @Test
    fun `a coin flip and a dice roll show their outcome`() {
        assertEquals("Flip: heads", "[flip result=\"heads\"][/flip]".toChatPreview())
        assertEquals(
            "Random: 42",
            "[random min=\"1\" max=\"100\" result=\"42\"][/random]".toChatPreview(),
        )
    }

    @Test
    fun `a tag without its partner is dropped and its text kept`() {
        assertEquals("unclosed", "[b]unclosed".toChatPreview())
    }

    @Test
    fun `runs of whitespace become one space`() {
        assertEquals("one two", "[b]one[/b]\n\n   two".toChatPreview())
    }
}
