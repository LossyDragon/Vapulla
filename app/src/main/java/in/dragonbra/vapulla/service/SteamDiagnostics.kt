package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What Steam told the client when it accepted the logon, plus where the client is connected.
 *
 * @property server the connection manager (CM) server, as `host:port`.
 * @property serverTimeOffsetMs Steam's clock minus this device's, at sign-in.
 * @property heartbeatSeconds how often the client has to send a heartbeat to stay signed in.
 */
@Immutable
data class ConnectionInfo(
    val server: String,
    val allowedProtocols: String,
    val steamId: Long,
    val sessionId: Int?,
    val cellId: Int,
    val publicIp: String,
    val ipCountry: String,
    val heartbeatSeconds: Int,
    val serverTimeOffsetMs: Long?,
    val accountFlags: ImmutableList<String>,
    val emailDomain: String,
    val vanityUrl: String,
    val familyGroupId: Long,
    val signedInAt: Long,
)

/** One step in the life of the connection. [id] is a running number, used as the list key. */
@Immutable
data class ConnectionEvent(val id: Long, val timestamp: Long, val text: String)

/** A CM server the client knows about. [current] marks the one it is connected to. */
@Immutable
data class CmServer(val host: String, val port: Int, val protocols: String, val current: Boolean)

/**
 * Process-wide record of what the connection is doing and has done, for the Under the Hood
 * screens. Written by [SteamConnection]. Nothing here is persisted: it describes this run.
 */
object SteamDiagnostics {

    private const val MAX_EVENTS = 200

    /** When this process started, for uptime. */
    val processStartedAt: Long = System.currentTimeMillis()

    private val nextId = AtomicLong()

    private val _connection = MutableStateFlow<ConnectionInfo?>(null)

    /** The current signed-in session, or null while there is none. */
    val connection: StateFlow<ConnectionInfo?> = _connection.asStateFlow()

    private val _events = MutableStateFlow<ImmutableList<ConnectionEvent>>(persistentListOf())

    /** Connection history of this run, oldest first. */
    val events: StateFlow<ImmutableList<ConnectionEvent>> = _events.asStateFlow()

    internal fun signedIn(info: ConnectionInfo) {
        _connection.value = info
    }

    internal fun signedOut() {
        _connection.value = null
    }

    internal fun record(text: String) {
        val event = ConnectionEvent(nextId.getAndIncrement(), System.currentTimeMillis(), text)
        _events.update { current -> (current + event).takeLast(MAX_EVENTS).toImmutableList() }
    }
}
