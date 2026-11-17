package `in`.dragonbra.vapulla.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-wide connection state, written by the classes in this package and read by the UI
 * through the read-only flows on [SteamService]'s companion.
 */
internal object SteamState {

    /** True between an accepted logon and the end of that session. */
    val isLoggedIn = MutableStateFlow(false)

    /**
     * True during a session without an account, see `SteamConnection.logOnAnonymous`.
     * [isLoggedIn] stays false then: nothing that belongs to an account exists.
     */
    val isAnonymous = MutableStateFlow(false)

    /** Progress of the sign-in attempt under way. Not replayed: only a login screen hears it. */
    val loginResult = MutableSharedFlow<LoginResult>()

    /**
     * Why Steam ended the last session, when it was not the user signing out. Held as state
     * rather than sent through [loginResult], because the login screen that shows it does
     * not exist yet at the moment the session ends.
     */
    val signOutReason = MutableStateFlow<String?>(null)

    /**
     * True when the user disconnected and kept the saved sign-in. The login screen that
     * comes up next must then not use it to sign straight back in.
     */
    val stayDisconnected = MutableStateFlow(false)
}
