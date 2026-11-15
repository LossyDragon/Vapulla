package `in`.dragonbra.vapulla.service

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What Steam tells the client about its own account after it signs in. Each part arrives in
 * a message of its own, so a field is empty (or null) until its message has come.
 *
 * @property country two-letter code of the country Steam places the account in.
 * @property hasWallet null until Steam has said whether the account has a wallet.
 * @property walletCents the balance in hundredths of [walletCurrency], whatever the currency.
 * @property walletPendingCents money on its way into the wallet, in the same unit.
 * @property walletCurrency ISO code of the wallet's currency, e.g. "USD".
 * @property emailValidated null until Steam has sent the email details.
 * @property customUrl the name in the profile's custom URL; empty when it has none.
 * @property playingElsewhere another device signed in to the account is in a game, which
 *   is why Steam would not let this one start one.
 * @property playingElsewhereAppId the game that other device is in; 0 when Steam does not say.
 * @property presenceGameAppId the game the account's own presence shows; 0 for none. This
 *   app never plays anything, so a game here is one running on another device.
 * @property presenceGameName that game's name when the presence carries one, as it does
 *   for a game from outside Steam.
 */
@Immutable
data class AccountDetails(
    val country: String = "",
    val authorizedComputers: Int = 0,
    val hasWallet: Boolean? = null,
    val walletCents: Long = 0,
    val walletPendingCents: Long = 0,
    val walletCurrency: String = "",
    val email: String = "",
    val emailValidated: Boolean? = null,
    val customUrl: String = "",
    val playingElsewhere: Boolean = false,
    val playingElsewhereAppId: Int = 0,
    val presenceGameAppId: Int = 0,
    val presenceGameName: String = "",
)

/** Steam's answer to a request to change the profile name. */
data class NameChange(val result: EResult, val name: String)

/**
 * Process-wide record of the signed-in account, for the Account screen. Written by
 * [SteamConnection] from the `SteamUser` callbacks. Nothing here is persisted.
 */
object SteamAccount {

    private val _details = MutableStateFlow(AccountDetails())

    /** The current session's account details; all empty while signed out. */
    val details: StateFlow<AccountDetails> = _details.asStateFlow()

    private val _nameChanges = MutableSharedFlow<NameChange>(extraBufferCapacity = 1)

    /** Steam's answers to profile name changes, as they arrive. */
    val nameChanges: SharedFlow<NameChange> = _nameChanges.asSharedFlow()

    internal fun update(change: (AccountDetails) -> AccountDetails) = _details.update(change)

    internal fun nameChanged(change: NameChange) {
        _nameChanges.tryEmit(change)
    }

    internal fun signedOut() {
        _details.value = AccountDetails()
    }
}
