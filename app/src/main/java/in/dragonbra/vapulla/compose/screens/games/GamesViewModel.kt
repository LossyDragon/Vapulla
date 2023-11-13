package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.retrofit.response.Game
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

    fun onSearch(isSearching: Boolean) {
        Timber.d("isSearching: $isSearching")
        _state.update { it.copy(isSearching = isSearching) }
    }

    fun onSortMethod() {
        Timber.d("onSortMethod()")
        _state.update {
            if (_state.value.sortMethod == SortOptions.Alphabetical) {
                it.copy(sortMethod = SortOptions.Playtime)
            } else {
                it.copy(sortMethod = SortOptions.Alphabetical)
            }
        }

        search(searchText.value.text)
    }

    private fun search(query: String) {
        val trimmedQuery = query.trim().lowercase()
        val isSearching = state.value.isSearching && searchText.value.text.isNotEmpty()

        val sortedList = state.value.gameList
            .let { list ->
                if (isSearching) {
                    list.filter { it.name.lowercase().contains(trimmedQuery) }
                } else {
                    list
                }
            }
            .sortedWith(
                when (state.value.sortMethod) {
                    SortOptions.Alphabetical -> compareBy { it.name.lowercase() }
                    SortOptions.Playtime -> compareByDescending<Game> { it.playtimeTwoWeeks ?: 0 }
                        .thenByDescending { it.playtimeForever }
                        .thenBy { it.name }
                }
            )

        _state.update { it.copy(filteredGameList = sortedList) }
    }

    fun setContents(name: String, items: ArrayList<Game>) {
        val list = items.sortedBy { it.name.lowercase() }
        _state.update { it.copy(name = name, gameList = list, filteredGameList = list) }
    }
}
