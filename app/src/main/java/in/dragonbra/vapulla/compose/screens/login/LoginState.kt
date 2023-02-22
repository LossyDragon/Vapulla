package `in`.dragonbra.vapulla.compose.screens.login

data class LoginState(
    val username: String = "",
    val usernameError: String? = null,

    val password: String = "",
    val passwordError: String? = null,

    val steamGuard: String = "",
    val steamGuardError: String? = null,

    val is2Fa: Boolean = false,
    val expectSteamGuard: Boolean = false,

    val isPasswordVisible: Boolean = false,

    val isLoading: Boolean = false,
    val generalMessage: String? = null
)
