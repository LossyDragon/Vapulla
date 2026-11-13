package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.util.IDebugNetworkListener
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * One packet seen on the Steam connection. [id] is a running number, used as the list key.
 *
 * @property detail the parsed header and, when kept, the body; null if it could not be read.
 */
@Immutable
data class NetworkLogEntry(
    val id: Long,
    val timestamp: Long,
    val incoming: Boolean,
    val msg: EMsg,
    val size: Int,
    val detail: PacketDetail? = null,
) {
    /** What to call the packet: the service method when it is a service call, else the EMsg. */
    val label: String get() = detail?.method ?: msg.name
}

/** Everything seen of one message type since the log was last cleared. */
@Immutable
data class NetworkLogTotal(
    val msg: EMsg,
    val packetsIn: Int = 0,
    val packetsOut: Int = 0,
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
) {
    val bytes: Long get() = bytesIn + bytesOut
}

/**
 * Ring buffer of the last [maxEntries] packets, fed by JavaSteam's debug network listener,
 * plus running [totals] per message type that are not limited to the buffer.
 * It is a showcase of the library's debugging hook rather than a production feature. Headers
 * are always read; bodies are only kept while [keepBodies] is on, and never for messages
 * that carry sign-in secrets (see [PacketInspector]).
 */
object NetworkLog : IDebugNetworkListener {

    /** Packets to keep; set from Settings by the application. */
    @Volatile
    var maxEntries: Int = 500

    /** Whether packet bodies are kept for the detail view; set from Settings. */
    @Volatile
    var keepBodies: Boolean = false

    private val nextId = AtomicLong()

    private val _entries = MutableStateFlow<ImmutableList<NetworkLogEntry>>(persistentListOf())
    val entries: StateFlow<ImmutableList<NetworkLogEntry>> = _entries

    private val _totals = MutableStateFlow<PersistentMap<EMsg, NetworkLogTotal>>(persistentMapOf())

    /** Packets and bytes per message type since the last [clear]. */
    val totals: StateFlow<PersistentMap<EMsg, NetworkLogTotal>> = _totals

    override fun onIncomingNetworkMessage(msgType: EMsg, data: ByteArray) =
        record(true, msgType, data)

    override fun onOutgoingNetworkMessage(msgType: EMsg, data: ByteArray) =
        record(false, msgType, data)

    fun clear() {
        _entries.value = persistentListOf()
        _totals.value = persistentMapOf()
    }

    private fun record(incoming: Boolean, msg: EMsg, data: ByteArray) {
        val size = data.size
        val entry =
            NetworkLogEntry(
                nextId.getAndIncrement(),
                System.currentTimeMillis(),
                incoming,
                msg,
                size,
                PacketInspector.inspect(msg, data, keepBodies),
            )
        _entries.update { current -> (current + entry).takeLast(maxEntries).toImmutableList() }

        _totals.update { current ->
            val total = current[msg] ?: NetworkLogTotal(msg)
            current.put(
                msg,
                if (incoming) {
                    total.copy(packetsIn = total.packetsIn + 1, bytesIn = total.bytesIn + size)
                } else {
                    total.copy(packetsOut = total.packetsOut + 1, bytesOut = total.bytesOut + size)
                },
            )
        }
    }
}
