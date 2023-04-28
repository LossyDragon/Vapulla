package `in`.dragonbra.vapulla.compose.screens.login

import android.graphics.Bitmap
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

@HiltViewModel
class LoginViewModel @Inject constructor(
    val accountManager: AccountManager
) : ViewModel() {

    private val loginValidation: LoginValidation = LoginValidation()

    var twoFactorFuture: CompletableFuture<String> = CompletableFuture()
        private set

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()

    private val _qrCodeStateFlow = MutableStateFlow<Bitmap?>(null)
    val qrCodeStateFlow = _qrCodeStateFlow.asStateFlow()

    fun prefillInputs() {
        val username = accountManager.username ?: return
        _loginState.update { it.copy(username = username) }
    }

    fun onServiceBoundVerifyLoginDetails(hasInfo: () -> Unit) {
        if (!accountManager.loginKey.isNullOrEmpty() && !accountManager.username.isNullOrEmpty()) {
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
        _loginState.update { it.copy(expectSteamGuard = false) }
    }

    fun onUsernameUpdate(username: String) {
        if (_loginState.value.usernameError.isNotEmpty()) {
            _loginState.update { it.copy(usernameError = "") }
        }
        _loginState.update { it.copy(username = username) }
    }

    fun onPasswordUpdate(password: String) {
        if (_loginState.value.passwordError.isNotEmpty()) {
            _loginState.update { it.copy(passwordError = "") }
        }
        _loginState.update { it.copy(password = password) }
    }

    fun onSteamGuardUpdate(steamGuard: String) {
        if (_loginState.value.steamGuardError.isNotEmpty()) {
            _loginState.update { it.copy(steamGuardError = "") }
        }
        _loginState.update { it.copy(steamGuard = steamGuard) }
    }

    fun onPasswordVisible(isVisible: Boolean) {
        _loginState.update { it.copy(isPasswordVisible = isVisible) }
    }

    fun onLoadingVisible(isLoading: Boolean) {
        _loginState.update { it.copy(isLoading = isLoading, generalMessage = "Loading") }
    }

    fun onShowSteamGuard(expectSteamGuard: Boolean, error: String) {
        _loginState.update {
            it.copy(
                expectSteamGuard = expectSteamGuard,
                generalMessage = error,
                isLoading = false
            )
        }
    }

    fun onShowMessage(error: String) {
        _loginState.update { it.copy(generalMessage = error) }
    }

    fun onTwoFactorSubmit() {
        val code = _loginState.value.steamGuard
        twoFactorFuture.complete(code)
    }

    fun showFailedScreen() {
        _loginState.update {
            it.copy(
                expectSteamGuard = false,
                generalMessage = "Failed to connect to steam",
                isLoading = false
            )
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

        // TODO make sure our login message updates

        Timber.d("New challege URL: $challengeURL")
        _qrCodeStateFlow.value = QRCode(challengeURL).render().nativeImage() as Bitmap
    }

    fun doLoginQR() {
        _loginState.update { it.copy(isLoading = true, isSigningInViaQR = true) }
        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

    fun cancelLoginQR() {
        _loginState.update { it.copy(isLoading = false, isSigningInViaQR = false) }
        viewModelScope.launch {
            val event = ValidationEvent.CancelService
            loginEventChannel.send(event)
        }
    }

    fun doLogin() {
        val usernameValidation = loginValidation.validateUsername(_loginState.value.username)
        val passwordValidation = loginValidation.validatePassword(_loginState.value.password)

        val isError = listOf(usernameValidation, passwordValidation).any { !it.isSuccessful }
        if (isError) {
            _loginState.update {
                it.copy(
                    usernameError = usernameValidation.errorMessage,
                    passwordError = passwordValidation.errorMessage
                )
            }

            return
        }

        if (_loginState.value.expectSteamGuard) {
            val steamGuard = loginValidation.validateSteamGuard(_loginState.value.steamGuard)
            if (!steamGuard.isSuccessful) {
                _loginState.update {
                    it.copy(steamGuardError = steamGuard.errorMessage, isLoading = false)
                }

                return
            }
        }

        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }
}
