package `in`.dragonbra.vapulla.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.LoginResult
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
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
 */
class LoginViewModel(
    private val connection: ServiceConnection,
    private val accountManager: AccountManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _navigateToHome = MutableSharedFlow<Boolean>()
    val navigateToHome: SharedFlow<Boolean> = _navigateToHome.asSharedFlow()

    /** Completed by [onTwoFactorSubmit] while JavaSteam waits for a Steam Guard code. */
    private var pendingGuardCode: CompletableFuture<String>? = null

    /** A saved sign-in is waiting for the screen to be in front of the user. */
    private var signInWhenVisible = false

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

            _uiState.value = _uiState.value.copy(
                loginStep = LoginStep.QRCODE,
                isWaitingForConfirmation = true,
                twoFactorCode = "",
            )

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
            SteamService.isLoading.collect { loading ->
                _uiState.value = _uiState.value.copy(isLoading = loading)
            }
        }

        viewModelScope.launch {
            connection.isBound.collect { bound ->
                if (bound) autoSignIn()
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
                    LoginResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    LoginResult.Success -> {
                        Timber.d("Logged in, navigating to home screen")
                        _uiState.value = LoginUiState() // Reset
                        _navigateToHome.emit(true)
                    }

                    is LoginResult.Error -> {
                        Timber.e(result.error)
                        _uiState.value = _uiState.value.copy(
                            loginStep = LoginStep.CREDENTIALS,
                            isLoading = false,
                            qrCode = "",
                            error = result.error,
                        )
                    }

                    is LoginResult.QRCode -> {
                        Timber.d("QR CODE: ${result.qrCode}")
                        _uiState.value = _uiState.value.copy(
                            loginStep = LoginStep.QRCODE,
                            isLoading = true,
                            qrCode = result.qrCode,
                        )
                    }

                    is LoginResult.QRCodeEnded -> {
                        Timber.d("QR Code Ended")
                        _uiState.value = _uiState.value.copy(
                            loginStep = LoginStep.CREDENTIALS,
                            isLoading = false,
                            qrCode = "",
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        Timber.d("ViewModel Cleared")
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

        _uiState.update {
            it.copy(username = username, refreshToken = refreshToken, isLoading = true)
        }
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
        connection.steamService!!.auth.handleQrCodeCancel()
    }

    fun onTwoFactorChange(code: String) {
        _uiState.value = _uiState.value.copy(twoFactorCode = code)
    }

    fun onUsernameChange(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername)
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword)
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
                previousCodeWasIncorrect = previousCodeWasIncorrect,
            )
        }
        if (previousCodeWasIncorrect) {
            viewModelScope.launch { _snackbarMessage.emit("That code was incorrect, try again") }
        }
        return future
    }

    fun onSignInViaQR() {
        _uiState.value = _uiState.value.copy(loginStep = LoginStep.QRCODE, error = null)

        // The service stops itself when a session ends, so make sure it is started again.
        connection.startForegroundService()
        connection.steamService!!.auth.handleQRLogin()
    }

    /** Signs in without an account, to look at what Steam shows anyone. */
    fun onExplore() {
        if (!connection.startForegroundService()) return

        _uiState.update { it.copy(error = null, isLoading = true) }
        connection.steamService!!.auth.handleAnonymousLogin()
    }

    fun onSignInViaCredentials() {
        signIn()
    }

    /** @return false when the service could not be started, so nothing was sent. */
    private fun signIn(): Boolean {
        val state = _uiState.value

        if (state.username.isBlank()) {
            Timber.w("username is blank!")
            viewModelScope.launch {
                _snackbarMessage.emit("Username is empty")
            }
            return true
        }
        if (state.password.isBlank() && state.refreshToken.isBlank()) {
            Timber.w("password is blank!")
            viewModelScope.launch {
                _snackbarMessage.emit("Password is empty")
            }
            return true
        }

        _uiState.update { it.copy(error = null) }

        // The service stops itself when a session ends, so make sure it is started again.
        if (!connection.startForegroundService()) {
            _uiState.update { it.copy(isLoading = false) }
            return false
        }
        connection.steamService!!.auth.handleCredentialLogin(
            username = state.username,
            password = state.password,
            authenticator = authenticator,
        )
        return true
    }
}
