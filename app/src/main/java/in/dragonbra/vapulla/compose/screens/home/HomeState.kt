package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListItem

data class HomeState(
    val avatarHash: String = "",
    val filteredFriendsList: Map<String, List<FriendListItem>> = mapOf(),
    val friendsList: Map<String, List<FriendListItem>> = mapOf(),
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val nickname: String = "",
    val status: EPersonaState = EPersonaState.Offline,
    val updateTime: Long = 0L
)
