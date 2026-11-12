package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.util.IDebugNetworkListener
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** One packet seen on the Steam connection. [id] is a running number, used as the list key. */
data class NetworkLogEntry(
    val id: Long,
    val timestamp: Long,
    val incoming: Boolean,
    val msg: EMsg,
    val size: Int,
)

/**
 * Ring buffer of the last [maxEntries] packets, fed by JavaSteam's debug network listener.
 * It is a showcase of the library's debugging hook rather than a production feature, so it
 * only records message types and sizes, never payloads.
 */
object NetworkLog : IDebugNetworkListener {

    /** Packets to keep; set from Settings by the application. */
    @Volatile
    var maxEntries: Int = 500

    private val nextId = AtomicLong()

    private val _entries = MutableStateFlow<ImmutableList<NetworkLogEntry>>(persistentListOf())
    val entries: StateFlow<ImmutableList<NetworkLogEntry>> = _entries

    override fun onIncomingNetworkMessage(msgType: EMsg, data: ByteArray) =
        record(true, msgType, data.size)

    override fun onOutgoingNetworkMessage(msgType: EMsg, data: ByteArray) =
        record(false, msgType, data.size)

    fun clear() {
        _entries.value = persistentListOf()
    }

    private fun record(incoming: Boolean, msg: EMsg, size: Int) {
        val entry =
            NetworkLogEntry(
                nextId.getAndIncrement(),
                System.currentTimeMillis(),
                incoming,
                msg,
                size,
            )
        _entries.update { current -> (current + entry).takeLast(maxEntries).toImmutableList() }
    }
}
