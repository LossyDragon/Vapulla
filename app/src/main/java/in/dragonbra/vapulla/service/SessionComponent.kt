package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope

/**
 * A part of the Steam session that does more than answer requests: it reacts to what Steam
 * pushes and may run background jobs. [SteamConnection] creates one set per client.
 */
interface SessionComponent {

    /** Registers this component's callbacks; the result is closed with the client. */
    fun subscribe(callbacks: CallbackManager): List<Closeable>

    /**
     * Launches the jobs that only make sense while signed in. [session] is cancelled when the
     * sign-in ends, so a new one never runs alongside the previous one's loops.
     */
    fun start(session: CoroutineScope) = Unit
}
