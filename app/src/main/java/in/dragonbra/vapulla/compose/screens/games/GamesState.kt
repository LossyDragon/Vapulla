package `in`.dragonbra.vapulla.compose.screens.games

import `in`.dragonbra.vapulla.retrofit.response.Games

sealed class SortOptions {
    data object Alphabetical : SortOptions()
    data object Playtime : SortOptions()
}

data class GamesState(
    val filteredGamesList: List<Games> = listOf(),
    val gamesList: List<Games> = listOf(),
    val isSearching: Boolean = false,
    val name: String = "",
    val sortMethod: SortOptions = SortOptions.Alphabetical
)
