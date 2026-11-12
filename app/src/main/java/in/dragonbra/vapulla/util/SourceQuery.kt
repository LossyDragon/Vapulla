package `in`.dragonbra.vapulla.util

import `in`.dragonbra.vapulla.service.api.ServerInfo
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal A2S_INFO client for Source engine servers. The master server only gives us an
 * address and a player count, so this asks the server itself for its name, map and slots.
 * See https://developer.valvesoftware.com/wiki/Server_queries.
 */
object SourceQuery {

    private val INFO_REQUEST = byteArrayOf(-1, -1, -1, -1, 'T'.code.toByte()) +
        "Source Engine Query".toByteArray() + 0

    private const val HEADER_CHALLENGE = 'A'.code.toByte()
    private const val HEADER_INFO = 'I'.code.toByte()

    /** Queries [endpoint], following one challenge round trip; null on timeout or bad data. */
    suspend fun info(endpoint: InetSocketAddress, timeoutMs: Int = 1500): ServerInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMs
                    var request = INFO_REQUEST

                    // First reply is usually a challenge that must be echoed back.
                    repeat(2) {
                        socket.send(DatagramPacket(request, request.size, endpoint))

                        val buffer = ByteArray(1400)
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)

                        val data = ByteBuffer.wrap(
                            buffer,
                            0,
                            packet.length,
                        ).order(ByteOrder.LITTLE_ENDIAN)
                        data.int // 0xFFFFFFFF simple-packet header
                        when (data.get()) {
                            HEADER_CHALLENGE ->
                                request =
                                    INFO_REQUEST + ByteArray(4).also(data::get)

                            HEADER_INFO -> return@use parseInfo(data)

                            else -> return@use null
                        }
                    }
                    null
                }
            }.getOrNull()
        }

    private fun parseInfo(data: ByteBuffer): ServerInfo {
        data.get() // protocol version
        val name = data.cString()
        val map = data.cString()
        data.cString() // game folder
        val game = data.cString()
        data.short // app id
        val players = data.get().toInt() and 0xFF
        val maxPlayers = data.get().toInt() and 0xFF
        val bots = data.get().toInt() and 0xFF
        data.get() // server type
        data.get() // environment
        val passwordProtected = data.get().toInt() == 1
        val vacSecured = data.get().toInt() == 1

        return ServerInfo(
            name = name,
            map = map,
            game = game,
            players = players,
            maxPlayers = maxPlayers,
            bots = bots,
            passwordProtected = passwordProtected,
            vacSecured = vacSecured,
        )
    }

    private fun ByteBuffer.cString(): String {
        val out = ByteArrayOutputStream()
        while (hasRemaining()) {
            val byte = get()
            if (byte == 0.toByte()) break
            out.write(byte.toInt())
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
