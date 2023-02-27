package `in`.dragonbra.vapulla.compose.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginValidation: LoginValidation = LoginValidation()
) : ViewModel() {

    var logOnDetails = LogOnDetails()
        private set

    var loginState by mutableStateOf(LoginState())

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()
    fun onEvent(event: LoginEvent) {
        Timber.d("Login Event: ${event.javaClass}")
        when (event) {
            LoginEvent.Login -> doLogin()
            LoginEvent.ShowFailedScreen -> {
                loginState = loginState.copy(
                    generalMessage = "Failed to connect to steam",
                    isLoading = false
                )
            }

            is LoginEvent.PasswordChanged -> {
                loginState = loginState.copy(password = event.password)
            }

            is LoginEvent.PasswordVisibleChanged -> {
                loginState = loginState.copy(isPasswordVisible = event.visibility)
            }

            is LoginEvent.ShowLoading -> {
                loginState =
                    loginState.copy(isLoading = event.isLoading, generalMessage = "Loading")
            }

            is LoginEvent.ShowLoginForm -> {
                loginState = loginState.copy(generalMessage = event.error)
            }

            is LoginEvent.ShowSteamGuard -> {
                loginState = loginState.copy(
                    expectSteamGuard = event.expectSteamGuard,
                    generalMessage = event.error,
                    is2Fa = event.is2fa,
                    isLoading = false
                )
            }

            is LoginEvent.SteamGuardChanged -> {
                loginState = loginState.copy(steamGuard = event.steamGuard)
            }

            is LoginEvent.UsernameChanged -> {
                loginState = loginState.copy(username = event.username)
            }
        }
    }

    private fun doLogin() {
        val username = loginValidation.validateUsername(loginState.username)
        val password = loginValidation.validatePassword(loginState.password)

        val isError = listOf(username, password).any { !it.isSuccessful }
        if (isError) {
            loginState = loginState.copy(
                usernameError = username.errorMessage,
                passwordError = password.errorMessage
            )

            return
        }

        logOnDetails.apply {
            this.username = loginState.username
            this.password = loginState.password
            this.loginKey = null
        }

        if (loginState.expectSteamGuard) {
            val steamGuard = loginValidation.validateSteamGuard(loginState.steamGuard)
            if (!steamGuard.isSuccessful) {
                loginState = loginState.copy(
                    steamGuardError = steamGuard.errorMessage,
                    isLoading = false
                )

                return
            }

            if (loginState.is2Fa) {
                logOnDetails.twoFactorCode = loginState.steamGuard
            } else {
                logOnDetails.authCode = loginState.steamGuard
            }
        }

        viewModelScope.launch {
            val event = ValidationEvent.StartService
            loginEventChannel.send(event)
        }
    }

//    fun retry() {
//        if (account.hasLoginKey()) {
//            logOnDetails.username = account.username
//            logOnDetails.password = null
//            logOnDetails.loginKey = account.loginKey
//            startSteamService()
//        }
//    }
//
//    fun cancelSteamGuard() {
//        expectSteamGuard = false
//        ifViewAttached {
//            it.showLoginForm()
//        }
//    }
}
