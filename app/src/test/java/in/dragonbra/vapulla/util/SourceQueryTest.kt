package `in`.dragonbra.vapulla.util

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [SourceQuery] reads the answer a game server gives to an A2S_INFO request. */
class SourceQueryTest {

    /** An answer as it stands after the header byte: the fields in the order of the protocol. */
    private fun infoAnswer(passwordProtected: Boolean, vacSecured: Boolean): ByteBuffer {
        val out = ByteArrayOutputStream()
        fun text(value: String) = out.write(value.toByteArray() + 0)

        out.write(17) // Protocol version.
        text("Valve Matchmaking Server")
        text("ctf_2fort")
        text("tf")
        text("Team Fortress")
        out.write(byteArrayOf(0xB8.toByte(), 0x01)) // App ID 440, low byte first.
        out.write(12) // Players.
        out.write(200) // Slots; more than a signed byte holds.
        out.write(2) // Bots.
        out.write('d'.code) // Dedicated server.
        out.write('l'.code) // Linux.
        out.write(if (passwordProtected) 1 else 0)
        out.write(if (vacSecured) 1 else 0)

        return ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
    }

    @Test
    fun `an answer gives the server's name, map, game and player counts`() {
        val info = SourceQuery.parseInfo(infoAnswer(passwordProtected = false, vacSecured = true))

        assertEquals("Valve Matchmaking Server", info.name)
        assertEquals("ctf_2fort", info.map)
        assertEquals("Team Fortress", info.game)
        assertEquals(12, info.players)
        assertEquals(200, info.maxPlayers)
        assertEquals(2, info.bots)
        assertFalse(info.passwordProtected)
        assertTrue(info.vacSecured)
    }

    @Test
    fun `a server behind a password says so`() {
        val info = SourceQuery.parseInfo(infoAnswer(passwordProtected = true, vacSecured = false))

        assertTrue(info.passwordProtected)
        assertFalse(info.vacSecured)
    }
}
