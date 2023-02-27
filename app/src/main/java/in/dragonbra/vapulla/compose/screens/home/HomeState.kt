package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.vapulla.adapter.FriendListItem

data class HomeState(
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,

    val friendsList: List<FriendListItem> = listOf(),
    val updateTime: Long = 0L,

    val nickname: String = "",
    val status: String = "",
    val avatarHash: String = "",
)
