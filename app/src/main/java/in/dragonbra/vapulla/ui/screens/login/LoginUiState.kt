package `in`.dragonbra.vapulla.ui.screens.login

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val qrCode: String = "",
    val isWaitingForConfirmation: Boolean = false,
    val previousCodeWasIncorrect: Boolean = false,
    val twoFactorCode: String = "",
    val twoFactorMessage: String = "",
    val refreshToken: String = "",
    val loginStep: LoginStep = LoginStep.CREDENTIALS,
    val isLoading: Boolean = false,
    val isServiceRunning: Boolean = false,
)