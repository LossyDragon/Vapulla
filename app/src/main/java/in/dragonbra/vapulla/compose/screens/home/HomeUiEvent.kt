package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListItem

sealed class HomeUiEvent {
    data class AcceptRequest(val friend: FriendListItem) : HomeUiEvent()
    data class BlockFriend(val friend: FriendListItem) : HomeUiEvent()
    data class ChangeStatus(val state: EPersonaState) : HomeUiEvent()
    data class IgnoreRequest(val friend: FriendListItem) : HomeUiEvent()
    data object AddFriend : HomeUiEvent()
    data object Disconnect : HomeUiEvent()
    data object LogOut : HomeUiEvent()
    data object Refresh : HomeUiEvent()
    data object Settings : HomeUiEvent()
}
