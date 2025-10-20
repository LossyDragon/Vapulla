package `in`.dragonbra.vapulla.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.LoginResult
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.CompletableFuture

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

    // private val submitChannel = Channel<String>()

    private val authenticator = object : IAuthenticator {
        override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
            Timber.i("Two-Factor, device code")
            TODO("Device Code not implemented")
        }

        override fun getEmailCode(
            email: String?,
            previousCodeWasIncorrect: Boolean
        ): CompletableFuture<String> {
            Timber.i("Two-Factor, asking for email code")
            TODO("Email Code not implemented")
        }

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
        connection.bindService()
        connection.startForegroundService()

        viewModelScope.launch {
            SteamService.isLoading.collect { loading ->
                _uiState.value = _uiState.value.copy(isLoading = loading)
            }
        }

        viewModelScope.launch {
            connection.isBound.collect { bound ->
                if (bound &&
                    (!accountManager.username.isNullOrBlank() &&
                            !accountManager.loginKey.isNullOrBlank())
                ) {
                    _uiState.update {
                        it.copy(
                            username = accountManager.username!!,
                            refreshToken = accountManager.loginKey!!,
                            isLoading = true
                        )
                    }
                    Timber.d("Auto Logging in ")
                    onSignInViaCredentials()
                }
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
                        _snackbarMessage.emit(result.error)
                    }

                    is LoginResult.QRCode -> {
                        Timber.d("QR CODE: ${result.qrCode}")
                        _uiState.value = _uiState.value.copy(
                            loginStep = LoginStep.QRCODE,
                            isLoading = true,
                            qrCode = result.qrCode
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
        super.onCleared()
        Timber.d("ViewModel Cleared")
    }

    fun onQrCodeCancel() {
        connection.steamService!!.handleQrCodeCancel()
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
        TODO()
    }

    fun onSignInViaQR() {
        _uiState.value = _uiState.value.copy(loginStep = LoginStep.QRCODE)

        connection.steamService!!.handleQRLogin()
    }

    fun onSignInViaCredentials() {
        val state = _uiState.value

        if (state.username.isBlank()) {
            Timber.w("username is blank!")
            viewModelScope.launch {
                _snackbarMessage.emit("Username is empty")
            }
            return
        }
        if (state.password.isBlank() && state.refreshToken.isBlank()) {
            Timber.w("password is blank!")
            viewModelScope.launch {
                _snackbarMessage.emit("Password is empty")
            }
            return
        }

        connection.steamService!!.handleCredentialLogin(
            username = state.username,
            password = state.password,
            refreshToken = state.refreshToken,
            authenticator = authenticator,
        )
    }
}