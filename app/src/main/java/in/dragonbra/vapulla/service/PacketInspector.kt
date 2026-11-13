package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import com.google.protobuf.UnknownFieldSet
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesBase.CMsgProtoBufHeader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * What a packet's header says, plus its body when that was kept.
 *
 * @property format how the packet is laid out: protobuf, the older struct header, or bare.
 * @property method for service calls the method called, e.g. `Player.GetOwnedGames#1`.
 * @property sourceJob the sender's job ID: set on a request, echoed as [targetJob] in the answer.
 * @property result Steam's result code, only on protobuf answers.
 * @property body the bytes after the header, cut to [PacketInspector.MAX_BODY]; null when not kept.
 * @property bodyNote why [body] is missing or shortened.
 */
@Immutable
class PacketDetail(
    val format: String,
    val method: String?,
    val sourceJob: Long?,
    val targetJob: Long?,
    val steamId: Long?,
    val sessionId: Int?,
    val result: String?,
    val headerSize: Int,
    val bodySize: Int,
    val body: ByteArray?,
    val bodyNote: String?,
)

/**
 * Reads the header of a raw Steam packet, as handed to JavaSteam's debug network listener.
 * JavaSteam parses these itself but keeps the result internal, so the layout is repeated here:
 *
 * - protobuf: EMsg with the top bit set, header length, a `CMsgProtoBufHeader`, then the body
 * - struct (`ExtendedClientMsgHdr`): 36 fixed bytes, then the body
 * - bare (`MsgHdr`): 20 fixed bytes; only the channel encryption handshake uses it
 */
object PacketInspector {

    /** Most of a body that is kept per packet; the license list alone can be far larger. */
    const val MAX_BODY = 16 * 1024

    private const val PROTO_MASK = 0x80000000.toInt()
    private const val PROTO_PREFIX = 8
    private const val EXTENDED_HEADER = 36
    private const val BARE_HEADER = 20

    /** No job: both headers use all bits set for "none". */
    private const val NO_JOB = -1L

    /** Bytes per line of [hexDump]; narrow enough for a phone in portrait. */
    private const val HEX_WIDTH = 8

    /**
     * @param keepBody false drops the body whatever it is; true keeps it unless the message
     *   carries sign-in secrets.
     * @return null when the packet is too short to have a header.
     */
    fun inspect(msg: EMsg, data: ByteArray, keepBody: Boolean): PacketDetail? = runCatching {
        if (data.size < Int.SIZE_BYTES) return null
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val isProto = buffer.getInt(0) and PROTO_MASK != 0

        when {
            isProto -> {
                val headerLength = buffer.getInt(Int.SIZE_BYTES)
                val header = CMsgProtoBufHeader.parseFrom(
                    ByteBuffer.wrap(data, PROTO_PREFIX, headerLength),
                )
                val method = header.targetJobName.takeIf { it.isNotEmpty() }

                detail(
                    format = "Protobuf",
                    msg = msg,
                    method = method,
                    sourceJob = header.jobidSource,
                    targetJob = header.jobidTarget,
                    steamId = header.steamid.takeIf { header.hasSteamid() },
                    sessionId = header.clientSessionid.takeIf { header.hasClientSessionid() },
                    result = header.eresult.takeIf { header.hasEresult() }
                        ?.let { EResult.from(it)?.name ?: it.toString() },
                    headerSize = PROTO_PREFIX + headerLength,
                    data = data,
                    keepBody = keepBody,
                )
            }

            msg.name.startsWith("ChannelEncrypt") -> detail(
                format = "Bare header",
                msg = msg,
                sourceJob = buffer.getLong(12),
                targetJob = buffer.getLong(4),
                headerSize = BARE_HEADER,
                data = data,
                keepBody = keepBody,
            )

            else -> detail(
                format = "Struct header",
                msg = msg,
                targetJob = buffer.getLong(7),
                sourceJob = buffer.getLong(15),
                steamId = buffer.getLong(24),
                sessionId = buffer.getInt(32),
                headerSize = EXTENDED_HEADER,
                data = data,
                keepBody = keepBody,
            )
        }
    }.getOrNull()

    private fun detail(
        format: String,
        msg: EMsg,
        method: String? = null,
        sourceJob: Long,
        targetJob: Long,
        steamId: Long? = null,
        sessionId: Int? = null,
        result: String? = null,
        headerSize: Int,
        data: ByteArray,
        keepBody: Boolean,
    ): PacketDetail {
        val bodySize = (data.size - headerSize).coerceAtLeast(0)
        val kept = minOf(bodySize, MAX_BODY)

        val note = when {
            bodySize == 0 -> "This message has no body."
            !keepBody -> "Bodies are not kept. Turn on \"Keep packet contents\" in Settings."
            isSecret(msg, method) -> "Not kept: this message carries sign-in secrets."
            kept < bodySize -> "Only the first $kept of $bodySize bytes were kept."
            else -> null
        }
        val body = if (bodySize > 0 && keepBody && !isSecret(msg, method)) {
            data.copyOfRange(headerSize, headerSize + kept)
        } else {
            null
        }

        return PacketDetail(
            format = format,
            method = method,
            sourceJob = sourceJob.takeIf { it != NO_JOB },
            targetJob = targetJob.takeIf { it != NO_JOB },
            steamId = steamId?.takeIf { it != 0L },
            sessionId = sessionId?.takeIf { it != 0 },
            result = result,
            headerSize = headerSize,
            bodySize = bodySize,
            body = body,
            bodyNote = note,
        )
    }

    /**
     * Messages whose body holds a password, token, ticket or session key. A rule of thumb
     * by name rather than a list of every such message, so it errs towards withholding.
     */
    private fun isSecret(msg: EMsg, method: String?): Boolean {
        val words = listOf("logon", "channelencrypt", "nonce", "ticket", "token", "password")
        val name = msg.name.lowercase()
        val call = method.orEmpty().lowercase()

        return words.any { it in name || it in call } || call.startsWith("authentication.")
    }

    /**
     * A protobuf body read without its schema: field numbers and values, nested messages
     * indented. Field names are not in the bytes, so they cannot be shown.
     *
     * @return null when the bytes are not valid protobuf (a struct body, or a cut-off one).
     */
    fun decodeProtobuf(body: ByteArray): String? = runCatching {
        UnknownFieldSet.parseFrom(body).toString().trimEnd().takeIf { it.isNotEmpty() }
    }.getOrNull()

    /** Offset, bytes in hex and the same bytes as text, [HEX_WIDTH] to a line. */
    fun hexDump(bytes: ByteArray): String = buildString {
        for (offset in bytes.indices step HEX_WIDTH) {
            val line = bytes.copyOfRange(offset, minOf(offset + HEX_WIDTH, bytes.size))

            append("%04x  ".format(offset))
            line.forEach { append("%02x ".format(it)) }
            repeat(HEX_WIDTH - line.size) { append("   ") }
            append(' ')
            line.forEach { byte ->
                val char = byte.toInt().toChar()
                append(if (char in ' '..'~') char else '.')
            }
            append('\n')
        }
    }.trimEnd()
}
