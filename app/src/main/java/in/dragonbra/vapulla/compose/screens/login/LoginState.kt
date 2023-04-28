package `in`.dragonbra.vapulla.compose.screens.login

data class LoginState(
    val expectSteamGuard: Boolean = false,
    val generalMessage: String = "",
    val is2Fa: Boolean = false,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val isSigningInViaQR: Boolean = false,
    val password: String = "",
    val passwordError: String = "",
    val refreshToken: String = "",
    val steamGuard: String = "",
    val steamGuardError: String = "",
    val username: String = "",
    val usernameError: String = ""
)
