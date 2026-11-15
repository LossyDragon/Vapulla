package `in`.dragonbra.vapulla.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [AuthTicketReader] reads the fields of an auth session ticket. */
class AuthTicketReaderTest {

    private val steamId = 76561197960287930

    /** The value of the row [label] in a part, or null when it has no such row. */
    private fun TicketPart.row(label: String): String? =
        rows.firstOrNull { it.first == label }?.second

    private fun buffer(size: Int): ByteBuffer =
        ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)

    /** An ownership ticket for app 440 with two packages, one DLC and a signature. */
    private fun ownershipTicket(): ByteArray {
        val body = buffer(64)
            .putInt(64) // Its own length, without the signature.
            .putInt(4)
            .putLong(steamId)
            .putInt(440)
            .putInt(0x01020304)
            .putInt(0)
            .putInt(2)
            .putInt(1_700_000_000)
            .putInt(1_700_100_000)
            .putShort(2).putInt(0).putInt(469)
            .putShort(1).putInt(459).putShort(1).putInt(469)
            .putShort(0)
            .array()

        return body + ByteArray(128)
    }

    /** A session ticket: connect token, session header, then the ownership ticket. */
    private fun sessionTicket(): ByteArray {
        val ownership = ownershipTicket()
        val head = buffer(56)
            .putInt(20).putLong(123456789).putLong(steamId).putInt(1_700_000_000)
            .putInt(24).putInt(1).putInt(2).putLong(0).putInt(5000).putInt(3)
            .putInt(ownership.size)
            .array()

        return head + ownership
    }

    @Test
    fun `bytes that are no ticket give nothing`() {
        assertTrue(AuthTicketReader.read(ByteArray(0)).isEmpty())
        assertTrue(AuthTicketReader.read(byteArrayOf(1, 2, 3)).isEmpty())
    }

    @Test
    fun `a session ticket breaks into its three parts`() {
        val titles = AuthTicketReader.read(sessionTicket()).map { it.title }

        assertEquals(
            listOf("Game connect token", "Session header", "App ownership ticket"),
            titles,
        )
    }

    @Test
    fun `the connect token and the session header are read`() {
        val (token, header) = AuthTicketReader.read(sessionTicket())

        assertEquals(steamId.toString(), token.row("Steam ID"))
        assertEquals("Auth session (2)", header.row("Type"))
        assertEquals("3", header.row("Tickets made this run"))
    }

    @Test
    fun `the ownership ticket is read down to its signature`() {
        val ownership = AuthTicketReader.read(sessionTicket()).last()

        assertEquals("4", ownership.row("Version"))
        assertEquals(steamId.toString(), ownership.row("Steam ID"))
        assertEquals("440", ownership.row("App ID"))
        assertEquals("1.2.3.4", ownership.row("Public address"))
        assertEquals("0x00000002", ownership.row("Flags"))
        assertEquals("0, 469", ownership.row("Packages"))
        assertEquals("459", ownership.row("DLC"))
        assertEquals("128 bytes", ownership.row("Signature"))
    }

    @Test
    fun `an ownership ticket on its own is read too`() {
        val part = AuthTicketReader.read(ownershipTicket()).single()

        assertEquals("App ownership ticket", part.title)
        assertEquals("440", part.row("App ID"))
    }

    @Test
    fun `a ticket cut short keeps the parts before the cut`() {
        val cut = sessionTicket().copyOf(70)

        assertEquals(
            listOf("Game connect token", "Session header"),
            AuthTicketReader.read(cut).map { it.title },
        )
    }
}
