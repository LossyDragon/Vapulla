package `in`.dragonbra.vapulla.compose.screens.chat

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.adapter.FriendListItem

sealed class ChatUiEvent {
    object RequestEmotes : ChatUiEvent()
    object NavigateUp : ChatUiEvent()
    data class SendTypingStatus(val id: SteamID) : ChatUiEvent()
    data class SendMessage(val id: SteamID, val message: String, val emoteSet: Set<String>) :
        ChatUiEvent()
}