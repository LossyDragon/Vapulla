package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesBase.CMsgProtoBufHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [PacketInspector] reads the header of a raw Steam packet and decides what to keep of it. */
class PacketInspectorTest {

    private val body = byteArrayOf(0x08, 0x96.toByte(), 0x01)
    private val call = EMsg.ServiceMethodCallFromClient
    private val ownedGames = "Player.GetOwnedGames#1"

    /** A protobuf packet as it is on the wire: marked message type, header length, header, body. */
    private fun protobufPacket(msg: EMsg, method: String, body: ByteArray): ByteArray {
        val header = CMsgProtoBufHeader.newBuilder()
            .setSteamid(76561197960287930)
            .setClientSessionid(1234)
            .setJobidSource(7)
            .setTargetJobName(method)
            .build()
            .toByteArray()

        return ByteBuffer.allocate(8 + header.size + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(msg.code() or 0x80000000.toInt())
            .putInt(header.size)
            .put(header)
            .put(body)
            .array()
    }

    @Test
    fun `a packet too short for a header is refused`() {
        assertNull(PacketInspector.inspect(EMsg.ClientHeartBeat, ByteArray(2), keepBody = true))
    }

    @Test
    fun `a protobuf packet gives its method, sender and body`() {
        val packet = protobufPacket(call, ownedGames, body)
        val detail = PacketInspector.inspect(call, packet, keepBody = true)

        assertNotNull(detail)
        detail!!
        assertEquals("Protobuf", detail.format)
        assertEquals(ownedGames, detail.method)
        assertEquals(76561197960287930, detail.steamId)
        assertEquals(1234, detail.sessionId)
        assertEquals(7L, detail.sourceJob)
        assertEquals(packet.size - body.size, detail.headerSize)
        assertEquals(body.size, detail.bodySize)
        assertArrayEquals(body, detail.body)
        assertNull(detail.bodyNote)
    }

    @Test
    fun `the body is dropped when bodies are not kept`() {
        val packet = protobufPacket(call, ownedGames, body)
        val detail = PacketInspector.inspect(call, packet, keepBody = false)

        assertNull(detail!!.body)
        assertEquals(body.size, detail.bodySize)
        assertTrue(detail.bodyNote!!.startsWith("Bodies are not kept"))
    }

    @Test
    fun `a sign-in message never keeps its body`() {
        val method = "Authentication.BeginAuthSessionViaCredentials#1"
        val msg = EMsg.ServiceMethodCallFromClientNonAuthed
        val packet = protobufPacket(msg, method, body)
        val detail = PacketInspector.inspect(msg, packet, keepBody = true)

        assertNull(detail!!.body)
        assertTrue(detail.bodyNote!!.contains("sign-in secrets"))
    }

    @Test
    fun `the list of auth tickets in use never keeps its body`() {
        val packet = protobufPacket(EMsg.ClientAuthList, "", body)
        val detail = PacketInspector.inspect(EMsg.ClientAuthList, packet, keepBody = true)

        assertNull(detail!!.body)
    }

    @Test
    fun `a packet without the protobuf mark is read with the struct header`() {
        val packet = ByteArray(36 + 4)
        val detail = PacketInspector.inspect(EMsg.ClientHeartBeat, packet, keepBody = true)

        assertEquals("Struct header", detail!!.format)
        assertEquals(36, detail.headerSize)
        assertEquals(4, detail.bodySize)
    }

    @Test
    fun `a protobuf body is decoded to field numbers and values`() {
        assertEquals("1: 150", PacketInspector.decodeProtobuf(body))
    }

    @Test
    fun `bytes that are not protobuf decode to nothing`() {
        assertNull(PacketInspector.decodeProtobuf(byteArrayOf(0xFF.toByte(), 0xFF.toByte())))
        assertNull(PacketInspector.decodeProtobuf(ByteArray(0)))
    }

    @Test
    fun `a hex dump shows offset, bytes and text, eight bytes to a line`() {
        val lines = PacketInspector.hexDump("Hello, Steam".toByteArray()).lines()

        assertEquals(2, lines.size)
        assertEquals("0000  48 65 6c 6c 6f 2c 20 53  Hello, S", lines[0])
        assertTrue(lines[1].startsWith("0008  74 65 61 6d"))
        assertTrue(lines[1].endsWith("team"))
    }

    @Test
    fun `bytes that are not printable show as dots`() {
        assertTrue(PacketInspector.hexDump(byteArrayOf(0x00, 0x41, 0x7F)).endsWith(".A."))
    }
}
