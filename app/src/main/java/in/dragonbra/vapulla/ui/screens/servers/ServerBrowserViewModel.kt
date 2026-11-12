package `in`.dragonbra.vapulla.ui.screens.servers

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.ERegionCode
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.GameServer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The game whose servers are listed; null until the user picks one or the library loads. */
@Immutable
data class ServerGame(val appId: Int, val name: String, val iconUrl: String)

@Immutable
data class ServerBrowserUiState(
    val game: ServerGame? = null,
    val region: ERegionCode = ERegionCode.World,
    val servers: ImmutableList<GameServer> = persistentListOf(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Server browser: `SteamMasterServer` for the list, A2S queries for the details. The game
 * picker is fed from the library the way the Steam client does it: owned games whose app
 * info carries a real `gamedir` and is not flagged `noservers`, in alphabetical order.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerBrowserViewModel(
    private val connection: ServiceConnection,
    private val settingsManager: SettingsManager,
    private val appDao: SteamAppDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerBrowserUiState())
    val uiState: StateFlow<ServerBrowserUiState> = _uiState.asStateFlow()

    private val _gameSearch = MutableStateFlow("")
    val gameSearch: StateFlow<String> = _gameSearch.asStateFlow()

    /** Candidate games for the picker, filtered by [gameSearch]. */
    val games: StateFlow<ImmutableList<ServerGame>> = _gameSearch
        .flatMapLatest { query -> appDao.findServerGames(query) }
        .map { apps -> apps.map { it.toServerGame() }.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    init {
        // Start on the most played game so the first query works without a pick.
        viewModelScope.launch {
            val mostPlayed =
                appDao.findServerGames().first().maxByOrNull { it.playtimeForever } ?: return@launch
            _uiState.update { it.copy(game = it.game ?: mostPlayed.toServerGame()) }
        }
    }

    fun setGameSearch(query: String) {
        _gameSearch.value = query
    }

    /** Picks [game] and queries straight away; the picker is the natural trigger. */
    fun selectGame(game: ServerGame) {
        _uiState.update { it.copy(game = game, servers = persistentListOf(), error = null) }
        query()
    }

    fun selectRegion(region: ERegionCode) {
        _uiState.update { it.copy(region = region) }
    }

    fun query() {
        val state = _uiState.value
        val game = state.game ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val apis = connection.steamService?.apis
            if (apis == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            val settings = settingsManager.settings.first()
            Timber.d("Querying ${settings.maxServers} servers for ${game.name} (${game.appId})")
            runCatching {
                apis.servers.query(
                    appId = game.appId,
                    region = state.region,
                    maxServers = settings.maxServers,
                    timeoutMs = settings.serverTimeoutMs,
                )
            }
                .onSuccess { servers ->
                    // Alphabetical by server name; servers that never answered A2S sit at the end.
                    val sorted = servers.sortedWith(
                        compareByDescending<GameServer> { it.info != null }
                            .thenBy { it.info?.name?.trim()?.lowercase() ?: it.address },
                    )
                    _uiState.update {
                        it.copy(servers = sorted.toImmutableList(), isLoading = false)
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Server query failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error =
                                error.message ?: "Query failed",
                        )
                    }
                }
        }
    }

    private fun SteamAppListItem.toServerGame() =
        ServerGame(appId = id, name = name, iconUrl = clientIconUrl)
}
