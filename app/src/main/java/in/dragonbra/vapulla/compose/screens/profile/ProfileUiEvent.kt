package `in`.dragonbra.vapulla.compose.screens.profile

sealed class ProfileUiEvent {
    data class SetNickName(val nickName: String) : ProfileUiEvent()
    data object BlockFriend : ProfileUiEvent()
    data object GetAliases : ProfileUiEvent()
    data object NavigateBack : ProfileUiEvent()
    data object RemoveFriend : ProfileUiEvent()
}
