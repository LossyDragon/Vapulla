package `in`.dragonbra.vapulla.ui.screens.family

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.Family
import `in`.dragonbra.vapulla.service.api.SharedGame
import `in`.dragonbra.vapulla.service.requestMessage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
class FamilyViewModel(private val connection: SteamServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }

            connection.request("Loading the family") { apis ->
                val family = apis.family.family()
                family to family?.let { apis.family.sharedLibrary(it.id) }.orEmpty()
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
                    _uiState.value =
                        FamilyUiState(isLoading = false, message = error.requestMessage)
                }
        }
    }
}
