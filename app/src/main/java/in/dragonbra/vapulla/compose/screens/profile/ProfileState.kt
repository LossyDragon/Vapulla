package `in`.dragonbra.vapulla.compose.screens.profile

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.retrofit.response.Games

data class ProfileState(
    val aliasHistory: List<String> = listOf(),
    val friend: FriendListItem? = null,
    val gamesCount: Int? = 0,
    val gamesList: ArrayList<Games> = arrayListOf(),
    val isLoading: Boolean = true,
    val levelCount: Int? = 0,
    val steamID: SteamID? = null
)
