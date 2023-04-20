package `in`.dragonbra.vapulla.compose.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.vapulla.manager.AccountManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    private val loginValidation: LoginValidation = LoginValidation()

    var logOnDetails = LogOnDetails()
        private set

    val loginKey: String?
        get() = accountManager.loginKey

    private val _isServiceNotBound = MutableStateFlow(true)
    val isServiceNotBound = _isServiceNotBound.asStateFlow()

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()

    fun prefillInputs() {
        val username = accountManager.username ?: return
        _loginState.update { it.copy(username = username) }
    }

    fun setUsername() {
        accountManager.username = logOnDetails.username
    }

    fun resetAccountManager() {
        accountManager.clear()
    }

    fun onServiceBoundVerifyLoginDetails(hasInfo: () -> Unit) {
        if (!accountManager.loginKey.isNullOrEmpty() && !accountManager.username.isNullOrEmpty()) {
            with(logOnDetails) {
                loginKey = accountManager.loginKey
                password = null
                username = accountManager.username
            }

            onLoadingVisible(true)
            hasInfo()
        }
    }

    fun onServiceBound() {
        Timber.d("onServiceBound")
        _isServiceNotBound.value = false
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

    fun onShowSteamGuard(expectSteamGuard: Boolean, error: String, is2fa: Boolean) {
        _loginState.update {
            it.copy(
                expectSteamGuard = expectSteamGuard,
                generalMessage = error,
                is2Fa = is2fa,
                isLoading = false
            )
        }
    }

    fun onShowMessage(error: String, canRetry: Boolean) {
        _loginState.update { it.copy(generalMessage = error, isRetryVisible = canRetry) }
    }

    fun showFailedScreen() {
        _loginState.update {
            it.copy(
                expectSteamGuard = false,
                generalMessage = "Failed to connect to steam",
                isLoading = false,
                isRetryVisible = true
            )
        }
    }

    fun doRetry() {
        _loginState.update { it.copy(isRetryVisible = false) }
        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

    fun doLogin() {
        val username = loginValidation.validateUsername(_loginState.value.username)
        val password = loginValidation.validatePassword(_loginState.value.password)

        val isError = listOf(username, password).any { !it.isSuccessful }
        if (isError) {
            _loginState.update {
                it.copy(
                    usernameError = username.errorMessage,
                    passwordError = password.errorMessage
                )
            }

            return
        }

        logOnDetails.apply {
            this.username = _loginState.value.username
            this.password = _loginState.value.password
            this.loginKey = null
        }

        if (_loginState.value.expectSteamGuard) {
            val steamGuard = loginValidation.validateSteamGuard(_loginState.value.steamGuard)
            if (!steamGuard.isSuccessful) {
                _loginState.update {
                    it.copy(steamGuardError = steamGuard.errorMessage, isLoading = false)
                }

                return
            }

            if (_loginState.value.is2Fa) {
                logOnDetails.twoFactorCode = _loginState.value.steamGuard
            } else {
                logOnDetails.authCode = _loginState.value.steamGuard
            }
        }

        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }
}
