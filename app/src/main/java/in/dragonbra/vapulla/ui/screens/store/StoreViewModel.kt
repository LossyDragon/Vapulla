package `in`.dragonbra.vapulla.ui.screens.store

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.NotConnectedException
import `in`.dragonbra.vapulla.service.SteamAccount
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.SteamWishlist
import `in`.dragonbra.vapulla.service.api.StorePage
import `in`.dragonbra.vapulla.service.api.StoreSearchResult
import `in`.dragonbra.vapulla.service.api.WishlistEntry
import `in`.dragonbra.vapulla.service.requestMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The country prices are shown for until Steam has said which one the account is in. */
private const val DEFAULT_COUNTRY = "US"

/** Where the account is, as far as the store is concerned. */
private fun storeCountry(): String = SteamAccount.details.value.country.ifEmpty { DEFAULT_COUNTRY }

/**
 * @property searched the text the [results] are for; empty before the first search.
 * @property message why there are no results, when something went wrong.
 * @property wishlist the account's wishlist, most wanted first; shown while nothing is
 *   searched for.
 * @property isLoadingWishlist the wishlist's rows are being fetched.
 */
@Immutable
data class StoreUiState(
    val searched: String = "",
    val results: ImmutableList<StoreSearchResult> = persistentListOf(),
    val isSearching: Boolean = false,
    val message: String? = null,
    val wishlist: ImmutableList<StoreSearchResult> = persistentListOf(),
    val isLoadingWishlist: Boolean = false,
)

/** Searches the Steam store by name, and lists the account's wishlist. */
class StoreViewModel(private val connection: SteamServiceConnection) : ViewModel() {

    private data class WishlistRows(
        val rows: ImmutableList<StoreSearchResult> = persistentListOf(),
        val loading: Boolean = true,
    )

    /** The search half of the state; the wishlist is added to it in [uiState]. */
    private val _uiState = MutableStateFlow(StoreUiState())

    private val wishlist = MutableStateFlow(WishlistRows())

    val uiState: StateFlow<StoreUiState> =
        combine(_uiState, wishlist) { state, wishlist ->
            state.copy(wishlist = wishlist.rows, isLoadingWishlist = wishlist.loading)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreUiState(isLoadingWishlist = true),
        )

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // Read at sign-in as a rule; asked for here in case that did not work out.
            if (SteamWishlist.entries.value == null) {
                connection.request("Wishlist") { it.wishlist.refresh() }
            }

            SteamWishlist.entries.collectLatest { entries -> showWishlist(entries.orEmpty()) }
        }
    }

    /**
     * Turns the wishlist's app IDs into rows with a name, a picture and a price. The store is
     * only asked when an app is new to the list; one taken off just loses its row.
     */
    private suspend fun showWishlist(entries: List<WishlistEntry>) {
        val known = wishlist.value.rows.associateBy { it.appId }
        val appIds = entries.map { it.appId }

        if (appIds.all { it in known }) {
            wishlist.value = WishlistRows(appIds.mapNotNull(known::get).toImmutableList(), false)
            return
        }

        wishlist.update { it.copy(loading = true) }
        val rows = connection.request("Wishlist items") { it.store.listing(appIds, storeCountry()) }
            .getOrNull()

        // Without an answer, what was listed and is still wished for stays.
        wishlist.value = WishlistRows(
            rows = (rows ?: appIds.mapNotNull(known::get)).toImmutableList(),
            loading = false,
        )
    }

    /** Looks [text] up in the store; an empty one clears the results. */
    fun search(text: String) {
        val term = text.trim()

        searchJob?.cancel()
        if (term.isEmpty()) {
            _uiState.value = StoreUiState()
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(searched = term, isSearching = true, message = null) }

            connection.request("Store search") { it.store.search(term, storeCountry()) }
                .onSuccess { results ->
                    _uiState.value =
                        StoreUiState(searched = term, results = results.toImmutableList())
                }
                .onFailure { error ->
                    val message = if (error is NotConnectedException) {
                        error.requestMessage
                    } else {
                        "Could not reach the Steam store"
                    }
                    _uiState.value = StoreUiState(searched = term, message = message)
                }
        }
    }
}

/**
 * @property inLibrary the account owns this app, so it has a page in the library too.
 * @property message why there is no page, when there is none.
 * @property onWishlist whether the app is on the account's wishlist; null while the wishlist
 *   is not known.
 * @property isChangingWishlist Steam is being asked to put it on the wishlist or take it off.
 */
@Immutable
data class StorePageUiState(
    val page: StorePage? = null,
    val isLoading: Boolean = true,
    val inLibrary: Boolean = false,
    val message: String? = null,
    val onWishlist: Boolean? = null,
    val isChangingWishlist: Boolean = false,
)

/** One app's store page, from the `StoreBrowse` service. */
class StorePageViewModel(
    private val appId: Int,
    private val connection: SteamServiceConnection,
    appDao: SteamAppDao,
) : ViewModel() {

    private data class Loaded(
        val page: StorePage? = null,
        val loading: Boolean = true,
        val message: String? = null,
    )

    private val loaded = MutableStateFlow(Loaded())

    private val changingWishlist = MutableStateFlow(false)

    val uiState: StateFlow<StorePageUiState> =
        combine(
            loaded,
            appDao.findAppFlow(appId),
            SteamWishlist.entries,
            changingWishlist,
        ) { loaded, stored, wishlist, changingWishlist ->
            StorePageUiState(
                page = loaded.page,
                isLoading = loaded.loading,
                // Apps Steam only told us about, such as a friend's game, have no package.
                inLibrary = stored != null && stored.packageId != SteamApp.NO_PACKAGE,
                message = loaded.message,
                onWishlist = wishlist?.any { it.appId == appId },
                isChangingWishlist = changingWishlist,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorePageUiState(),
        )

    private val _messages = MutableSharedFlow<String>()

    /** What to tell the user about a change to the wishlist that did not work out. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** Puts the app on the wishlist, or takes it off when it is on it. */
    fun toggleWishlist() {
        val onWishlist = uiState.value.onWishlist ?: return
        if (changingWishlist.value) return

        viewModelScope.launch {
            changingWishlist.value = true
            val result = connection.request("Wishlist change for $appId") {
                if (onWishlist) it.wishlist.remove(appId) else it.wishlist.add(appId)
            }
            changingWishlist.value = false

            result
                .onSuccess { if (it != EResult.OK) _messages.emit("Steam answered $it") }
                .onFailure { _messages.emit(it.requestMessage) }
        }
    }

    init {
        viewModelScope.launch {
            val result = connection.request("Store page for $appId") {
                it.store.page(appId, storeCountry())
            }
            val page = result.getOrNull()
            val notConnected = result.exceptionOrNull() as? NotConnectedException

            loaded.value = Loaded(
                page = page,
                loading = false,
                message = (notConnected?.requestMessage ?: "The store has no page for app $appId")
                    .takeIf { page == null },
            )
        }
    }
}
