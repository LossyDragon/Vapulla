package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem

sealed class HomeUiEvent {
    data class AcceptRequest(val friend: FriendListItem) : HomeUiEvent()
    data class BlockFriend(val friend: FriendListItem) : HomeUiEvent()
    data class ChangeStatus(val state: EPersonaState) : HomeUiEvent()
    data class IgnoreRequest(val friend: FriendListItem) : HomeUiEvent()
    object AddFriend : HomeUiEvent()
    object Disconnect : HomeUiEvent()
    object LogOut : HomeUiEvent()
    object Refresh : HomeUiEvent()
    object Settings : HomeUiEvent()
}
