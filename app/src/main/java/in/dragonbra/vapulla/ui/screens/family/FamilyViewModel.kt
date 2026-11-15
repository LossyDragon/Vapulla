package `in`.dragonbra.vapulla.ui.screens.family

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.Family
import `in`.dragonbra.vapulla.service.api.SharedGame
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @property family the account's Steam Family; null while loading and when it has none.
 * @property games what the family shares with the account, by name.
 * @property message why there is nothing to show, when there is not.
 */
@Immutable
data class FamilyUiState(
    val family: Family? = null,
    val games: ImmutableList<SharedGame> = persistentListOf(),
    val isLoading: Boolean = true,
    val message: String? = null,
)

/** The account's Steam Family and its shared library, via the `FamilyGroups` service. */
class FamilyViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }

            val api = connection.steamService?.apis?.family
            if (api == null) {
                _uiState.value =
                    FamilyUiState(isLoading = false, message = "Not connected to Steam")
                return@launch
            }

            runCatching {
                val family = api.family()
                family to family?.let { api.sharedLibrary(it.id) }.orEmpty()
            }
                .onSuccess { (family, games) ->
                    _uiState.value = FamilyUiState(
                        family = family,
                        games = games.toImmutableList(),
                        isLoading = false,
                        message = "This account is not in a Steam Family".takeIf { family == null },
                    )
                }
                .onFailure { error ->
                    Timber.w(error, "Loading the family failed")
                    _uiState.value = FamilyUiState(
                        isLoading = false,
                        message = error.message ?: "Could not load the family",
                    )
                }
        }
    }
}
