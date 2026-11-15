package `in`.dragonbra.vapulla.ui.screens.achievements

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.GameStats
import `in`.dragonbra.vapulla.service.requestMessage
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class AchievementsUiState(
    val gameName: String = "",
    val stats: GameStats? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * The account's achievements and stats for one game, from `SteamUserStats.getUserStats`.
 * Nothing is stored; the list is fetched each time the screen opens or is refreshed.
 */
class AchievementsViewModel(
    private val appId: Int,
    private val appDao: SteamAppDao,
    private val connection: SteamServiceConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = appDao.findAppName(appId)?.decodeHtml().orEmpty()
            _uiState.update { it.copy(gameName = name) }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            connection.request("GetUserStats") { it.library.gameStats(appId) }
                .onSuccess { stats ->
                    _uiState.update {
                        it.copy(
                            stats = stats,
                            isLoading = false,
                            error = "Steam has no stats for this game".takeIf { stats == null },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.requestMessage) }
                }
        }
    }
}
