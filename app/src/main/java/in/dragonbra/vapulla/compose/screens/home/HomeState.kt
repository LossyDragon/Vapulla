package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.vapulla.adapter.FriendListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HomeState(
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,

    val friendsList: MutableStateFlow<List<FriendListItem>> = MutableStateFlow(listOf()),
    val updateTime: Long = 0L,

    val nickname: String = "",
    val status: String = "",
    val avatarHash: String = ""
) {
    val list: StateFlow<List<FriendListItem>> = friendsList
}
