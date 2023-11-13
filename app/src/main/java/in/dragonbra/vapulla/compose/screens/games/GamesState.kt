package `in`.dragonbra.vapulla.compose.screens.games

import `in`.dragonbra.vapulla.retrofit.response.Game

sealed class SortOptions {
    data object Alphabetical : SortOptions()
    data object Playtime : SortOptions()
}

data class GamesState(
    val filteredGameList: List<Game> = listOf(),
    val gameList: List<Game> = listOf(),
    val isSearching: Boolean = false,
    val name: String = "",
    val sortMethod: SortOptions = SortOptions.Alphabetical
)
