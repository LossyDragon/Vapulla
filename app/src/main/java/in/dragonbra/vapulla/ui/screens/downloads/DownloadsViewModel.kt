package `in`.dragonbra.vapulla.ui.screens.downloads

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.DownloadPlanner
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.service.download.InstallLocation
import `in`.dragonbra.vapulla.service.download.MoveProgress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * @property active downloading or waiting their turn, in queue order.
 * @property finished completed and failed, newest first; these stay until cleared.
 * @property progress live figures of the one download that is running, if any.
 * @property updates app IDs of the finished downloads Steam has newer content for.
 * @property moving the finished download that is being moved to another location, if any.
 * @property waitingForWifi the queue is held because downloads are set to Wi-Fi only.
 */
@Immutable
data class DownloadsUiState(
    val active: ImmutableList<Download> = persistentListOf(),
    val paused: ImmutableList<Download> = persistentListOf(),
    val finished: ImmutableList<Download> = persistentListOf(),
    val progress: DownloadProgress? = null,
    val updates: ImmutableSet<Int> = persistentSetOf(),
    val moving: MoveProgress? = null,
    val waitingForWifi: Boolean = false,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = active.isEmpty() && paused.isEmpty() && finished.isEmpty()
}

/** The download queue and its history, with the controls to pause, resume and clear. */
class DownloadsViewModel(
    downloadDao: DownloadDao,
    appDao: SteamAppDao,
    private val downloads: DownloadManager,
) : ViewModel() {

    /**
     * The finished downloads that are out of date. Steam's catalogue is only read for games
     * that completed, and again whenever one of those changes in it.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val updates: Flow<ImmutableSet<Int>> = downloadDao.observeAll()
        .map { all -> all.filter { it.state == DownloadState.COMPLETED && !it.isWorkshop } }
        .distinctUntilChanged()
        .flatMapLatest { completed ->
            appDao.findAppsFlow(completed.map { it.appId }).map { apps ->
                val byId = apps.associateBy { it.id }
                completed
                    .filter { DownloadPlanner.hasUpdate(it, byId[it.appId]) }
                    .map { it.appId }
                    .toImmutableSet()
            }
        }

    val uiState: StateFlow<DownloadsUiState> =
        combine(
            downloadDao.observeAll(),
            downloads.progress,
            downloads.waitingForWifi,
            updates,
            downloads.moving,
        ) { all, progress, waitingForWifi, updates, moving ->
            DownloadsUiState(
                active = all
                    .filter {
                        it.state == DownloadState.DOWNLOADING || it.state == DownloadState.QUEUED
                    }
                    // The running one first; the rest are already oldest first.
                    .sortedByDescending { it.state == DownloadState.DOWNLOADING }
                    .toImmutableList(),
                paused = all.filter { it.state == DownloadState.PAUSED }.toImmutableList(),
                finished = all
                    .filter { it.state.isFinished }
                    .sortedByDescending { maxOf(it.finishedAt, it.addedAt) }
                    .toImmutableList(),
                progress = progress,
                updates = updates,
                moving = moving,
                waitingForWifi = waitingForWifi,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadsUiState(),
        )

    fun pause(download: Download) = downloads.pause(download.appId, download.workshopId)

    fun resume(download: Download) = downloads.resume(download.appId, download.workshopId)

    fun verify(download: Download) = downloads.verify(download.appId, download.workshopId)

    fun remove(download: Download, deleteFiles: Boolean) =
        downloads.remove(download.appId, download.workshopId, deleteFiles)

    fun clearFinished() = downloads.clearFinished()

    /** Where a finished download can be moved to. Fixed while the screen is open. */
    val installLocations: StateFlow<ImmutableList<InstallLocation>> =
        flow { emit(downloads.installLocations().toImmutableList()) }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), persistentListOf())

    /** Moves the files of a finished [download] to the install location [toRoot]. */
    fun move(download: Download, toRoot: String) =
        downloads.move(download.appId, download.workshopId, toRoot)
}
