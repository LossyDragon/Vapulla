package `in`.dragonbra.vapulla.compose.screens.profile

import `in`.dragonbra.javasteam.types.SteamID

sealed class ProfileUiEvent {
    data class BlockFriend(val steamID: SteamID) : ProfileUiEvent()
    data class GetAliases(val steamID: SteamID) : ProfileUiEvent()
    data class RemoveFriend(val steamID: SteamID) : ProfileUiEvent()
    data class SetNickName(val steamID: SteamID, val nickName: String) : ProfileUiEvent()
    object NavigateBack : ProfileUiEvent()
}
