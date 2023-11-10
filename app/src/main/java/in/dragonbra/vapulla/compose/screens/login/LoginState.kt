package `in`.dragonbra.vapulla.compose.screens.login

data class LoginState(
    val expectSteamGuardCode: Boolean = false,
    val expectSteamGuardApp: Boolean = false,
    val isLoading: Boolean = false,
    val isPasswordValid: PasswordValidation = PasswordValidation.Valid,
    val isPasswordVisible: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val isSigningInViaQR: Boolean = false,
    val isSteamGuardValid: Boolean = false,
    val isUsernameValid: Boolean = true,
    val password: String = "",
    val refreshToken: String = "",
    val steamGuard: String = "",
    val username: String = ""
)
