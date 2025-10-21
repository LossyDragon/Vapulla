package `in`.dragonbra.vapulla.ui.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.dragonbra.vapulla.data.dao.SteamAppDao
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.data.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.manager.AccountManager
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
import java.util.EnumSet

class GamesViewModel(
    private val steamAppDao: SteamAppDao,
    private val accountManager: AccountManager,
) : ViewModel() {

    val localAccountId: StateFlow<Long?> = accountManager.steamid
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _appTypes = MutableStateFlow(emptySet<AppType>())
    val appTypes: StateFlow<Set<AppType>> = _appTypes.asStateFlow()

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
                        query = query
                    )
                }
            ).flow
        }.flatMapLatest { it }.cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateAppTypes(appType: AppType) {
        _appTypes.value = if (_appTypes.value.contains(appType)) {
            _appTypes.value - appType
        } else {
            _appTypes.value + appType
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }
}