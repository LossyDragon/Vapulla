package `in`.dragonbra.vapulla.compose.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class ChatUiEvent {
    data object NavigateUp : ChatUiEvent()
    data class SendTypingStatus(val id: SteamID) : ChatUiEvent()
    data class SendMessage(val id: SteamID, val message: String, val emoteSet: Set<String>) :
        ChatUiEvent()
}

data class ChatState(
    val chatMessages: Map<String, List<ChatMessage>> = mapOf(),
    val currentChatSteamID: SteamID? = null,
    val emoteSet: Set<String> = setOf(),
    val emoticonData: List<Emoticon> = listOf(),
    val friend: FriendListItem? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val steamFriendDao: SteamFriendDao,
    private val emoticonDao: EmoticonDao,
    private val schemaManager: GameSchemaManager
) : ViewModel() {

    private var typingFlow = MutableSharedFlow<Unit>()
    private var typingJob: Job? = null

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _uiState = MutableSharedFlow<ChatUiEvent>()
    val uiState = _uiState.asSharedFlow()

    fun init(steamID: SteamID) {
        viewModelScope.launch {
            typingFlow
                .debounce(10.seconds)
                .collectLatest {
                    Timber.d("Cancelling: Emitting isTyping")
                    typingJob?.cancel()
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.findLivePaged(steamID.convertToUInt64()).collectLatest { list ->
                val formattedList = list.groupBy { it.formattedTs }
                _state.update { it.copy(chatMessages = formattedList) }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            steamFriendDao.findLive(steamID.convertToUInt64()).collectLatest { friend ->
                if (friend.relation == EFriendRelationship.Friend.code()) {
                    _state.update { it.copy(friend = friend) }
                    schemaManager.touch(friend.gameAppId)
                } else {
                    emit(ChatUiEvent.NavigateUp)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            emoticonDao.getLive().collectLatest { list ->
                val emoteSet = list.filter { !it.isSticker }.map { it.name }.toSet()
                _state.update { it.copy(emoticonData = list, emoteSet = emoteSet) }
            }
        }
    }

    fun onResume() {
        val steamID = _state.value.currentChatSteamID
            ?: throw IllegalArgumentException("SteamID was null in markRead")

        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.markRead(steamID.convertToUInt64())
        }
    }

    fun setChatSteamID(steamID: SteamID) {
        _state.update { it.copy(currentChatSteamID = steamID) }
    }

    fun isTyping() {
        viewModelScope.launch {
            if (typingJob?.isActive != true) {
                typingJob = typingJob()
            }
            typingFlow.emit(Unit)
        }
    }

    fun sendMessage(message: String) {
        if (message.isEmpty()) return

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

    private fun CoroutineScope.typingJob() = launch {
        val steamID = _state.value.currentChatSteamID
            ?: throw IllegalArgumentException(
                "SteamID was null trying to send a message status"
            )

        emit(ChatUiEvent.SendTypingStatus(steamID))

        while (isActive) {
            delay(10.seconds)

            if (!isActive) {
                // TODO still sends out Typing Status if we're cancelled.
                return@launch
            }

            emit(ChatUiEvent.SendTypingStatus(steamID))
        }
    }
}
