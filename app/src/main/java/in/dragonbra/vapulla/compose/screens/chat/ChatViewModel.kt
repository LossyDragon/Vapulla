package `in`.dragonbra.vapulla.compose.screens.chat

import android.text.format.DateUtils
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.model.FriendListItem
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val steamFriendDao: SteamFriendDao,
    private val emoticonDao: EmoticonDao,
    private val schemaManager: GameSchemaManager
) : ViewModel() {

    companion object {
        const val TYPING_INTERVAL = DateUtils.SECOND_IN_MILLIS * 20
    }

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _uiState = MutableSharedFlow<ChatUiEvent>()
    val uiState = _uiState.asSharedFlow()

    private val _message = MutableStateFlow(TextFieldValue(""))
    val message: MutableStateFlow<TextFieldValue> = _message

    private val _chatMessages = MutableStateFlow<PagingData<ChatMessage>>(PagingData.empty())
    val chatMessages: Flow<PagingData<ChatMessage>> = _chatMessages.asSharedFlow()

    private lateinit var chatData: Flow<PagingData<ChatMessage>>
    private val chatObserver = Observer<PagingData<ChatMessage>> { list ->
        _chatMessages.value = list
    }

    private lateinit var emoticonData: LiveData<List<Emoticon>>
    private val emoteObserver = Observer<List<Emoticon>> { list ->
        val emoteSet = list.filter { !it.isSticker }.map { it.name }.toSet()
        _state.update { it.copy(emoticonData = list, emoteSet = emoteSet) }
    }

    private lateinit var friendData: LiveData<FriendListItem>
    private val friendObserver = Observer<FriendListItem> { friend ->
        if (friend.relation == EFriendRelationship.Friend.code()) {
            _state.update { it.copy(friend = friend) }
        } else {
            emit(ChatUiEvent.NavigateUp)
        }
    }

    fun onPostCreate(lifecycleOwner: LifecycleOwner) {
        Timber.d("onPostCreate")

        val steamID = _state.value.currentChatSteamID?.convertToUInt64()
            ?: throw IllegalArgumentException("SteamID null no onPostCreate")

        chatData = Pager(
            PagingConfig(50),
            null,
            chatMessageDao.findLivePaged(steamID).asPagingSourceFactory(Dispatchers.IO)
        ).flow.cachedIn(viewModelScope)
        chatData.asLiveData().observe(lifecycleOwner, chatObserver)

        friendData = steamFriendDao.findLive(steamID)
        friendData.observe(lifecycleOwner, friendObserver)
        val friendGameID = friendData.value?.gameAppId
        if (friendGameID != null) {
            viewModelScope.launch {
                schemaManager.touch(friendGameID)
            }
        }

        emoticonData = emoticonDao.getLive()
        emoticonData.observe(lifecycleOwner, emoteObserver)

        _state.update {
            it.copy(
                // messages = chatData,
                friend = friendData.value,
                emoticonData = emoticonData.value ?: listOf()
            )
        }
    }

    fun onResume() {
        val steamID = _state.value.currentChatSteamID
            ?: throw IllegalArgumentException("SteamID was null in markRead")

        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.markRead(steamID.convertToUInt64())
        }
    }

    fun onDestroy() {
        chatData.asLiveData().removeObserver(chatObserver)
        friendData.removeObserver(friendObserver)
        emoticonData.removeObserver(emoteObserver)
    }

    fun setChatSteamID(steamID: SteamID) {
        _state.update { it.copy(currentChatSteamID = steamID) }
    }

    // TODO isTyping
    fun isTyping() {
        if (_state.value.lastTypingMessage < System.currentTimeMillis() - TYPING_INTERVAL) {
            _state.update { it.copy(lastTypingMessage = System.currentTimeMillis()) }

            val steamID = _state.value.currentChatSteamID
                ?: throw IllegalArgumentException(
                    "SteamID was null trying to send a message status"
                )

            emit(ChatUiEvent.SendTypingStatus(steamID))
        }
    }

    fun sendMessage(message: String) {
        if (message.isEmpty()) return

        _state.update { it.copy(lastTypingMessage = 0L) }

        val steamID = _state.value.currentChatSteamID
            ?: throw IllegalArgumentException("SteamID was null trying to send a message")

        val emoteSet = _state.value.emoteSet
        emit(ChatUiEvent.SendMessage(steamID, message, emoteSet))
    }

    private fun emit(event: ChatUiEvent) {
        viewModelScope.launch {
            _uiState.emit(event)
        }
    }
}
