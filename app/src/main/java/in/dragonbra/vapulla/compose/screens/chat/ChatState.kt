package `in`.dragonbra.vapulla.compose.screens.chat

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.model.FriendListItem

data class ChatState(
    val currentChatSteamID: SteamID? = null,
    val emoteSet: Set<String> = setOf(),
    val emoticonData: List<Emoticon> = listOf(),
    val friend: FriendListItem? = null,
    val lastTypingMessage: Long = 0L
)
