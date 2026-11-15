package `in`.dragonbra.vapulla.ui.screens.leaderboards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.Leaderboard
import `in`.dragonbra.vapulla.service.api.LeaderboardScore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @property boards the game's leaderboards; shown while none is [selected].
 * @property message why there are no leaderboards, when something went wrong.
 * @property selected the leaderboard whose [scores] are shown.
 * @property friendsOnly the scores are those of friends, not the best of everyone.
 */
@Immutable
data class LeaderboardsUiState(
    val gameName: String = "",
    val boards: ImmutableList<Leaderboard> = persistentListOf(),
    val isLoading: Boolean = true,
    val message: String? = null,
    val selected: Leaderboard? = null,
    val scores: ImmutableList<LeaderboardScore> = persistentListOf(),
    val isLoadingScores: Boolean = false,
    val friendsOnly: Boolean = false,
)

/** A game's leaderboards and the scores on one of them, via `SteamUserStats`. */
class LeaderboardsViewModel(
    private val appId: Int,
    private val connection: ServiceConnection,
    appDao: SteamAppDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardsUiState())
    val uiState: StateFlow<LeaderboardsUiState> = _uiState.asStateFlow()

    private var scoresJob: Job? = null

    init {
        viewModelScope.launch {
            val name = appDao.findAppName(appId).orEmpty()
            _uiState.update { it.copy(gameName = name) }
        }

        viewModelScope.launch {
            val api = connection.steamService?.apis?.leaderboards
            if (api == null) {
                _uiState.update { it.copy(isLoading = false, message = "Not connected to Steam") }
                return@launch
            }

            runCatching { api.boards(appId) }
                .onSuccess { boards ->
                    _uiState.update {
                        it.copy(
                            boards = boards.toImmutableList(),
                            isLoading = false,
                            message = "This game has no leaderboards".takeIf { boards.isEmpty() },
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "Leaderboards of $appId failed")
                    _uiState.update {
                        it.copy(isLoading = false, message = "Could not reach the Steam community")
                    }
                }
        }
    }

    /** Shows the scores on [board], or with null goes back to the list. */
    fun select(board: Leaderboard?) {
        _uiState.update { it.copy(selected = board, scores = persistentListOf()) }
        loadScores()
    }

    fun setFriendsOnly(friendsOnly: Boolean) {
        _uiState.update { it.copy(friendsOnly = friendsOnly, scores = persistentListOf()) }
        loadScores()
    }

    private fun loadScores() {
        scoresJob?.cancel()

        val board = _uiState.value.selected
        val api = connection.steamService?.apis?.leaderboards
        if (board == null || api == null) {
            _uiState.update { it.copy(isLoadingScores = false) }
            return
        }

        scoresJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingScores = true) }

            val scores = runCatching { api.scores(appId, board, _uiState.value.friendsOnly) }
                .onFailure { Timber.w(it, "Scores of leaderboard ${board.id} failed") }
                .getOrDefault(emptyList())

            _uiState.update { it.copy(scores = scores.toImmutableList(), isLoadingScores = false) }
        }
    }
}
