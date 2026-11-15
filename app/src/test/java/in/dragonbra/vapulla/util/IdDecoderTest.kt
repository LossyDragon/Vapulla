package `in`.dragonbra.vapulla.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [IdDecoder] reads Steam's IDs in their different written forms. */
class IdDecoderTest {

    /** The value of the row [label] in a reading, or null when it has no such row. */
    private fun DecodedId.row(label: String): String? =
        rows.firstOrNull { it.first == label }?.second

    @Test
    fun `text that is no ID gives nothing`() {
        assertTrue(IdDecoder.decode("").isEmpty())
        assertTrue(IdDecoder.decode("   ").isEmpty())
        assertTrue(IdDecoder.decode("not an id").isEmpty())
    }

    @Test
    fun `a 64-bit Steam ID breaks into its parts`() {
        val readings = IdDecoder.decode("76561197960287930")
        val steamId = readings.first { it.title == "Steam ID" }

        assertEquals("76561197960287930", steamId.row("64-bit"))
        assertEquals("[U:1:22202]", steamId.row("Steam3"))
        assertEquals("STEAM_0:0:11101", steamId.row("Steam2"))
        assertEquals("22202", steamId.row("Account ID"))
        assertEquals("Public", steamId.row("Universe"))
        assertEquals("Individual", steamId.row("Type"))
        assertEquals("Yes", steamId.row("Valid"))
    }

    @Test
    fun `a 64-bit number is also read as a game ID and a global ID`() {
        val titles = IdDecoder.decode("76561197960287930").map { it.title }

        assertEquals(listOf("Steam ID", "Game ID", "Global ID"), titles)
    }

    @Test
    fun `the Steam3 text form gives the same account`() {
        val reading = IdDecoder.decode("[U:1:22202]").single()

        assertEquals("76561197960287930", reading.row("64-bit"))
    }

    @Test
    fun `the Steam2 text form gives the same account in either case`() {
        val upper = IdDecoder.decode("STEAM_0:0:11101").single()
        val lower = IdDecoder.decode("steam_0:0:11101").single()

        assertEquals("76561197960287930", upper.row("64-bit"))
        assertEquals("76561197960287930", lower.row("64-bit"))
    }

    @Test
    fun `a small number is read as an account ID and as an app ID`() {
        val readings = IdDecoder.decode("440")

        assertEquals(listOf("Steam ID", "Game ID"), readings.map { it.title })
        assertEquals("440", readings[0].row("Account ID"))
        assertEquals("440", readings[1].row("App ID"))
    }

    @Test
    fun `the largest unsigned 64-bit number is still read`() {
        assertTrue(IdDecoder.decode("18446744073709551615").isNotEmpty())
    }

    @Test
    fun `a custom URL is taken from a name or from a profile address`() {
        assertEquals("lossydragon", IdDecoder.customUrlName("lossydragon"))
        assertEquals("lossydragon", IdDecoder.customUrlName(" lossydragon "))
        assertEquals(
            "lossydragon",
            IdDecoder.customUrlName("https://steamcommunity.com/id/lossydragon/"),
        )
        assertEquals("lossydragon", IdDecoder.customUrlName("steamcommunity.com/id/lossydragon"))
    }

    @Test
    fun `text that cannot be a custom URL is refused`() {
        assertNull(IdDecoder.customUrlName("a"))
        assertNull(IdDecoder.customUrlName("two words"))
        assertNull(IdDecoder.customUrlName("https://example.com/id/someone"))
    }
}
