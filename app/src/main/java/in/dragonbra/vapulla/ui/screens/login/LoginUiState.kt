package `in`.dragonbra.vapulla.ui.screens.login

import androidx.compose.runtime.Immutable

/**
 * @property qrCode the address the QR code stands for; empty until Steam has sent one.
 * @property twoFactorCode the Steam Guard code as typed so far.
 * @property twoFactorMessage which Steam Guard code is asked for, and where to find it.
 * @property isWaitingForConfirmation the sign-in is waiting to be approved in the Steam
 *   mobile app.
 * @property error why the last sign-in attempt failed; shown until the next attempt starts.
 * @property isOffline the device has no internet connection, which is shown in place of [error].
 */
@Immutable
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val qrCode: String = "",
    val twoFactorCode: String = "",
    val twoFactorMessage: String = "",
    val isWaitingForConfirmation: Boolean = false,
    val loginStep: LoginStep = LoginStep.CREDENTIALS,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
)
