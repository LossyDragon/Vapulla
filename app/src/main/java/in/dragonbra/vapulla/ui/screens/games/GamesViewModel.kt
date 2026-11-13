package `in`.dragonbra.vapulla.ui.screens.games

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.download.DownloadManager
import java.util.EnumSet
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/** What the library shows about a game's download: enough for a pill and the summary. */
@Immutable
data class DownloadBadge(val name: String, val state: DownloadState, val percent: Int)

class GamesViewModel(
    private val steamAppDao: SteamAppDao,
    accountManager: AccountManager,
    downloadDao: DownloadDao,
    downloads: DownloadManager,
) : ViewModel() {

    /**
     * Downloads by app ID. Progress is cut down to whole percents here, so the list is only
     * redrawn when a number on screen actually changes, not twice a second.
     */
    val downloadBadges: StateFlow<ImmutableMap<Int, DownloadBadge>> =
        combine(
            downloadDao.observeAll(),
            downloads.progress
                .map { live -> live?.let { it.appId to (it.fraction * 100).toInt() } }
                .distinctUntilChanged(),
        ) { all, live ->
            all.associate { download ->
                val percent = when {
                    download.state == DownloadState.COMPLETED -> 100
                    live?.first == download.appId -> live.second
                    else -> (download.fraction * 100).toInt()
                }
                download.appId to DownloadBadge(download.name, download.state, percent)
            }.toImmutableMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentMapOf(),
        )

    val localAccountId: StateFlow<Long?> = accountManager.steamid
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appTypes = MutableStateFlow(persistentSetOf<AppType>())
    val appTypes: StateFlow<ImmutableSet<AppType>> = _appTypes.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val steamApps: Flow<PagingData<SteamAppListItem>> =
        combine(appTypes, searchQuery) { selectedTypes, query ->
            val typesToUse = selectedTypes.ifEmpty { setOf(AppType.game) }
            val flags = AppType.toFlags(EnumSet.copyOf(typesToUse))

            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = {
                    steamAppDao.getAllOwnedAppsPaged(
                        appType = flags,
                        query = query,
                    )
                },
            ).flow
        }.flatMapLatest { it }.cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateAppTypes(appType: AppType) {
        _appTypes.value = if (_appTypes.value.contains(appType)) {
            _appTypes.value.removing(appType)
        } else {
            _appTypes.value.adding(appType)
        }
    }

    override fun onCleared() {
        Timber.d("onCleared")
    }
}
