package `in`.dragonbra.vapulla.compose.screens.login

sealed class ValidationEvent {
    object StartService : ValidationEvent()
    object BindService : ValidationEvent()
}

sealed class LoginEvent {
    data class PasswordChanged(val password: String) : LoginEvent()
    data class PasswordVisibleChanged(val visibility: Boolean) : LoginEvent()
    data class ShowLoading(val isLoading: Boolean) : LoginEvent()
    data class ShowLoginForm(val error: String, val canRetry: Boolean) : LoginEvent()
    data class ShowSteamGuard(
        val is2fa: Boolean,
        val expectSteamGuard: Boolean = false,
        val error: String = ""
    ) : LoginEvent()

    data class SteamGuardChanged(val steamGuard: String) : LoginEvent()
    data class UsernameChanged(val username: String) : LoginEvent()
    object Login : LoginEvent()
    object Retry : LoginEvent()
    object ShowFailedScreen : LoginEvent()
}
