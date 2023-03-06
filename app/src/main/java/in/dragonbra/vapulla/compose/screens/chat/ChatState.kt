package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.paging.PagingData
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.model.FriendListItem
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class ChatState(
    val lastTypingMessage: Long = 0L,
    val currentChatSteamID: SteamID? = null,

    val friend: FriendListItem? = null,
    val messages: Flow<PagingData<ChatMessage>> = flowOf(),
    val emoticonData: List<Emoticon> = listOf()
)
