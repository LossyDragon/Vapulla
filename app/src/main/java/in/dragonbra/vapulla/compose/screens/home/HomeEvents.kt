package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem

sealed class HomeEvent {
    data class SwipeRefresh(val isRefreshing: Boolean) : HomeEvent()
    data class Search(val isSearching: Boolean) : HomeEvent()
    data class UpdateAccount(
        val nickname: String,
        val status: String,
        val avatarHash: String
    ) : HomeEvent()

    data class UpdateFriends(
        val list: List<FriendListItem>,
        val updateTime: Long
    ) : HomeEvent()

    object RefreshFriendsList : HomeEvent()
}

sealed class HomeUiEvent {
    data class ChangeStatus(val state: EPersonaState) : HomeUiEvent()
    data class AcceptRequest(val friend: FriendListItem) : HomeUiEvent()
    data class IgnoreRequest(val friend: FriendListItem) : HomeUiEvent()
    data class BlockFriend(val friend: FriendListItem) : HomeUiEvent()
    object Disconnect : HomeUiEvent()
    object Refresh : HomeUiEvent()
}
