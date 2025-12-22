package `in`.dragonbra.vapulla.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ChatViewModel(
    private val friendId: Long,
    private val messageDao: ChatMessageDao,
) : ViewModel() {

    companion object {
        data class ChatUiState(
            val messageText: String = "",
            val showEmojiKeyboard: Boolean = false,
            val isLoading: Boolean = false
        )
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages: Flow<PagingData<ChatMessage>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { messageDao.getMessagesPagingSource(friendId) }
    ).flow.cachedIn(viewModelScope)

    init {
        Timber.d("Chat Init $friendId")
        viewModelScope.launch {
            messageDao.markMessagesAsRead(friendId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared()")
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        val chatMessage = ChatMessage(
            message = message.trim(),
            timestamp = System.currentTimeMillis(),
            friendId = friendId,
            fromLocal = true,
            unread = false,
            timestampConfirmed = false
        )

        viewModelScope.launch {
            messageDao.insertMessage(chatMessage)
            _uiState.update { it.copy(messageText = "") }
        }
    }

    fun updateMessageText(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun toggleEmojiKeyboard(hide: Boolean? = null) {
        _uiState.update { it.copy(showEmojiKeyboard = hide ?: !it.showEmojiKeyboard) }
    }

    fun insertEmoji(emoji: String) {
        val currentText = _uiState.value.messageText
        _uiState.update { it.copy(messageText = currentText + emoji) }
    }
}
