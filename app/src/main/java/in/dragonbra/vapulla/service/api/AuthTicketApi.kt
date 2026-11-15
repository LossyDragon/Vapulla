package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.TicketInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Auth session tickets from the `SteamAuthTicket` handler: the proof of identity and
 * ownership a game hands to a game server, which checks it with Steam.
 *
 * A ticket stays valid until it is cancelled, so every ticket made should be given back to
 * [cancel] once it is no longer needed.
 */
class AuthTicketApi(private val authTicket: SteamAuthTicket, private val scope: CoroutineScope) {

    /**
     * Makes a ticket for [appId] and registers it with Steam as in use.
     *
     * @throws Exception with the reason when the account does not own the app, or Steam has
     *   handed out no game connect token to build a ticket from.
     */
    suspend fun make(appId: Int): TicketInfo = authTicket.getAuthSessionTicket(appId).await()

    /** Tells Steam that [ticket] is no longer in use; a server is then refused with it. */
    fun cancel(ticket: TicketInfo) {
        scope.launch(Dispatchers.IO) {
            runCatching { ticket.close() }
                .onFailure { Timber.w(it, "Cancelling an auth ticket failed") }
        }
    }
}
