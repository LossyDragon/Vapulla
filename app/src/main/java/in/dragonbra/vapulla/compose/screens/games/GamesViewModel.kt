package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.retrofit.response.Games
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class GamesViewModel : ViewModel() {

    private val _state = MutableStateFlow(GamesState())
    val state = _state.asStateFlow()

    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText: MutableStateFlow<TextFieldValue> = _searchText

    init {
        viewModelScope.launch {
            searchText.collectLatest {
                search(it.text)
            }
        }
    }

    fun isSearching() {
        _state.update { it.copy(isSearching = true) }
    }

    fun isNotSearching() {
        _state.update { it.copy(isSearching = false) }
    }

    fun onSortMethod() {
        Timber.d("onSortMethod()")
        when (_state.value.sortMethod) {
            SortOptions.SortAlphabetical -> {
                val sorted = _state.value.filteredGamesList.sortedBy { it.name.lowercase() }
                _state.update {
                    it.copy(
                        filteredGamesList = sorted,
                        sortMethod = SortOptions.SortPlaytime
                    )
                }
            }
            SortOptions.SortPlaytime -> {
                val sorted = _state.value.filteredGamesList.sortedWith(
                    compareBy(
                        { it.playtime_2weeks },
                        { it.playtime_forever }
                    )
                ).reversed()
                _state.update {
                    it.copy(
                        filteredGamesList = sorted,
                        sortMethod = SortOptions.SortAlphabetical
                    )
                }
            }
        }
    }

    private fun search(query: String) {
        val isSearching = state.value.isSearching && searchText.value.text.isNotEmpty()
        val list = if (isSearching) {
            state.value.gamesList.filter {
                it.name.lowercase().contains(query.trim().lowercase())
            }
        } else {
            state.value.gamesList
        }

        val sortedList = list.sortedBy { it.name.lowercase() }
        _state.update { it.copy(filteredGamesList = sortedList) }
    }

    fun setContents(name: String, items: ArrayList<Games>) {
        val list = items.sortedBy { it.name.lowercase() }
        _state.update { it.copy(name = name, gamesList = list, filteredGamesList = list) }
    }
}
