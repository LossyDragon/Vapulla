package `in`.dragonbra.vapulla.ui.screens.store

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamAccount
import `in`.dragonbra.vapulla.service.api.StorePage
import `in`.dragonbra.vapulla.service.api.StoreSearchResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The country prices are shown for until Steam has said which one the account is in. */
private const val DEFAULT_COUNTRY = "US"

/** Where the account is, as far as the store is concerned. */
private fun storeCountry(): String = SteamAccount.details.value.country.ifEmpty { DEFAULT_COUNTRY }

/**
 * @property searched the text the [results] are for; empty before the first search.
 * @property message why there are no results, when something went wrong.
 */
@Immutable
data class StoreUiState(
    val searched: String = "",
    val results: ImmutableList<StoreSearchResult> = persistentListOf(),
    val isSearching: Boolean = false,
    val message: String? = null,
)

/** Searches the Steam store by name. */
class StoreViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    /** Looks [text] up in the store; an empty one clears the results. */
    fun search(text: String) {
        val term = text.trim()

        searchJob?.cancel()
        if (term.isEmpty()) {
            _uiState.value = StoreUiState()
            return
        }

        searchJob = viewModelScope.launch {
            val store = connection.steamService?.apis?.store
            if (store == null) {
                _uiState.value = StoreUiState(searched = term, message = "Not connected to Steam")
                return@launch
            }

            _uiState.update { it.copy(searched = term, isSearching = true, message = null) }

            runCatching { store.search(term, storeCountry()) }
                .onSuccess { results ->
                    _uiState.value =
                        StoreUiState(searched = term, results = results.toImmutableList())
                }
                .onFailure { error ->
                    Timber.w(error, "Store search failed")
                    _uiState.value =
                        StoreUiState(searched = term, message = "Could not reach the Steam store")
                }
        }
    }
}

/**
 * @property inLibrary the account owns this app, so it has a page in the library too.
 * @property message why there is no page, when there is none.
 */
@Immutable
data class StorePageUiState(
    val page: StorePage? = null,
    val isLoading: Boolean = true,
    val inLibrary: Boolean = false,
    val message: String? = null,
)

/** One app's store page, from the `StoreBrowse` service. */
class StorePageViewModel(
    private val appId: Int,
    private val connection: ServiceConnection,
    appDao: SteamAppDao,
) : ViewModel() {

    private data class Loaded(
        val page: StorePage? = null,
        val loading: Boolean = true,
        val message: String? = null,
    )

    private val loaded = MutableStateFlow(Loaded())

    val uiState: StateFlow<StorePageUiState> =
        combine(loaded, appDao.findAppFlow(appId)) { loaded, stored ->
            StorePageUiState(
                page = loaded.page,
                isLoading = loaded.loading,
                // Apps Steam only told us about, such as a friend's game, have no package.
                inLibrary = stored != null && stored.packageId != Int.MAX_VALUE,
                message = loaded.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorePageUiState(),
        )

    init {
        viewModelScope.launch {
            val store = connection.steamService?.apis?.store
            if (store == null) {
                loaded.value = Loaded(loading = false, message = "Not connected to Steam")
                return@launch
            }

            val page = runCatching { store.page(appId, storeCountry()) }
                .onFailure { Timber.w(it, "Store page for $appId failed") }
                .getOrNull()

            loaded.value = Loaded(
                page = page,
                loading = false,
                message = "The store has no page for app $appId".takeIf { page == null },
            )
        }
    }
}
