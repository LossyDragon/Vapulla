package `in`.dragonbra.vapulla.ui.screens.gamedetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.AchievementProgress
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class GameDetailUiState(
    val app: SteamApp? = null,
    val playersNow: Int? = null,
    val achievements: AchievementProgress? = null,
    val isLoadingExtras: Boolean = true,
)

/**
 * One game: the stored PICS row plus two live lookups, the current player count from
 * `SteamUserStats` and achievement progress from the `Player` service.
 */
class GameDetailViewModel(
    private val appId: Int,
    appDao: SteamAppDao,
    private val connection: ServiceConnection,
) : ViewModel() {

    private data class Extras(
        val playersNow: Int? = null,
        val achievements: AchievementProgress? = null,
        val loading: Boolean = true,
    )

    private val extras = MutableStateFlow(Extras())

    val uiState: StateFlow<GameDetailUiState> = combine(appDao.findAppFlow(appId), extras) {
            app,
            live,
        ->
        GameDetailUiState(
            app = app,
            playersNow = live.playersNow,
            achievements = live.achievements,
            isLoadingExtras = live.loading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameDetailUiState(),
    )

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            val apis = connection.steamService?.apis
            if (apis == null) {
                extras.update { it.copy(loading = false) }
                return@launch
            }

            val players = async { runCatching { apis.library.currentPlayers(appId) }.getOrNull() }
            val progress = async {
                runCatching { apis.library.achievementsProgress(listOf(appId))[appId] }.getOrNull()
            }

            extras.update {
                it.copy(
                    playersNow = players.await(),
                    achievements = progress.await(),
                    loading = false,
                )
            }
        }
    }

    /** Showcases `SteamApps.RequestFreeLicense`; the outcome is reported through [messages]. */
    fun requestFreeLicense() {
        viewModelScope.launch {
            val apis = connection.steamService?.apis ?: return@launch
            val result = runCatching { apis.library.requestFreeLicense(appId) }
                .onFailure { Timber.w(it, "RequestFreeLicense failed") }
                .getOrNull()

            _messages.emit(
                when {
                    result == null -> "Could not reach Steam"

                    result.result != EResult.OK -> "Steam answered ${result.result}"

                    result.grantedApps.isEmpty() && result.grantedPackages.isEmpty() ->
                        "Nothing granted, you probably own it already"

                    else -> "Granted ${result.grantedApps.size} app(s) and ${result.grantedPackages.size} package(s)"
                },
            )
        }
    }
}
