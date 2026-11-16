package `in`.dragonbra.vapulla.ui.screens.remote

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.RemoteApp
import `in`.dragonbra.vapulla.service.api.RemoteClient
import `in`.dragonbra.vapulla.service.requestMessage
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property selectedId the client whose [games] are shown; null until the clients are known.
 * @property games the selected client's games, by name.
 */
@Immutable
data class RemoteClientsUiState(
    val clients: ImmutableList<RemoteClient> = persistentListOf(),
    val selectedId: Long? = null,
    val games: ImmutableList<RemoteApp> = persistentListOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val selected: RemoteClient? get() = clients.firstOrNull { it.id == selectedId }
}

/**
 * The account's Steam clients on other machines and the games on the one picked, via the
 * `ClientComm` service. While the screen is in front the games are asked for again every
 * so often, sooner while one of them is downloading, which is how the progress moves.
 */
class RemoteClientsViewModel(private val connection: SteamServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteClientsUiState())
    val uiState: StateFlow<RemoteClientsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var watchJob: Job? = null

    init {
        refresh()
    }

    /** Asks for the clients again, and for the games of the one picked. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            connection.request("GetAllClientLogonInfo") { it.remoteClients.clients() }
                .onSuccess { clients ->
                    // Keep the client that was picked while it is still there.
                    val picked = _uiState.value.selectedId
                    val selectedId = clients.firstOrNull { it.id == picked }?.id
                        ?: clients.firstOrNull()?.id

                    _uiState.update {
                        it.copy(clients = clients.toImmutableList(), selectedId = selectedId)
                    }
                    loadGames()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.requestMessage) }
                }
        }
    }

    /** Shows the games of another client. */
    fun select(clientId: Long) {
        if (clientId == _uiState.value.selectedId) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(selectedId = clientId, games = persistentListOf(), isLoading = true)
            }
            loadGames()
        }
    }

    /** The screen is in front: keep the games up to date until [stopWatching]. */
    fun startWatching() {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            while (true) {
                val downloading = _uiState.value.games.any { it.changing }
                delay(if (downloading) REFRESH_DOWNLOADING else REFRESH_IDLE)

                // Not on top of a load that is under way.
                if (loadJob?.isActive != true) loadGames(quietly = true)
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
    }

    /** @param quietly true for a refresh in the background, which shows no progress or error. */
    private suspend fun loadGames(quietly: Boolean = false) {
        val clientId = _uiState.value.selectedId
        if (clientId == null) {
            _uiState.update { it.copy(games = persistentListOf(), isLoading = false) }
            return
        }

        val games = connection.request("GetClientAppList") { it.remoteClients.games(clientId) }

        // Another client was picked while Steam was answering.
        if (_uiState.value.selectedId != clientId) return

        games
            .onSuccess { answer ->
                if (answer != null) {
                    _uiState.update {
                        it.copy(games = answer.toImmutableList(), isLoading = false, error = null)
                    }
                } else if (!quietly) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "The client did not answer")
                    }
                }
            }
            .onFailure { error ->
                if (!quietly) {
                    _uiState.update { it.copy(isLoading = false, error = error.requestMessage) }
                }
            }
    }

    private companion object {
        // What Steam suggests in its answers: 10 s while updating, 60 s otherwise.
        val REFRESH_DOWNLOADING = 10.seconds
        val REFRESH_IDLE = 60.seconds
    }
}
