package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem

data class HomeState(
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,

    val friendsList:  Map<String, List<FriendListItem>> = mapOf(),
    val filteredFriendsList:  Map<String, List<FriendListItem>> = mapOf(),
    val updateTime: Long = 0L,

    val nickname: String = "",
    val status: EPersonaState = EPersonaState.Offline,
    val avatarHash: String = ""
)
