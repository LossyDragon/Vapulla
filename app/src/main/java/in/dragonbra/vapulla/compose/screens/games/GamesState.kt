package `in`.dragonbra.vapulla.compose.screens.games

import `in`.dragonbra.vapulla.retrofit.response.Games

data class GamesState(
    val name: String = "",
    val isSearching: Boolean = false,
    val gamesList: List<Games> = listOf(),
    val filteredGamesList: List<Games> = listOf()
)