package `in`.dragonbra.vapulla.ui.screens.downloads

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * @property active downloading or waiting their turn, in queue order.
 * @property finished completed and failed, newest first; these stay until cleared.
 * @property progress live figures of the one download that is running, if any.
 */
@Immutable
data class DownloadsUiState(
    val active: ImmutableList<Download> = persistentListOf(),
    val paused: ImmutableList<Download> = persistentListOf(),
    val finished: ImmutableList<Download> = persistentListOf(),
    val progress: DownloadProgress? = null,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = active.isEmpty() && paused.isEmpty() && finished.isEmpty()
}

/** The download queue and its history, with the controls to pause, resume and clear. */
class DownloadsViewModel(downloadDao: DownloadDao, private val downloads: DownloadManager) :
    ViewModel() {

    val uiState: StateFlow<DownloadsUiState> =
        combine(downloadDao.observeAll(), downloads.progress) { all, progress ->
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
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadsUiState(),
        )

    fun pause(appId: Int) = downloads.pause(appId)

    fun resume(appId: Int) = downloads.resume(appId)

    fun remove(appId: Int, deleteFiles: Boolean) = downloads.remove(appId, deleteFiles)

    fun clearFinished() = downloads.clearFinished()
}
