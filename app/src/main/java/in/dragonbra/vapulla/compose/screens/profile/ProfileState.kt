package `in`.dragonbra.vapulla.compose.screens.profile

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.retrofit.response.Games

data class ProfileState(
    val isLoading: Boolean = true,

    val aliasHistory: List<String> = listOf(),
    val friend: FriendListItem? = null,
    val steamID: SteamID? = null,

    val gamesCount: Int? = 0,
    val gamesList: ArrayList<Games> = arrayListOf(),
    val levelCount: Int? = 0
)
