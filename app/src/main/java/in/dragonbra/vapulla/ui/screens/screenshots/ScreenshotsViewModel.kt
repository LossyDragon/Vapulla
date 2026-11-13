package `in`.dragonbra.vapulla.ui.screens.screenshots

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.Screenshot
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class ScreenshotsUiState(
    val items: ImmutableList<Screenshot> = persistentListOf(),
    val total: Int = 0,
    val nextPage: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/** The signed-in account's uploaded screenshots, loaded a page at a time. */
class ScreenshotsViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenshotsUiState())
    val uiState: StateFlow<ScreenshotsUiState> = _uiState.asStateFlow()

    init {
        load(reset = true)
    }

    fun refresh() = load(reset = true)

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
                apis.screenshots.mine(page = if (reset) 1 else checkNotNull(state.nextPage))
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
                Timber.w(error, "GetUserFiles failed")
                _uiState.update {
                    it.copy(isLoading = false, error = error.message ?: "Request failed")
                }
            }
        }
    }
}
