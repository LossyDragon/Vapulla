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

    fun setSearching(value: Boolean) {
        _state.update { it.copy(isSearching = value) }
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
