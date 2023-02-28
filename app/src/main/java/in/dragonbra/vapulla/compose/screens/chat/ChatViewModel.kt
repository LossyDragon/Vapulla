package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    val chatMessageDao: ChatMessageDao,
    val steamFriendDao: SteamFriendDao,
    val emoticonDao: EmoticonDao,
    val schemaManager: GameSchemaManager
) : ViewModel()
