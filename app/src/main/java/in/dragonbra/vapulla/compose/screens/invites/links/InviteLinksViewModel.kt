package `in`.dragonbra.vapulla.compose.screens.invites.links

import androidx.lifecycle.ViewModel
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.model.InviteTokenItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InvitesState(
    val inviteTokens: List<InviteTokenItem> = listOf(),
    val loggedInSteamID: SteamID? = null,
    val loggedInUniverse: EUniverse = EUniverse.Invalid
)

class InviteLinksViewModel : ViewModel() {

    private val _state = MutableStateFlow(InvitesState())
    val state = _state.asStateFlow()

    fun onInvitesList(data: List<InviteTokenItem>) {
        _state.update { it.copy(inviteTokens = data) }
    }

    fun setLoggedInInfo(steamID: SteamID, steamUniverse: EUniverse) {
        _state.update { it.copy(loggedInSteamID = steamID, loggedInUniverse = steamUniverse) }
    }
}
