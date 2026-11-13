package `in`.dragonbra.vapulla.ui.screens.chat

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import `in`.dragonbra.vapulla.db.dao.ChatMessageDao
import `in`.dragonbra.vapulla.db.dao.EmoticonDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.composables.BBCode
import `in`.dragonbra.vapulla.util.readChatMedia
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Immutable
data class ChatUiState(
    val messageText: String = "",
    val showEmojiKeyboard: Boolean = false,
    val isLoading: Boolean = false,
    val friend: SteamFriend? = null,
    /** The friend is typing a message to us right now. */
    val isFriendTyping: Boolean = false,
    /** An image is being uploaded to this chat. */
    val isUploading: Boolean = false,
)

/**
 * Chat with one friend. Messages are paged out of the database, which the service fills:
 * recent history when the chat opens, then every message Steam pushes. Sending goes through
 * the service too, which shows the message at once and confirms it when Steam accepts it.
 */
class ChatViewModel(
    private val friendId: Long,
    private val friendDao: SteamFriendDao,
    private val messageDao: ChatMessageDao,
    emoticonDao: EmoticonDao,
    private val connection: ServiceConnection,
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Emits the text of a message that could not be sent, so the screen can offer it back. */
    private val _sendFailures = MutableSharedFlow<String>()
    val sendFailures: SharedFlow<String> = _sendFailures.asSharedFlow()

    /** One-off messages for the screen to show, such as why an upload failed. */
    private val _notices = MutableSharedFlow<String>()
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    /**
     * The account's Steam emoticons and stickers for the picker. The service stores them from
     * `EmoticonListCallback` after each sign-in, so the list is there offline as well.
     */
    val emoticons: StateFlow<ImmutableList<Emoticon>> = emoticonDao.getAll()
        .map { it.toImmutableList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    val messages: Flow<PagingData<ChatMessage>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5,
        ),
        pagingSourceFactory = { messageDao.getMessagesPagingSource(friendId) },
    ).flow
        .map { page ->
            // Parse each page's BB Code off the main thread as it loads. The rows then find
            // their text already in the cache instead of parsing during composition.
            page.map { message ->
                if (BBCode.hasMarkup(message.message)) {
                    withContext(Dispatchers.Default) { BBCode.parse(message.message) }
                }
                message
            }
        }
        .cachedIn(viewModelScope)

    init {
        Timber.d("Chat Init $friendId")

        viewModelScope.launch {
            friendDao.findFlow(friendId).collect { friend ->
                _uiState.update { it.copy(friend = friend) }
            }
        }

        viewModelScope.launch {
            connection.steamService?.apis?.chat?.loadRecent(friendId)
        }

        viewModelScope.launch {
            connection.steamService?.apis?.chat?.typing?.collect { typing ->
                _uiState.update { it.copy(isFriendTyping = friendId in typing) }
            }
        }
    }

    override fun onCleared() {
        Timber.d("onCleared()")
    }

    /**
     * Called as the chat enters and leaves the screen. While it is visible, incoming messages
     * are read on arrival instead of raising a notification.
     */
    fun onChatVisible(visible: Boolean) {
        val chat = connection.steamService?.apis?.chat

        if (!visible) {
            chat?.setActiveChat(null)
            return
        }

        chat?.setActiveChat(friendId)

        viewModelScope.launch {
            // Without a connection the messages are still marked read locally.
            chat?.ack(friendId) ?: messageDao.markMessagesAsRead(friendId)
        }
    }

    fun sendMessage(message: String) {
        val text = message.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(messageText = "") }

        viewModelScope.launch {
            val sent = connection.steamService?.apis?.chat?.send(friendId, text) ?: false
            if (!sent) {
                _sendFailures.emit(text)
            }
        }
    }

    /**
     * Uploads the picked image to this chat. Steam posts it as a message itself, which then
     * arrives through the service like one sent from another device.
     */
    fun sendImage(uri: Uri) {
        if (_uiState.value.isUploading) return
        _uiState.update { it.copy(isUploading = true) }

        viewModelScope.launch {
            val media = connection.steamService?.apis?.media

            val failure = if (media == null) {
                "Not connected to Steam"
            } else {
                runCatching {
                    val image = application.readChatMedia(uri)
                    media.sendToFriend(friendId, image.bytes, image.fileName)
                }
                    .onFailure { Timber.w(it, "Image upload failed") }
                    .exceptionOrNull()
                    ?.let { "Image not sent: ${it.message}" }
            }

            _uiState.update { it.copy(isUploading = false) }
            failure?.let { _notices.emit(it) }
        }
    }

    fun updateMessageText(text: String) {
        val changed = text != _uiState.value.messageText
        _uiState.update { it.copy(messageText = text) }

        // Every edit that leaves something in the field counts as typing; the API spaces the
        // notices out. Clearing the field (sending, deleting everything) does not.
        if (changed && text.isNotBlank()) {
            viewModelScope.launch {
                connection.steamService?.apis?.chat?.sendTyping(friendId)
            }
        }
    }

    /** Toggles the emoji keyboard, or forces it when [show] is given. */
    fun toggleEmojiKeyboard(show: Boolean? = null) {
        _uiState.update { it.copy(showEmojiKeyboard = show ?: !it.showEmojiKeyboard) }
    }

    /**
     * Sends a sticker, which in Steam chat is a message of its own: the `/sticker` slash
     * command, which Steam turns into the sticker for everyone in the conversation.
     */
    fun sendSticker(sticker: Emoticon) {
        viewModelScope.launch {
            val command = "/sticker ${sticker.name}"
            val sent = connection.steamService?.apis?.chat?.send(friendId, command) ?: false
            if (!sent) {
                _notices.emit("Sticker not sent")
            }
        }
    }
}
