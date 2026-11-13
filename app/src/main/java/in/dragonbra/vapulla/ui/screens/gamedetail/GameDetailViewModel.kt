package `in`.dragonbra.vapulla.ui.screens.gamedetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.AchievementProgress
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.DownloadPlan
import `in`.dragonbra.vapulla.service.download.DownloadPlanner
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
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

/**
 * @property download this game's entry in the download list, if it has one.
 * @property downloadProgress live figures, only while this game is the one downloading.
 * @property downloadPlans what can be downloaded, one per platform; empty when nothing can.
 * @property freeBytes space a download of this game could use: free space less what other
 *   unfinished downloads will still fill.
 */
@Immutable
data class GameDetailUiState(
    val app: SteamApp? = null,
    val playersNow: Int? = null,
    val achievements: AchievementProgress? = null,
    val isLoadingExtras: Boolean = true,
    val download: Download? = null,
    val downloadProgress: DownloadProgress? = null,
    val downloadPlans: ImmutableList<DownloadPlan> = persistentListOf(),
    val freeBytes: Long = 0L,
)

/**
 * One game: the stored PICS row plus two live lookups, the current player count from
 * `SteamUserStats` and achievement progress from the `Player` service, and its download.
 */
class GameDetailViewModel(
    private val appId: Int,
    appDao: SteamAppDao,
    downloadDao: DownloadDao,
    private val licenseDao: SteamLicenseDao,
    private val connection: ServiceConnection,
    private val downloads: DownloadManager,
) : ViewModel() {

    private data class Extras(
        val playersNow: Int? = null,
        val achievements: AchievementProgress? = null,
        val loading: Boolean = true,
    )

    private data class DownloadInfo(
        val download: Download?,
        val progress: DownloadProgress?,
        val freeBytes: Long,
    )

    private val extras = MutableStateFlow(Extras())

    private val storedApp = appDao.findAppFlow(appId)

    /**
     * The app info as Steam has it right now. Depots are picked from this when it arrives:
     * the stored row can predate a game update, or the depot fields added for downloads.
     */
    private val freshApp = MutableStateFlow<SteamApp?>(null)

    /** Every depot the account's licenses cover; DLC depots outside it are not downloaded. */
    private val ownedDepotIds = MutableStateFlow<Set<Int>>(emptySet())

    private val freeBytes = MutableStateFlow(0L)

    private val downloadPlans =
        combine(storedApp, freshApp, ownedDepotIds) { stored, fresh, owned ->
            (fresh ?: stored)?.let { DownloadPlanner.plans(it, owned) }.orEmpty().toImmutableList()
        }

    private val downloadInfo =
        combine(downloadDao.observe(appId), downloads.progress, freeBytes) { download, live, free ->
            DownloadInfo(download, live?.takeIf { it.appId == appId }, free)
        }

    val uiState: StateFlow<GameDetailUiState> =
        combine(storedApp, extras, downloadInfo, downloadPlans) { app, live, info, plans ->
            GameDetailUiState(
                app = app,
                playersNow = live.playersNow,
                achievements = live.achievements,
                isLoadingExtras = live.loading,
                download = info.download,
                downloadProgress = info.progress,
                downloadPlans = plans,
                freeBytes = info.freeBytes,
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

        viewModelScope.launch {
            freshApp.value = runCatching { connection.steamService?.apis?.library?.appInfo(appId) }
                .onFailure { Timber.w(it, "Fresh app info for $appId failed") }
                .getOrNull()
        }

        viewModelScope.launch(Dispatchers.IO) {
            ownedDepotIds.value = licenseDao.getAllDepotIds()
                .flatMapTo(HashSet()) { ids -> ids.split(',').mapNotNull(String::toIntOrNull) }
        }

        // Recounted whenever a download is added, moves on or goes away, since each of those
        // changes what is free or what is already spoken for.
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.observeAll().collect { freeBytes.value = downloads.availableBytes(appId) }
        }
    }

    /** Queues this game's depots for [os]. Also how a finished download is brought up to date. */
    fun startDownload(os: SteamApp.OS) {
        viewModelScope.launch {
            val app = freshApp.value ?: uiState.value.app ?: return@launch
            val plan = DownloadPlanner.plan(app, os, ownedDepotIds.value)

            if (plan == null) {
                _messages.emit("There is nothing to download for this platform")
            } else {
                downloads.enqueue(app, plan)?.let { _messages.emit(it) }
            }
        }
    }

    fun pauseDownload() = downloads.pause(appId)

    fun resumeDownload() = downloads.resume(appId)

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
