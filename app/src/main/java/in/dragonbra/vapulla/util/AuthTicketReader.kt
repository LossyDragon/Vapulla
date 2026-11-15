package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.types.SteamID
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date

/** One part of a ticket: what it is, and the fields in it. */
data class TicketPart(val title: String, val note: String, val rows: List<Pair<String, String>>)

/**
 * Reads the fields of an auth session ticket, the blob a game hands to a server to prove who
 * is playing. JavaSteam's `SteamAuthTicket` builds it from three parts, one after the other:
 *
 * 1. a game connect token Steam handed out after sign-in,
 * 2. a short session header written by the client,
 * 3. the app ownership ticket, signed by Steam.
 *
 * An ownership ticket on its own is read too. Valve does not document the layout; it is the
 * one the community has worked out.
 */
object AuthTicketReader {

    /** Size of a game connect token: a token, a Steam ID and a time. */
    private const val CONNECT_TOKEN_SIZE = 20

    private const val TYPE_AUTH_SESSION = 2
    private const val TYPE_WEB_API = 5

    /** The parts of [ticket] that could be read; empty when it is no ticket at all. */
    fun read(ticket: ByteArray): List<TicketPart> {
        val buffer = ByteBuffer.wrap(ticket).order(ByteOrder.LITTLE_ENDIAN)
        val parts = ArrayList<TicketPart>()

        try {
            // A session ticket starts with the size of its connect token.
            if (buffer.remaining() >= Int.SIZE_BYTES && buffer.getInt(0) == CONNECT_TOKEN_SIZE) {
                parts += connectToken(buffer)
                parts += sessionHeader(buffer)
                buffer.int // Size of the ownership ticket that follows.
            }
            parts += ownershipTicket(buffer, ticket.size)
        } catch (_: BufferUnderflowException) {
            // Cut short, or not a ticket: keep the parts read so far.
        }
        return parts
    }

    private fun connectToken(buffer: ByteBuffer): TicketPart {
        buffer.int // Its size.
        buffer.long // The token itself, a random number.
        val steamId = SteamID(buffer.long)
        val issued = buffer.int

        return TicketPart(
            title = "Game connect token",
            note = "Steam hands the client a few of these after sign-in. Each ticket uses one up.",
            rows = listOf(
                "Steam ID" to steamId.convertToUInt64().toULong().toString(),
                "Issued" to issued.toDate(),
            ),
        )
    }

    private fun sessionHeader(buffer: ByteBuffer): TicketPart {
        buffer.int // Its size.
        buffer.int // Always 1.
        val type = buffer.int
        buffer.long // The client's public and private address; JavaSteam writes random bytes.
        buffer.int // How long the client has been connected.
        val sequence = buffer.int

        return TicketPart(
            title = "Session header",
            note = "Written by the client, not by Steam: the kind of ticket and a counter.",
            rows = listOf(
                "Type" to when (type) {
                    TYPE_AUTH_SESSION -> "Auth session ($type)"
                    TYPE_WEB_API -> "Web API ($type)"
                    else -> type.toString()
                },
                "Tickets made this run" to sequence.toUInt().toString(),
            ),
        )
    }

    private fun ownershipTicket(buffer: ByteBuffer, ticketSize: Int): TicketPart {
        val start = buffer.position()
        val length = buffer.int // Counts itself, but not the signature after the ticket.
        val version = buffer.int
        val steamId = SteamID(buffer.long)
        val appId = buffer.int
        val publicIp = buffer.int
        buffer.int // The private address.
        val flags = buffer.int
        val issued = buffer.int
        val expires = buffer.int
        val licenses = List(buffer.count()) { buffer.int.toUInt() }
        val dlc = List(buffer.count()) {
            val dlcAppId = buffer.int.toUInt()
            repeat(buffer.count()) { buffer.int } // The packages that grant it.
            dlcAppId
        }
        buffer.short // Reserved.
        val signature = ticketSize - (start + length)

        return TicketPart(
            title = "App ownership ticket",
            note = "Steam's signed statement that the account owns the app. A server checks " +
                "the signature with Steam's public key.",
            rows = listOf(
                "Version" to version.toString(),
                "Steam ID" to steamId.convertToUInt64().toULong().toString(),
                "App ID" to appId.toUInt().toString(),
                "Public address" to publicIp.toAddress(),
                "Flags" to "0x%08x".format(flags),
                "Issued" to issued.toDate(),
                "Expires" to expires.toDate(),
                "Packages" to licenses.joinToString(),
                "DLC" to dlc.joinToString(),
                "Signature" to if (signature > 0) "$signature bytes" else "None",
            ),
        )
    }

    /** An unsigned 16-bit count of the entries that follow. */
    private fun ByteBuffer.count(): Int = short.toInt() and 0xFFFF

    /** Unsigned seconds since 1970 as a date. */
    private fun Int.toDate(): String = Date(toUInt().toLong() * 1000).toString()

    /** An IPv4 address held in an integer, highest byte first. */
    private fun Int.toAddress(): String =
        listOf(this ushr 24, this ushr 16, this ushr 8, this).joinToString(".") {
            (it and 0xFF).toString()
        }
}
