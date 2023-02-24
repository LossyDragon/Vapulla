package `in`.dragonbra.vapulla.compose.screens.login

data class LoginState(
    val username: String = "",
    val usernameError: String = "",

    val password: String = "",
    val passwordError: String = "",

    val steamGuard: String = "",
    val steamGuardError: String = "",

    val is2Fa: Boolean = false,
    val expectSteamGuard: Boolean = false,

    val generalMessage: String = "",
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false
)
