package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListGroup

data class HomeState(
    val avatarHash: String = "",
    val filteredFriendsList: List<FriendListGroup> = listOf(),
    val friendsList: List<FriendListGroup> = listOf(),
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val nickname: String = "",
    val status: EPersonaState = EPersonaState.Offline,
    val updateTime: Long = 0L
)
