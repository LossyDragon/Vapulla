package `in`.dragonbra.vapulla.ui.screens.gamedetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.AchievementProgress
import `in`.dragonbra.vapulla.service.api.FriendsGameplay
import `in`.dragonbra.vapulla.service.api.NewsItem
import `in`.dragonbra.vapulla.service.api.SteamNews
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.DownloadPlan
import `in`.dragonbra.vapulla.service.download.DownloadPlanner
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.util.runSuspendCatching
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @property news the game's latest news posts, newest first; empty until they arrive.
 * @property friends what the account's friends have to do with the game, as rows of a label
 *   and the friends it applies to; empty when none do.
 * @property leaderboards how many leaderboards the game has.
 * @property badges the levels of the badges the account crafted from the game's trading
 *   cards, ready to show; empty when it has none.
 * @property download this game's entry in the download list, if it has one.
 * @property downloadProgress live figures, only while this game is the one downloading.
 * @property updateAvailable Steam has newer content than what was downloaded.
 * @property waitingForWifi downloads are held because they are set to Wi-Fi only.
 * @property downloadChoices what can be downloaded, and from which branches and languages.
 * @property freeBytes space a download of this game could use: free space less what other
 *   unfinished downloads will still fill.
 */
@Immutable
data class GameDetailUiState(
    val app: SteamApp? = null,
    val playersNow: Int? = null,
    val achievements: AchievementProgress? = null,
    val isLoadingExtras: Boolean = true,
    val news: ImmutableList<NewsItem> = persistentListOf(),
    val friends: ImmutableList<Pair<String, String>> = persistentListOf(),
    val leaderboards: Int = 0,
    val badges: String = "",
    val download: Download? = null,
    val downloadProgress: DownloadProgress? = null,
    val updateAvailable: Boolean = false,
    val waitingForWifi: Boolean = false,
    val downloadChoices: DownloadChoices = DownloadChoices(),
    val freeBytes: Long = 0L,
)

/**
 * What the download dialog offers.
 *
 * @property plans one per platform, for the [branch] and [language] picked; empty when
 *   there is nothing to download.
 * @property branches the branches that can be downloaded, the public one first.
 * @property lockedBranches the branches behind a password that has not been given yet.
 * @property languages the languages the app has depots of their own for, English first.
 */
@Immutable
data class DownloadChoices(
    val plans: ImmutableList<DownloadPlan> = persistentListOf(),
    val branches: ImmutableList<String> = persistentListOf(),
    val lockedBranches: ImmutableList<String> = persistentListOf(),
    val languages: ImmutableList<String> = persistentListOf(),
    val branch: String = DownloadPlanner.BRANCH,
    val language: String = DownloadPlanner.LANGUAGE,
)

/**
 * One game: the stored PICS row, its download, and what is looked up live when the screen
 * opens: the player count from `SteamUserStats`, achievement progress and friends' gameplay
 * from the `Player` service, the number of leaderboards, the app info as Steam has it now,
 * and the latest news from the Web API.
 */
class GameDetailViewModel(
    private val appId: Int,
    appDao: SteamAppDao,
    downloadDao: DownloadDao,
    private val licenseDao: SteamLicenseDao,
    private val connection: SteamServiceConnection,
    private val downloads: DownloadManager,
    private val friendDao: SteamFriendDao,
) : ViewModel() {

    private data class Extras(
        val playersNow: Int? = null,
        val achievements: AchievementProgress? = null,
        val loading: Boolean = true,
        val news: ImmutableList<NewsItem> = persistentListOf(),
        val friends: ImmutableList<Pair<String, String>> = persistentListOf(),
        val leaderboards: Int = 0,
        val badges: String = "",
    )

    private data class DownloadInfo(
        val download: Download?,
        val progress: DownloadProgress?,
        val freeBytes: Long,
        val waitingForWifi: Boolean,
    )

    /** A password-protected branch that was opened: what Steam handed out, and the password. */
    private class UnlockedBranch(
        val manifests: Map<Int, SteamApp.ManifestInfo>,
        val password: String,
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

    /** The branch and language picked in the download dialog. */
    private val selection = MutableStateFlow(DownloadPlanner.BRANCH to DownloadPlanner.LANGUAGE)

    /** The password-protected branches opened so far, by name. */
    private val unlocked = MutableStateFlow<Map<String, UnlockedBranch>>(emptyMap())

    /** The app info downloads are planned from: the newest there is, with opened branches in. */
    private val plannedApp = combine(storedApp, freshApp, unlocked) { stored, fresh, unlocked ->
        unlocked.entries.fold(fresh ?: stored) { app, (branch, opened) ->
            app?.let { DownloadPlanner.unlock(it, branch, opened.manifests) }
        }
    }

    private val downloadChoices =
        combine(plannedApp, ownedDepotIds, selection) { app, owned, picked ->
            if (app == null) return@combine DownloadChoices()
            val (branch, language) = picked

            DownloadChoices(
                plans = DownloadPlanner.plans(app, owned, branch, language).toImmutableList(),
                branches = DownloadPlanner.branches(app).toImmutableList(),
                lockedBranches = DownloadPlanner.lockedBranches(app).toImmutableList(),
                languages = DownloadPlanner.languages(app).toImmutableList(),
                branch = branch,
                language = language,
            )
        }

    private val downloadInfo =
        combine(
            downloadDao.observe(appId),
            downloads.progress,
            freeBytes,
            downloads.waitingForWifi,
        ) { download, live, free, waitingForWifi ->
            DownloadInfo(download, live?.takeIf { it.appId == appId }, free, waitingForWifi)
        }

    val uiState: StateFlow<GameDetailUiState> =
        combine(storedApp, extras, downloadInfo, downloadChoices) { app, live, info, choices ->
            GameDetailUiState(
                app = app,
                playersNow = live.playersNow,
                achievements = live.achievements,
                isLoadingExtras = live.loading,
                news = live.news,
                friends = live.friends,
                leaderboards = live.leaderboards,
                badges = live.badges,
                download = info.download,
                downloadProgress = info.progress,
                updateAvailable = info.download?.let { DownloadPlanner.hasUpdate(it, app) } == true,
                waitingForWifi = info.waitingForWifi,
                downloadChoices = choices,
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
            val players = async {
                connection.request("Player count of $appId") { it.library.currentPlayers(appId) }
                    .getOrNull()
            }
            val progress = async {
                connection.request("Achievement progress of $appId") {
                    it.library.achievementsProgress(listOf(appId))[appId]
                }.getOrNull()
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
            val gameplay = connection
                .request("Friends' gameplay for $appId") { it.library.friendsGameplay(appId) }
                .getOrNull()
                ?: return@launch

            val names = friendDao.getFriendsFlow().first().associate { it.id to it.nameOrNickname }
            extras.update { it.copy(friends = friendRows(gameplay, names)) }
        }

        // Only the number is needed here; the Leaderboards screen fetches the list itself.
        viewModelScope.launch {
            val boards = connection
                .request("Leaderboards of $appId") { it.leaderboards.boards(appId) }
                .getOrNull()
                ?: return@launch

            extras.update { it.copy(leaderboards = boards.size) }
        }

        // The account's own badges from the game's trading cards: "Level 3, foil level 1".
        viewModelScope.launch {
            val badges = connection
                .request("Badges of $appId") { it.profile.gameBadges(appId) }
                .getOrNull()
                ?: return@launch

            val text = badges.joinToString { badge ->
                if (badge.isFoil) "foil level ${badge.level}" else "level ${badge.level}"
            }
            extras.update { it.copy(badges = text.replaceFirstChar(Char::uppercase)) }
        }

        // News comes from the Web API, so it does not need the Steam session.
        viewModelScope.launch {
            runSuspendCatching { SteamNews.load(appId) }
                .onFailure { Timber.w(it, "News for $appId failed") }
                .onSuccess { news -> extras.update { it.copy(news = news.toImmutableList()) } }
        }

        viewModelScope.launch {
            freshApp.value = connection
                .request("Fresh app info for $appId") { it.library.appInfo(appId) }
                .getOrNull()
        }

        // An update starts from the branch and language the game was downloaded with. A
        // branch behind a password is opened again with the password it was downloaded with.
        viewModelScope.launch {
            val download = downloadDao.find(appId) ?: return@launch

            if (download.branchPassword.isNotEmpty()) {
                openBranch(download.branch, download.branchPassword)
            }
            selection.value = download.branch to download.language
        }

        viewModelScope.launch(Dispatchers.IO) {
            ownedDepotIds.value = licenseDao.ownedDepotIds()
        }

        // Recounted whenever a download is added, moves on or goes away, since each of those
        // changes what is free or what is already spoken for. A running download reports
        // progress many times a second, so the changes in between two counts are skipped.
        viewModelScope.launch(Dispatchers.IO) {
            downloadDao.observeAll().conflate().collect {
                freeBytes.value = downloads.availableBytes(appId)
                delay(FREE_SPACE_INTERVAL)
            }
        }
    }

    /** One row per way friends relate to the game, leaving out the ways none of them do. */
    private fun friendRows(
        gameplay: FriendsGameplay,
        names: Map<Long, String>,
    ): ImmutableList<Pair<String, String>> = listOf(
        "Playing now" to gameplay.playingNow,
        "Played recently" to gameplay.playedRecently,
        "Played before" to gameplay.playedBefore,
        "Own it" to gameplay.own,
        "Want it" to gameplay.wishlisted,
    )
        .filter { (_, friends) -> friends.isNotEmpty() }
        .map { (label, friends) -> label to nameList(friends.mapNotNull(names::get), friends.size) }
        .toImmutableList()

    /** "Ann, Bob and 3 more": the first few of [names], and how many of [total] are left out. */
    private fun nameList(names: List<String>, total: Int): String {
        val shown = names.take(NAMES_SHOWN)
        val more = total - shown.size

        return when {
            shown.isEmpty() -> "$total friend(s)"
            more > 0 -> "${shown.joinToString()} and $more more"
            else -> shown.joinToString()
        }
    }

    /**
     * Opens a branch that is behind a password and picks it. A wrong password is reported
     * through [messages].
     */
    fun unlockBranch(branch: String, password: String) {
        viewModelScope.launch {
            if (openBranch(branch, password)) {
                selectBranch(branch)
            } else {
                _messages.emit("Steam did not accept that password for $branch")
            }
        }
    }

    /** Asks Steam for the manifests of a locked [branch]; true when the password was right. */
    private suspend fun openBranch(branch: String, password: String): Boolean {
        val manifests = connection
            .request("Unlocking branch $branch of $appId") {
                it.library.unlockBranch(appId, branch, password)
            }
            .getOrNull()
            ?: return false

        unlocked.update { it + (branch to UnlockedBranch(manifests, password)) }
        return true
    }

    fun selectBranch(branch: String) = selection.update { it.copy(first = branch) }

    fun selectLanguage(language: String) = selection.update { it.copy(second = language) }

    /**
     * Queues this game's depots for [os], from the branch and in the language picked. Also
     * how a finished download is brought up to date.
     */
    fun startDownload(os: SteamApp.OS) {
        viewModelScope.launch {
            val app = plannedApp.first() ?: return@launch
            val (branch, language) = selection.value
            val plan = DownloadPlanner.plan(app, os, ownedDepotIds.value, branch, language)

            if (plan == null) {
                _messages.emit("There is nothing to download for this platform")
            } else {
                val password = unlocked.value[branch]?.password.orEmpty()
                downloads.enqueue(app, plan, password)?.let { _messages.emit(it) }
            }
        }
    }

    fun pauseDownload() = downloads.pause(appId)

    fun resumeDownload() = downloads.resume(appId)

    /** Has the downloaded files checked against Steam's; damaged ones are fetched again. */
    fun verifyDownload() = downloads.verify(appId)

    /** Showcases `SteamApps.RequestFreeLicense`; the outcome is reported through [messages]. */
    fun requestFreeLicense() {
        viewModelScope.launch {
            val result = connection
                .request("RequestFreeLicense") { it.library.requestFreeLicense(appId) }
                .getOrNull()

            _messages.emit(
                when {
                    result == null -> "Could not reach Steam"

                    result.result != EResult.OK -> "Steam answered ${result.result}"

                    result.grantedApps.isEmpty() && result.grantedPackages.isEmpty() ->
                        "Nothing granted, you probably own it already"

                    else ->
                        "Granted ${result.grantedApps.size} app(s) and " +
                            "${result.grantedPackages.size} package(s)"
                },
            )
        }
    }

    private companion object {
        /** How many friends a row names before it starts counting. */
        const val NAMES_SHOWN = 4

        /** The least time between two counts of the free space. */
        val FREE_SPACE_INTERVAL = 2.seconds
    }
}
