package `in`.dragonbra.vapulla.ui.screens.leaderboards

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.service.NotConnectedException
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.Leaderboard
import `in`.dragonbra.vapulla.service.api.LeaderboardScore
import `in`.dragonbra.vapulla.service.requestMessage
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property boards the game's leaderboards; shown while none is [selected].
 * @property message why there are no leaderboards, when something went wrong.
 * @property selected the leaderboard whose [scores] are shown.
 * @property scoresError why there are no [scores], when asking for them failed.
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
    val scoresError: String? = null,
    val friendsOnly: Boolean = false,
)

/** A game's leaderboards and the scores on one of them, via `SteamUserStats`. */
class LeaderboardsViewModel(
    private val appId: Int,
    private val connection: SteamServiceConnection,
    appDao: SteamAppDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardsUiState())
    val uiState: StateFlow<LeaderboardsUiState> = _uiState.asStateFlow()

    private var scoresJob: Job? = null

    init {
        viewModelScope.launch {
            val name = appDao.findAppName(appId)?.decodeHtml().orEmpty()
            _uiState.update { it.copy(gameName = name) }
        }

        viewModelScope.launch {
            connection.request("Leaderboards of $appId") { it.leaderboards.boards(appId) }
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
                    val message = if (error is NotConnectedException) {
                        error.requestMessage
                    } else {
                        "Could not reach the Steam community"
                    }
                    _uiState.update { it.copy(isLoading = false, message = message) }
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

    /** Fetches the scores of the selected board. A newer request replaces this one. */
    private fun loadScores() {
        scoresJob?.cancel()

        val state = _uiState.value
        val board = state.selected
        if (board == null) {
            _uiState.update { it.copy(isLoadingScores = false, scoresError = null) }
            return
        }

        scoresJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingScores = true, scoresError = null) }

            connection.request("Scores of leaderboard ${board.id}") {
                it.leaderboards.scores(appId, board, state.friendsOnly)
            }
                .onSuccess { scores ->
                    _uiState.update {
                        it.copy(scores = scores.toImmutableList(), isLoadingScores = false)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoadingScores = false, scoresError = error.requestMessage)
                    }
                }
        }
    }
}
