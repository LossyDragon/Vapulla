package `in`.dragonbra.vapulla.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.LoginResult
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.util.NetworkMonitor
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Drives the login screen. Signs in automatically when a saved refresh token exists, and
 * otherwise relays credential or QR sign-in to [SteamService] and reflects its progress.
 *
 * JavaSteam calls the [IAuthenticator] on its own threads, so the state is only ever changed
 * with `update`, which does not lose a change made elsewhere at the same moment.
 */
class LoginViewModel(
    private val connection: SteamServiceConnection,
    private val accountManager: AccountManager,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** One-off messages for the screen to show. */
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Emits when the sign-in succeeded and the screen should be left. */
    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    /** Completed by [onTwoFactorSubmit] while JavaSteam waits for a Steam Guard code. */
    private var pendingGuardCode: CompletableFuture<String>? = null

    /** A saved sign-in is waiting for the screen to be in front of the user. */
    private var signInWhenVisible = false

    /** A sign-in is saved, so signing in needs no password. */
    private var hasSavedSignIn = false

    /**
     * Answers JavaSteam's Steam Guard prompts. Codes are asked for on the two-factor step and
     * handed back through [pendingGuardCode]; mobile confirmation just shows a waiting state.
     */
    private val authenticator = object : IAuthenticator {
        override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> =
            requestGuardCode(
                "Enter the code from your Steam Guard mobile app",
                previousCodeWasIncorrect,
            )

        override fun getEmailCode(
            email: String?,
            previousCodeWasIncorrect: Boolean,
        ): CompletableFuture<String> = requestGuardCode(
            "Enter the code Steam sent to ${email ?: "your email"}",
            previousCodeWasIncorrect,
        )

        override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
            Timber.i("Two-Factor, device confirmation")

            _uiState.update {
                it.copy(
                    loginStep = LoginStep.QRCODE,
                    isWaitingForConfirmation = true,
                    twoFactorCode = "",
                )
            }

            return CompletableFuture.completedFuture(true)
        }
    }

    init {
        // Steam ended the previous session (revoked, signed in elsewhere, ...): say so,
        // since otherwise the user lands here with no explanation.
        SteamService.takeSignOutReason()?.let { reason ->
            _uiState.update { it.copy(error = reason) }
        }

        viewModelScope.launch {
            connection.isBound.collect { bound ->
                if (bound) autoSignIn()
            }
        }

        // Say so when there is no internet, and when it is back use the saved sign-in again
        // without waiting for a tap. Not while something is being typed or shown instead.
        viewModelScope.launch {
            var wasOffline = false
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }

                val state = _uiState.value
                val idle = !state.isLoading &&
                    state.loginStep == LoginStep.CREDENTIALS &&
                    state.password.isEmpty() &&
                    !SteamService.isLoggedIn.value &&
                    !SteamService.isAnonymous.value
                if (online && wasOffline && idle && connection.isBound.value) autoSignIn()

                wasOffline = !online
            }
        }

        // An anonymous session has started and its screens are in front. This one stays
        // underneath, so leave it ready for when they are left again.
        viewModelScope.launch {
            SteamService.isAnonymous.collect { anonymous ->
                if (anonymous) _uiState.update { it.copy(isLoading = false, error = null) }
            }
        }

        viewModelScope.launch {
            SteamService.loginResult.collect { result ->
                when (result) {
                    LoginResult.Loading -> _uiState.update { it.copy(isLoading = true) }

                    LoginResult.Success -> {
                        Timber.d("Logged in, navigating to home screen")
                        _uiState.value = LoginUiState(isOffline = !networkMonitor.isOnline.value)
                        hasSavedSignIn = false
                        _navigateToHome.emit(Unit)
                    }

                    is LoginResult.Error -> {
                        Timber.e(result.error)
                        _uiState.update {
                            it.copy(
                                loginStep = LoginStep.CREDENTIALS,
                                isLoading = false,
                                qrCode = "",
                                error = result.error,
                            )
                        }
                    }

                    is LoginResult.QRCode -> {
                        Timber.d("QR CODE: ${result.qrCode}")
                        _uiState.update {
                            it.copy(
                                loginStep = LoginStep.QRCODE,
                                isLoading = true,
                                qrCode = result.qrCode,
                            )
                        }
                    }

                    is LoginResult.QRCodeEnded -> {
                        Timber.d("QR Code Ended")
                        _uiState.update {
                            it.copy(
                                loginStep = LoginStep.CREDENTIALS,
                                isLoading = false,
                                qrCode = "",
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Signs in with the saved sign-in, if there is one. This screen also comes up when a
     * session is lost while the app is in the background, where Android does not let the
     * service start; the sign-in is then left for [onScreenResumed].
     */
    private suspend fun autoSignIn() {
        val username = accountManager.username.first()
        val refreshToken = accountManager.refreshToken.first()
        if (username.isNullOrBlank() || refreshToken.isNullOrBlank()) return

        hasSavedSignIn = true

        // No internet: the attempt could only fail. It is made when the network is back.
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(username = username) }
            return
        }

        _uiState.update { it.copy(username = username, isLoading = true) }
        Timber.d("Auto Logging in ")
        signInWhenVisible = !signIn()
    }

    /** The screen is in front of the user: do the sign-in that could not start before. */
    fun onScreenResumed() {
        if (!signInWhenVisible) return
        signInWhenVisible = false

        viewModelScope.launch { autoSignIn() }
    }

    fun onQrCodeCancel() {
        connection.steamService?.auth?.handleQrCodeCancel()
    }

    fun onTwoFactorChange(code: String) {
        _uiState.update { it.copy(twoFactorCode = code) }
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.update { it.copy(username = newUsername) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun onTwoFactorSubmit() {
        val code = _uiState.value.twoFactorCode.trim()
        val pending = pendingGuardCode ?: return
        if (code.isEmpty()) return

        pendingGuardCode = null
        _uiState.update { it.copy(isLoading = true) }
        pending.complete(code)
    }

    private fun requestGuardCode(
        message: String,
        previousCodeWasIncorrect: Boolean,
    ): CompletableFuture<String> {
        Timber.i("Steam Guard code requested (previous incorrect: $previousCodeWasIncorrect)")
        val future = CompletableFuture<String>()
        pendingGuardCode = future

        _uiState.update {
            it.copy(
                loginStep = LoginStep.TWOFACTOR,
                isLoading = false,
                twoFactorCode = "",
                twoFactorMessage = message,
            )
        }
        if (previousCodeWasIncorrect) {
            viewModelScope.launch { _messages.emit("That code was incorrect, try again") }
        }
        return future
    }

    fun onSignInViaQR() {
        // The service stops itself when a session ends, so make sure it is started again.
        connection.startForegroundService()
        val service = boundService() ?: return

        _uiState.update { it.copy(loginStep = LoginStep.QRCODE, error = null) }
        service.auth.handleQRLogin()
    }

    /** Signs in without an account, to look at what Steam shows anyone. */
    fun onExplore() {
        if (!connection.startForegroundService()) return
        val service = boundService() ?: return

        _uiState.update { it.copy(error = null, isLoading = true) }
        service.auth.handleAnonymousLogin()
    }

    fun onSignInViaCredentials() {
        signIn()
    }

    /** @return false when the service could not be started, so nothing was sent. */
    private fun signIn(): Boolean {
        val state = _uiState.value

        if (state.username.isBlank()) {
            Timber.w("username is blank!")
            viewModelScope.launch { _messages.emit("Username is empty") }
            return true
        }
        if (state.password.isBlank() && !hasSavedSignIn) {
            Timber.w("password is blank!")
            viewModelScope.launch { _messages.emit("Password is empty") }
            return true
        }

        _uiState.update { it.copy(error = null) }

        // The service stops itself when a session ends, so make sure it is started again.
        if (!connection.startForegroundService()) {
            _uiState.update { it.copy(isLoading = false) }
            return false
        }
        val service = boundService() ?: return false

        service.auth.handleCredentialLogin(
            username = state.username,
            password = state.password,
            authenticator = authenticator,
        )
        return true
    }

    /** The bound service. While there is none, the screen says so and null is returned. */
    private fun boundService(): SteamService? {
        val service = connection.steamService
        if (service == null) {
            _uiState.update {
                it.copy(isLoading = false, error = "The Steam service is not ready, try again")
            }
        }
        return service
    }
}
