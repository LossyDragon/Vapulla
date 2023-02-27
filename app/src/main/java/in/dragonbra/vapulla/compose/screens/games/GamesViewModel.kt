package `in`.dragonbra.vapulla.compose.screens.games

import androidx.lifecycle.ViewModel
import `in`.dragonbra.vapulla.retrofit.response.Games
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GamesState(
    val sortDirection: Int = GamesActivity.SORT_ALPHABETICAL,
    val name: String = "",
    val gamesList: List<Games> = listOf()
)

class GamesViewModel : ViewModel() {

    private val _state = MutableStateFlow(GamesState())
    val state = _state.asStateFlow()

    fun sortList(direction: Int) {
        _state.update { it.copy(sortDirection = direction) }
    }

    fun search(query: String) {
        TODO()
    }

    fun setContents(name: String, items: List<Games>) {
        _state.update { it.copy(name = name, gamesList = items) }
    }
}