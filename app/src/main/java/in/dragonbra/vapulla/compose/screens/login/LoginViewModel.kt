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

data class LoginValidationResult(
    val isSuccessful: Boolean,
    val errorMessage: String? = null
)

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

sealed class ValidationEvent {
    object Login : ValidationEvent()
}

sealed class LoginEvent {
    data class UsernameChanged(val username: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data class SteamGuardChanged(val steamGuard: String) : LoginEvent()
    data class PasswordVisibleChanged(val visibility: Boolean) : LoginEvent()
    object Login : LoginEvent()
}

class LoginViewModel(
    private val loginValidation: ValidateLogin = ValidateLogin(),
) : ViewModel() {

    var loginState by mutableStateOf(LoginState())

    private val loginEventChannel = Channel<ValidationEvent>()
    val loginEvents = loginEventChannel.receiveAsFlow()
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UsernameChanged ->
                loginState = loginState.copy(username = event.username)
            is LoginEvent.PasswordChanged ->
                loginState = loginState.copy(username = event.password)
            is LoginEvent.SteamGuardChanged ->
                loginState = loginState.copy(username = event.steamGuard)
            is LoginEvent.PasswordVisibleChanged ->
                loginState = loginState.copy(isPasswordVisible = event.visibility)
            LoginEvent.Login -> doLogin()
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

        // Prep for Steam Client
        val logonDetails = LogOnDetails().apply {
            this.username = loginState.username
            this.password = loginState.password
            this.loginKey = null
        }

        viewModelScope.launch {
            loginEventChannel.send(ValidationEvent.Login)
        }
    }
}