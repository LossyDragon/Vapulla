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

// TODO Activity di?
class ValidateLogin {
    fun validateUsername(username: String): LoginValidationResult {
        return if (username.isBlank()) {
            LoginValidationResult(false, "Username is Blank")
        } else {
            LoginValidationResult(true)
        }
    }

    fun validatePassword(password: String): LoginValidationResult {
        return if (password.isBlank()) {
            LoginValidationResult(false, "Password is Blank")
        } else if (password.length < 6) {
            LoginValidationResult(false, "Password is less than 6 characters")
        } else if (!password.any { it.isLetterOrDigit() }) {
            LoginValidationResult(false, "Password requires at least one letter or digit")
        } else {
            LoginValidationResult(true)
        }
    }

    fun validateSteamGuard(code: String): LoginValidationResult {
        return if (code.length < 5) {
            LoginValidationResult(false, "Code must be 5 characters long")
        } else {
            LoginValidationResult(true)
        }
    }
}

data class LoginValidationResult(
    val isSuccessful: Boolean,
    val errorMessage: String? = null
)

sealed class ValidationEvent {
    object StartService : ValidationEvent()
}

sealed class LoginEvent {
    data class PasswordChanged(val password: String) : LoginEvent()
    data class PasswordVisibleChanged(val visibility: Boolean) : LoginEvent()
    data class SteamGuardChanged(val steamGuard: String) : LoginEvent()
    data class UsernameChanged(val username: String) : LoginEvent()

    data class ShowLoading(val isLoading: Boolean) : LoginEvent()
    data class ShowLoginForm(val error: String?) : LoginEvent()
    data class ShowSteamGuard(
        val is2fa: Boolean,
        val expectSteamGuard: Boolean = false,
        val error: String? = null
    ) : LoginEvent()

    object Login : LoginEvent()
    object ShowFailedScreen : LoginEvent()
}

class LoginViewModel(
    private val loginValidation: ValidateLogin = ValidateLogin()
) : ViewModel() {

    var logOnDetails = LogOnDetails()
        private set

    var loginState by mutableStateOf(LoginState())

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()
    fun onEvent(event: LoginEvent) {
        Timber.d("Login Event: $event")
        when (event) {
            LoginEvent.Login -> doLogin()
            LoginEvent.ShowFailedScreen ->
                loginState = loginState.copy(
                    generalMessage = "Failed to connect to steam",
                    isLoading = false
                )
            is LoginEvent.PasswordChanged ->
                loginState = loginState.copy(password = event.password)
            is LoginEvent.PasswordVisibleChanged ->
                loginState = loginState.copy(isPasswordVisible = event.visibility)
            is LoginEvent.ShowLoading ->
                loginState =
                    loginState.copy(isLoading = event.isLoading, generalMessage = "Loading")
            is LoginEvent.ShowLoginForm ->
                loginState = loginState.copy(generalMessage = event.error)
            is LoginEvent.ShowSteamGuard ->
                loginState = loginState.copy(
                    expectSteamGuard = event.expectSteamGuard,
                    generalMessage = event.error,
                    is2Fa = event.is2fa,
                    isLoading = false
                )
            is LoginEvent.SteamGuardChanged ->
                loginState = loginState.copy(steamGuard = event.steamGuard)
            is LoginEvent.UsernameChanged ->
                loginState = loginState.copy(username = event.username)
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
