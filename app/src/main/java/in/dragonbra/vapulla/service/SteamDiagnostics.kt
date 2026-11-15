package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.steam.CMClient
import `in`.dragonbra.vapulla.util.appendCapped
import java.util.concurrent.atomic.AtomicLong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
 * screens. Written by [SteamConnection], and by [NetworkLog] and JavaSteam's log for the
 * reason of a disconnect. Nothing here is persisted: it describes this run.
 */
object SteamDiagnostics {

    private const val MAX_EVENTS = 200

    /** How recent JavaSteam's last log line must be to count as the reason for a disconnect. */
    private const val LOG_IS_REASON_MS = 5_000

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
        _events.appendCapped(event, MAX_EVENTS)
    }

    /** Something seen on the connection, and when. */
    private class Seen(val text: String, val at: Long = System.currentTimeMillis()) {
        fun ago(): String = "$text ${(System.currentTimeMillis() - at) / 1000}s ago"
    }

    @Volatile
    private var lastIn: Seen? = null

    @Volatile
    private var lastOut: Seen? = null

    @Volatile
    private var lastLog: Seen? = null

    /** Notes the latest packet in each direction; called by [NetworkLog] for every packet. */
    internal fun packet(incoming: Boolean, label: String) {
        if (incoming) lastIn = Seen(label) else lastOut = Seen(label)
    }

    /**
     * Notes the latest thing JavaSteam's connection classes logged. A disconnect callback
     * carries no reason, but the library logs one just before it drops the connection.
     */
    internal fun libraryLog(clazz: Class<*>, message: String?, throwable: Throwable?) {
        val aboutConnection = clazz == CMClient::class.java ||
            clazz.name.startsWith("in.dragonbra.javasteam.networking.")

        // CMClient logs this while handling every disconnect, which would hide the reason.
        if (!aboutConnection || message == null || message.startsWith("EventHandler")) return

        lastLog = Seen(
            clazz.simpleName + ": " + message + throwable?.let { " ($it)" }.orEmpty(),
        )
    }

    /**
     * What is known about why the connection just dropped, as extra lines for the
     * "Disconnected" event: JavaSteam's last word, and the last packet each way.
     */
    internal fun disconnectDetail(): String = buildString {
        // Anything older was not logged about this disconnect.
        lastLog?.takeIf { System.currentTimeMillis() - it.at < LOG_IS_REASON_MS }?.let {
            append("\n${it.text}")
        }
        lastIn?.let { append("\nLast in: ${it.ago()}") }
        lastOut?.let { append("\nLast out: ${it.ago()}") }
    }
}
