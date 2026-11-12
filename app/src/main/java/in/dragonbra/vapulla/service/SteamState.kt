package `in`.dragonbra.vapulla.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-wide connection state, written by the classes in this package and read by the UI
 * through the read-only flows on [SteamService]'s companion.
 */
internal object SteamState {
    val isLoading = MutableStateFlow(false)

    /** True while a client exists and its callback loop runs. */
    val isRunning = MutableStateFlow(false)

    /** True between an accepted logon and the end of that session. */
    val isLoggedIn = MutableStateFlow(false)

    val loginResult = MutableSharedFlow<LoginResult>()
}
