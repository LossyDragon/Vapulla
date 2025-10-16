package `in`.dragonbra.vapulla.ui.screens.login

data class LoginUiState(
    /** Account Info **/
    val username: String = "",
    val password: String = "",
    /** 2FA Info **/
    val qrCode: String = "",
    val twoFactorCode: String = "",

    val isWaitingForConfirmation: Boolean = false,
    val previousCodeWasIncorrect: Boolean = false,
    val twoFactorMessage: String = "",
    val refreshToken: String = "",
    val loginStep: LoginStep = LoginStep.CREDENTIALS,
    val isLoading: Boolean = false,
)