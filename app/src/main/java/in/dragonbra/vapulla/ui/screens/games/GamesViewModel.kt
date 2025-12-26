package `in`.dragonbra.vapulla.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.manager.AccountManager
import java.util.EnumSet
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

class GamesViewModel(private val steamAppDao: SteamAppDao, accountManager: AccountManager) :
    ViewModel() {

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
    val steamApps: Flow<PagingData<SteamApp>> =
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
            _appTypes.value.remove(appType)
        } else {
            _appTypes.value.add(appType)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }
}
