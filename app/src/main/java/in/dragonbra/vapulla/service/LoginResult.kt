package `in`.dragonbra.vapulla.service

/** Progress of a sign-in attempt, emitted by [SteamService.loginResult]. */
sealed interface LoginResult {
    /** Credentials were accepted locally and the logon is in flight. */
    data object Loading : LoginResult

    /** Steam accepted the logon. */
    data object Success : LoginResult

    /** The QR flow finished, whether it succeeded, failed or was cancelled. */
    data object QRCodeEnded : LoginResult

    /** The logon failed; [error] is user-presentable. */
    data class Error(val error: String) : LoginResult

    /** A new challenge URL to render as a QR code. Steam rotates it periodically. */
    data class QRCode(val qrCode: String) : LoginResult
}
