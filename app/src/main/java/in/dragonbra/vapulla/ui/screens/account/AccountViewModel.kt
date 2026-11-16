package `in`.dragonbra.vapulla.ui.screens.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.AccountDetails
import `in`.dragonbra.vapulla.service.NotConnectedException
import `in`.dragonbra.vapulla.service.SteamAccount
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.FriendInvite
import `in`.dragonbra.vapulla.service.requestMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property name the profile name, as last confirmed by Steam.
 * @property details what Steam has sent about the account in this session.
 * @property isLoadingInvites the invite links are being fetched, made or revoked.
 * @property playingElsewhere the game another device on the account is in, by name; null
 *   when none is.
 */
@Immutable
data class AccountUiState(
    val name: String = "",
    val steamId: Long = 0,
    val details: AccountDetails = AccountDetails(),
    val invites: ImmutableList<FriendInvite> = persistentListOf(),
    val isLoadingInvites: Boolean = true,
    val playingElsewhere: String? = null,
)

/**
 * The signed-in account: what the `SteamUser` callbacks say about it, changing the profile
 * name through `SteamFriends`, and friend invite links from the `UserAccount` service.
 */
class AccountViewModel(
    private val connection: SteamServiceConnection,
    account: AccountManager,
    appDao: SteamAppDao,
) : ViewModel() {

    private data class Invites(
        val list: ImmutableList<FriendInvite> = persistentListOf(),
        val loading: Boolean = true,
    )

    private val invites = MutableStateFlow(Invites())

    /**
     * The game being played on another device, named from the library where it is known.
     * Steam says so in two ways: the account's own presence shows the game, and a message
     * says playing is blocked here. Either is enough, and the presence is the better source
     * for which game it is.
     */
    private val playingElsewhere = SteamAccount.details
        .map { details ->
            val appId = details.presenceGameAppId.takeIf { it > 0 } ?: details.playingElsewhereAppId
            val playing = appId > 0 ||
                details.presenceGameName.isNotEmpty() ||
                details.playingElsewhere

            Triple(playing, appId, details.presenceGameName)
        }
        .distinctUntilChanged()
        .map { (playing, appId, presenceName) ->
            when {
                !playing -> null
                presenceName.isNotEmpty() -> presenceName
                appId <= 0 -> "A game"
                else -> appDao.findAppName(appId)?.ifEmpty { null } ?: "App $appId"
            }
        }

    val uiState: StateFlow<AccountUiState> =
        combine(
            account.accountName,
            account.steamid,
            SteamAccount.details,
            invites,
            playingElsewhere,
        ) { name, steamId, details, invites, playingElsewhere ->
            AccountUiState(
                name = name.orEmpty(),
                steamId = steamId ?: 0,
                details = details,
                invites = invites.list,
                isLoadingInvites = invites.loading,
                playingElsewhere = playingElsewhere,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountUiState(),
        )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        refresh()

        // The answer to a name change is not tied to the request, so it is listened for.
        viewModelScope.launch {
            SteamAccount.nameChanges.collect { change ->
                _messages.emit(
                    if (change.result == EResult.OK) {
                        "Profile name changed to ${change.name}"
                    } else {
                        "Steam did not change the name: ${change.result}"
                    },
                )
            }
        }
    }

    /**
     * Fetches the invite links again and asks for the notification counts again. The other
     * details arrive on their own.
     */
    fun refresh() {
        viewModelScope.launch {
            invites.update { it.copy(loading = true) }

            // The counts are answered through SteamAccount.details, whenever Steam does.
            connection.request("NotificationCounts") { it.notifications.refresh() }

            // A failed request leaves the list as it was.
            val list = connection.request("GetFriendInviteTokens") { it.invites.invites() }
                .getOrNull()

            invites.update {
                Invites(list = list?.toImmutableList() ?: it.list, loading = false)
            }
        }
    }

    /** Asks Steam for a new profile name; the outcome is reported through [messages]. */
    fun changeName(name: String) {
        viewModelScope.launch {
            val wanted = name.trim()
            if (wanted.length !in NAME_LENGTH) {
                _messages.emit("A profile name has 2 to 32 characters")
                return@launch
            }

            connection.request("SetPersonaName") { it.friends.setPersonaName(wanted) }
                .onFailure { _messages.emit(it.requestMessage) }
        }
    }

    /** Makes a new invite link and shows it in the list. */
    fun createInvite() {
        viewModelScope.launch {
            invites.update { it.copy(loading = true) }
            val result = connection.request("CreateFriendInviteToken") { it.invites.create() }

            if (result.getOrNull() == null) {
                invites.update { it.copy(loading = false) }
                _messages.emit(
                    if (result.exceptionOrNull() is NotConnectedException) {
                        "Not connected to Steam"
                    } else {
                        "Steam did not make an invite link"
                    },
                )
            } else {
                refresh()
            }
        }
    }

    /** Makes [invite] stop working; the outcome is reported through [messages]. */
    fun revokeInvite(invite: FriendInvite) {
        viewModelScope.launch {
            val result = connection.request("RevokeFriendInviteToken") {
                it.invites.revoke(invite.token)
            }.getOrElse { EResult.Fail }

            if (result == EResult.OK) {
                _messages.emit("Invite link revoked")
                refresh()
            } else {
                _messages.emit("Steam answered $result")
            }
        }
    }

    private companion object {
        /** What Steam accepts as a profile name. */
        val NAME_LENGTH = 2..32
    }
}
