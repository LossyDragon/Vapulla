package `in`.dragonbra.vapulla.util

import `in`.dragonbra.vapulla.util.Utils.minutesToPlaytime
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** The formatting helpers of [Utils] that need nothing from Android. */
class UtilsTest {

    private val locale = Locale.getDefault()

    /** Numbers are written the way the device's language writes them, so one is fixed here. */
    @Before
    fun useEnglishNumbers() = Locale.setDefault(Locale.US)

    @After
    fun restoreLocale() = Locale.setDefault(locale)

    @Test
    fun `an owned emoticon has its colons marked`() {
        assertEquals("ːsteamhappyː hi", Utils.findEmotes(":steamhappy: hi", setOf("steamhappy")))
    }

    @Test
    fun `text between colons that is no owned emoticon stays as it is`() {
        val time = "at 10:30:15 sharp"

        assertEquals(time, Utils.findEmotes(time, setOf("steamhappy")))
        assertEquals(":nope:", Utils.findEmotes(":nope:", emptySet()))
    }

    @Test
    fun `a colon that closes one token may open the next`() {
        assertEquals(":aːbː", Utils.findEmotes(":a:b:", setOf("b")))
    }

    @Test
    fun `every owned emoticon in a message is marked`() {
        assertEquals("ːaː and ːbː", Utils.findEmotes(":a: and :b:", setOf("a", "b")))
    }

    @Test
    fun `a file size is shown in the largest unit that fits`() {
        assertEquals("512 B", 512L.toFileSize())
        assertEquals("1.5 KB", 1_536L.toFileSize())
        assertEquals("3.0 GB", (3L * 1024 * 1024 * 1024).toFileSize())
    }

    @Test
    fun `playtime is shown in minutes, then hours with a tenth, then whole hours`() {
        assertEquals("45 min", 45.minutesToPlaytime())
        assertEquals("12.5 h", 750.minutesToPlaytime())
        assertEquals("1,204 h", 72_240.minutesToPlaytime())
    }

    @Test
    fun `an avatar hash becomes the address of the full-size image`() {
        // Not the default avatar's hash, which would give the same address either way.
        val hash = "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678"
        val expected = "${Utils.Constants.AVATAR_BASE_URL}a1/${hash}_full.jpg"

        assertEquals(expected, Utils.getAvatarURL(hash))
    }

    @Test
    fun `no avatar hash, or one of only zeros, gives the default avatar`() {
        assertEquals(Utils.Constants.MISSING_AVATAR_URL, Utils.getAvatarURL(null))
        assertEquals(Utils.Constants.MISSING_AVATAR_URL, Utils.getAvatarURL(""))
        assertEquals(
            Utils.Constants.MISSING_AVATAR_URL,
            Utils.getAvatarURL(Utils.Constants.ALL_ZEROS),
        )
    }

    @Test
    fun `a group without an avatar has no avatar address`() {
        assertNull(Utils.getGroupAvatarURL(""))
        assertNull(Utils.getGroupAvatarURL(Utils.Constants.ALL_ZEROS))
        assertEquals(
            "${Utils.Constants.GROUP_AVATAR_BASE_URL}abc_256.jpg",
            Utils.getGroupAvatarURL("abc"),
        )
    }

    @Test
    fun `a time is told as how long ago it was, in the largest unit that fits`() {
        val now = 1_700_000_000_000L
        val second = 1_000L
        val hour = 3_600 * second
        val day = 24 * hour

        assertEquals("Just now", (now - 30 * second).toTimeAgo(now))
        assertEquals("1 minute ago", (now - 90 * second).toTimeAgo(now))
        assertEquals("1 hour ago", (now - hour).toTimeAgo(now))
        assertEquals("3 hours ago", (now - 3 * hour).toTimeAgo(now))
        assertEquals("2 days ago", (now - 2 * day).toTimeAgo(now))
        assertEquals("2 weeks ago", (now - 14 * day).toTimeAgo(now))
        assertEquals("1 year ago", (now - 400 * day).toTimeAgo(now))
    }

    @Test
    fun `a time in the future reads as just now`() {
        assertEquals("Just now", 2_000L.toTimeAgo(now = 1_000L))
    }

    @Test
    fun `money is written in its currency`() {
        assertEquals("$9.99", Utils.formatMoney(999, "USD"))
    }

    @Test
    fun `a currency the device does not know is shown as a number with its code`() {
        assertEquals("9.99 NOPE", Utils.formatMoney(999, "NOPE"))
    }
}
