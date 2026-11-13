package `in`.dragonbra.vapulla.ui.screens.workshop

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.WorkshopItem
import `in`.dragonbra.vapulla.service.api.WorkshopSort
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class WorkshopUiState(
    val sort: WorkshopSort = WorkshopSort.Trending,
    val searchText: String = "",
    val items: ImmutableList<WorkshopItem> = persistentListOf(),
    val total: Int = 0,
    val nextPage: Int? = null,
    val subscribed: PersistentSet<Long> = persistentSetOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Workshop browser for one app via the `PublishedFile` service. Subscriptions toggled here
 * are remembered for the session only; Steam does not report them back in query results.
 */
class WorkshopViewModel(
    private val appId: Int,
    private val connection: ServiceConnection,
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkshopUiState())
    val uiState: StateFlow<WorkshopUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        load(reset = true)
    }

    fun setSort(sort: WorkshopSort) {
        if (sort == _uiState.value.sort) return
        _uiState.update { it.copy(sort = sort) }
        load(reset = true)
    }

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun search() = load(reset = true)

    fun loadMore() = load(reset = false)

    private fun load(reset: Boolean) {
        val state = _uiState.value
        if (!reset && (state.nextPage == null || state.isLoading)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val apis = connection.steamService?.apis
            if (apis == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            runCatching {
                apis.workshop.query(
                    appId = appId,
                    sort = state.sort,
                    page = if (reset) 1 else checkNotNull(state.nextPage),
                    searchText = state.searchText,
                    perPage = settingsManager.settings.first().workshopPageSize,
                )
            }.onSuccess { page ->
                _uiState.update {
                    val items = if (reset) page.items else (it.items + page.items).toImmutableList()
                    it.copy(
                        items = items,
                        total = page.total,
                        nextPage = page.nextPage,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                Timber.w(error, "QueryFiles failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error =
                            error.message ?: "Request failed",
                    )
                }
            }
        }
    }

    fun toggleSubscription(item: WorkshopItem) {
        viewModelScope.launch {
            val apis = connection.steamService?.apis ?: return@launch
            val subscribed = item.id in _uiState.value.subscribed

            val result = runCatching {
                if (subscribed) {
                    apis.workshop.unsubscribe(
                        appId,
                        item.id,
                    )
                } else {
                    apis.workshop.subscribe(appId, item.id)
                }
            }.getOrElse { EResult.Fail }

            if (result == EResult.OK) {
                _uiState.update {
                    val set = if (subscribed) {
                        it.subscribed.removing(item.id)
                    } else {
                        it.subscribed.adding(item.id)
                    }
                    it.copy(subscribed = set)
                }
                _messages.emit(
                    if (subscribed) "Unsubscribed from ${item.title}" else "Subscribed to ${item.title}",
                )
            } else {
                _messages.emit("Steam answered $result")
            }
        }
    }
}
