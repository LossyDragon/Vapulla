package `in`.dragonbra.vapulla.compose.screens.profile

sealed class ProfileUiEvent {
    data class SetNickName(val nickName: String) : ProfileUiEvent()
    object BlockFriend : ProfileUiEvent()
    object GetAliases : ProfileUiEvent()
    object NavigateBack : ProfileUiEvent()
    object RemoveFriend : ProfileUiEvent()
}
