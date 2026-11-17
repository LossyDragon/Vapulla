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
import `in`.dragonbra.vapulla.service.api.LibraryCollection
import `in`.dragonbra.vapulla.service.api.SteamCollections
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import java.util.EnumSet
import kotlinx.collections.immutable.ImmutableList
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
import kotlinx.coroutines.flow.update

/** What the library shows about a game's download: enough for a pill and the summary. */
@Immutable
data class DownloadBadge(val name: String, val state: DownloadState, val percent: Int)

/**
 * The library: the owned apps, paged out of the database and narrowed by the search text, the
 * app types picked and a collection, with what each one's download is up to.
 */
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
            downloadDao.observeApps(),
            downloads.progress
                .map { live ->
                    live?.takeIf { it.workshopId == 0L }
                        ?.let { it.appId to (it.fraction * 100).toInt() }
                }
                .distinctUntilChanged(),
        ) { all, live ->
            all.associate { download ->
                val percent = when {
                    download.state == DownloadState.COMPLETED -> 100
                    live?.first == download.appId -> live.second
                    else -> (download.fraction * 100).toInt()
                }
                val name = download.name.decodeHtml()
                download.appId to DownloadBadge(name, download.state, percent)
            }.toImmutableMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentMapOf(),
        )

    /**
     * The signed-in account's ID, which is the low half of its Steam ID and what licenses
     * name their owner by. Null until it is known.
     */
    val localAccountId: StateFlow<Int?> = accountManager.steamid
        .map { steamId -> steamId?.toInt() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appTypes = MutableStateFlow(persistentSetOf<AppType>())
    val appTypes: StateFlow<ImmutableSet<AppType>> = _appTypes.asStateFlow()

    /** The account's collections from the Steam client, to narrow the library to one. */
    val collections: StateFlow<ImmutableList<LibraryCollection>> = SteamCollections.collections

    private val pickedCollectionId = MutableStateFlow<String?>(null)

    /**
     * The collection the library is narrowed to; null for none. One that is picked and then
     * goes away, deleted on Steam or with the session, counts as none.
     */
    val collection: StateFlow<LibraryCollection?> =
        combine(collections, pickedCollectionId) { all, id -> all.find { it.id == id } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val steamApps: Flow<PagingData<SteamAppListItem>> =
        combine(appTypes, searchQuery, collection) { selectedTypes, query, collection ->
            // A collection can hold any kind of app, so with no type picked it shows them all.
            val typesToUse = when {
                selectedTypes.isNotEmpty() -> selectedTypes
                collection != null -> AppType.entries
                else -> setOf(AppType.game)
            }
            val flags = AppType.toFlags(EnumSet.copyOf(typesToUse))
            val within = collection?.appIds?.joinToString(
                ",",
                prefix = ",",
                postfix = ",",
            ).orEmpty()

            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = {
                    steamAppDao.getAllOwnedAppsPaged(
                        appType = flags,
                        query = query,
                        within = within,
                    )
                },
            ).flow
        }.flatMapLatest { it }.cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** Narrows the library to the collection with [id], or back to all of it when it is picked. */
    fun toggleCollection(id: String) {
        pickedCollectionId.update { picked -> id.takeIf { it != picked } }
    }

    /** Adds [appType] to the types shown, or takes it out when it is one of them. */
    fun toggleAppType(appType: AppType) {
        _appTypes.update { types ->
            if (appType in types) types.removing(appType) else types.adding(appType)
        }
    }
}
