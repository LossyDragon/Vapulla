package `in`.dragonbra.vapulla.compose.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.vapulla.manager.AccountManager
import io.github.g0dkar.qrcode.QRCode
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class ValidationEvent {
    data object CancelService : ValidationEvent()
    data object StartService : ValidationEvent()
    data class Message(val message: String) : ValidationEvent()
}

sealed class PasswordValidation {
    data object Valid : PasswordValidation()
    data object Empty : PasswordValidation()
    data object Length : PasswordValidation()
    data object LetterOrDigit : PasswordValidation()
}

sealed class QrState {
    data object Loading : QrState()
    data class Ready(val qrCode: QRCode) : QrState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    val accountManager: AccountManager
) : ViewModel() {

    var twoFactorFuture: CompletableFuture<String> = CompletableFuture()
        private set

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()

    var qrCodeState by mutableStateOf<QrState>(QrState.Loading)
        private set

    private val state: LoginState
        get() = loginState.value

    fun prefillInputs() {
        val username = accountManager.username ?: return
        _loginState.update { it.copy(username = username) }
    }

    fun onServiceBoundVerifyLoginDetails(hasInfo: () -> Unit) {
        if (!accountManager.loginKey.isNullOrEmpty() &&
            !accountManager.username.isNullOrEmpty() &&
            accountManager.lastLoginSuccessful
        ) {
            onLoadingVisible(true)
            hasInfo()
        }
    }

    fun onServiceBound() {
        Timber.d("onServiceBound")
        _loginState.update {
            it.copy(isServiceConnected = true)
        }
    }

    fun onDestroy() {
        _loginState.update {
            it.copy(expectSteamGuardCode = false, expectSteamGuardApp = false)
        }
    }

    fun onUsernameUpdate(username: String) {
        if (!state.isUsernameValid) {
            _loginState.update { it.copy(isUsernameValid = true) }
        }
        _loginState.update { it.copy(username = username) }
    }

    fun onPasswordUpdate(password: String) {
        if (state.isPasswordValid != PasswordValidation.Valid) {
            _loginState.update { it.copy(isPasswordValid = PasswordValidation.Valid) }
        }
        _loginState.update { it.copy(password = password) }
    }

    fun onSteamGuardUpdate(steamGuard: String) {
        if (!state.isSteamGuardValid) {
            _loginState.update { it.copy(isSteamGuardValid = true) }
        }
        _loginState.update { it.copy(steamGuard = steamGuard) }
    }

    fun onPasswordVisible(isVisible: Boolean) {
        _loginState.update { it.copy(isPasswordVisible = isVisible) }
    }

    fun onLoadingVisible(isLoading: Boolean) {
        _loginState.update { it.copy(isLoading = isLoading) }
        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.Message("Loading"))
        }
    }

    fun onShowSteamGuard(
        expectSteamGuardCode: Boolean = false,
        expectSteamGuardApp: Boolean = false,
        error: String
    ) {
        _loginState.update {
            it.copy(
                expectSteamGuardCode = expectSteamGuardCode,
                expectSteamGuardApp = expectSteamGuardApp,
                isLoading = false
            )
        }
        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.Message(error))
        }
    }

    fun onShowMessage(error: String, reLogin: Boolean = false) {
        _loginState.update { it.copy(isLoading = !reLogin) }
        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.Message(error))
        }
    }

    fun onTwoFactorSubmit() {
        val code = _loginState.value.steamGuard
        twoFactorFuture.complete(code)
    }

    fun showFailedScreen(message: String) {
        accountManager.lastLoginSuccessful = false
        _loginState.update {
            it.copy(
                expectSteamGuardCode = false,
                expectSteamGuardApp = false,
                isLoading = false
            )
        }
        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.Message(message))
        }
    }

    fun doRetry() {
        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

    fun drawQRCode(authSession: QrAuthSession) {
        val challengeURL: String = authSession.challengeUrl

        Timber.d("New challenge URL: $challengeURL")

        val qrCode = QRCode(challengeURL)
        qrCodeState = QrState.Ready(qrCode)
    }

    fun doLoginQR() {
        _loginState.update { it.copy(isLoading = true, isSigningInViaQR = true) }
        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

    fun cancelLoginQR() {
        _loginState.update {
            it.copy(
                isLoading = false,
                isSigningInViaQR = false
            )
        }
        qrCodeState = QrState.Loading
        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.CancelService)
            loginEventChannel.send(ValidationEvent.Message("QR login cancelled"))
        }
    }

    fun doLogin() {
        val isValidGuardCode = state.steamGuard.length > 5
        val isValidUsername = state.username.isNotBlank()
        val isValidPassword = when {
            state.password.isBlank() -> PasswordValidation.Empty
            state.password.length < 6 -> PasswordValidation.Length
            !state.password.any { it.isLetterOrDigit() } -> PasswordValidation.LetterOrDigit
            else -> PasswordValidation.Valid
        }

        _loginState.update {
            it.copy(
                isUsernameValid = isValidUsername,
                isPasswordValid = isValidPassword,
                isSteamGuardValid = isValidGuardCode
            )
        }

        if (!isValidUsername || isValidPassword != PasswordValidation.Valid) {
            Timber.w("Username or Password wasn't valid")
            return
        }

        if (state.expectSteamGuardCode) {
            if (!isValidGuardCode) {
                Timber.w("Steam Guard code wasn't valid")
                return
            }
        }

        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }
}
