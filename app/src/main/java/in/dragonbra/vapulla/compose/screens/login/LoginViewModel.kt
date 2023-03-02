package `in`.dragonbra.vapulla.compose.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginValidation: LoginValidation = LoginValidation()
) : ViewModel() {

    var logOnDetails = LogOnDetails()
        private set

    private val _loginState = MutableStateFlow(LoginState())
    val loginState = _loginState.asStateFlow()

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()

    // ViewModel stuff
    fun onEvent(event: LoginEvent) {
        Timber.d("Login Event: ${event.javaClass}")
        when (event) {
            LoginEvent.Login -> doLogin()
            LoginEvent.Retry -> doRetry()
            LoginEvent.ShowFailedScreen -> {
                _loginState.update {
                    it.copy(
                        expectSteamGuard = false,
                        generalMessage = "Failed to connect to steam",
                        isLoading = false,
                        isRetryVisible = true
                    )
                }
            }

            is LoginEvent.PasswordChanged -> {
                if (_loginState.value.passwordError.isNotEmpty()) {
                    _loginState.update { it.copy(passwordError = "") }
                }
                _loginState.update { it.copy(password = event.password) }
            }

            is LoginEvent.PasswordVisibleChanged -> {
                _loginState.update { it.copy(isPasswordVisible = event.visibility) }
            }

            is LoginEvent.ShowLoading -> {
                _loginState.update {
                    it.copy(
                        isLoading = event.isLoading,
                        generalMessage = "Loading"
                    )
                }
            }

            is LoginEvent.ShowLoginForm -> {
                _loginState.update {
                    it.copy(
                        generalMessage = event.error,
                        isRetryVisible = event.canRetry
                    )
                }
            }

            is LoginEvent.ShowSteamGuard -> {
                _loginState.update {
                    it.copy(
                        expectSteamGuard = event.expectSteamGuard,
                        generalMessage = event.error,
                        is2Fa = event.is2fa,
                        isLoading = false
                    )
                }
            }

            is LoginEvent.SteamGuardChanged -> {
                if (_loginState.value.steamGuardError.isNotEmpty()) {
                    _loginState.update { it.copy(steamGuardError = "") }
                }
                _loginState.update { it.copy(steamGuard = event.steamGuard) }
            }

            is LoginEvent.UsernameChanged -> {
                if (_loginState.value.usernameError.isNotEmpty()) {
                    _loginState.update { it.copy(usernameError = "") }
                }
                _loginState.update { it.copy(username = event.username) }
            }
        }
    }

    fun onDestroy() {
        _loginState.update { it.copy(expectSteamGuard = false) }
    }

    private fun doRetry() {
        _loginState.update { it.copy(isRetryVisible = false) }
        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

    private fun doLogin() {
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
                    it.copy(
                        steamGuardError = steamGuard.errorMessage,
                        isLoading = false
                    )
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
