package `in`.dragonbra.vapulla.ui.screens.groupchat

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.EmoticonDao
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.ChatGroupMember
import `in`.dragonbra.vapulla.service.api.ChatRoomInfo
import `in`.dragonbra.vapulla.service.api.ChatRoomsApi
import `in`.dragonbra.vapulla.service.api.GroupChatMessage
import `in`.dragonbra.vapulla.service.api.GroupReactionChange
import `in`.dragonbra.vapulla.service.api.MessageReaction
import `in`.dragonbra.vapulla.service.requestMessage
import `in`.dragonbra.vapulla.ui.composables.BBCode
import `in`.dragonbra.vapulla.ui.screens.chat.sendChatImage
import `in`.dragonbra.vapulla.util.runSuspendCatching
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
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
data class GroupChatUiState(
    val groupName: String = "",
    val roomName: String = "",
    val chatId: Long = 0,
    /** Every channel of the group, this one included. */
    val rooms: ImmutableList<ChatRoomInfo> = persistentListOf(),
    /** Newest first, for the reversed list. */
    val messages: ImmutableList<GroupChatMessage> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    /** Steam has older messages than the ones loaded. */
    val hasOlder: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    /** The group's members, highest rank first; empty until [GroupChatViewModel.loadMembers]. */
    val members: ImmutableList<ChatGroupMember> = persistentListOf(),
    val isLoadingMembers: Boolean = false,
)

/**
 * One channel of a group chat. Unlike friend chats nothing is stored: the latest page of
 * history is fetched on opening, older pages as the list is scrolled up, and messages and
 * reactions Steam pushes while the channel is open are added as they arrive.
 */
class GroupChatViewModel(
    private val groupId: Long,
    private val chatId: Long,
    groupName: String,
    roomName: String,
    emoticonDao: EmoticonDao,
    private val connection: SteamServiceConnection,
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GroupChatUiState(groupName = groupName, roomName = roomName, chatId = chatId),
    )
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

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

    private val chatRooms: ChatRoomsApi?
        get() = connection.steamService?.apis?.chatRooms

    init {
        val api = chatRooms

        if (api == null) {
            _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
        } else {
            // Steam only pushes the messages of groups the session has asked for.
            api.holdGroup(groupId)

            // The group's channels, for switching between them from the top bar.
            viewModelScope.launch {
                api.groups.collect { groups ->
                    val rooms = groups.firstOrNull { it.groupId == groupId }?.rooms
                    if (rooms != null) {
                        _uiState.update { it.copy(rooms = rooms) }
                    }
                }
            }

            viewModelScope.launch {
                api.incoming.collect { message ->
                    if (message.groupId == groupId && message.chatId == chatId) {
                        add(listOf(message))
                        api.ack(groupId, chatId)
                    }
                }
            }

            // Reactions given by anyone, this account on another device included.
            viewModelScope.launch {
                api.reactionChanges.collect { change ->
                    if (change.groupId == groupId && change.chatId == chatId) {
                        applyChange(change)
                    }
                }
            }

            viewModelScope.launch {
                runSuspendCatching { api.history(groupId, chatId) }
                    .onSuccess { page ->
                        add(page.messages)
                        _uiState.update {
                            it.copy(isLoading = false, hasOlder = page.moreAvailable)
                        }
                        api.ack(groupId, chatId)
                    }
                    .onFailure { error ->
                        Timber.w(error, "GetMessageHistory failed")
                        _uiState.update {
                            it.copy(isLoading = false, error = error.requestMessage)
                        }
                    }
            }
        }
    }

    override fun onCleared() {
        chatRooms?.releaseGroup(groupId)
    }

    /** Fetches the page before the oldest loaded message; called as the list nears its top. */
    fun loadOlder() {
        val state = _uiState.value
        val oldest = state.messages.lastOrNull()
        val api = chatRooms

        if (state.isLoading || state.isLoadingOlder || !state.hasOlder) return
        if (oldest == null || api == null) return

        _uiState.update { it.copy(isLoadingOlder = true) }

        viewModelScope.launch {
            runSuspendCatching { api.history(groupId, chatId, before = oldest) }
                .onSuccess { page ->
                    val known = _uiState.value.messages.size
                    add(page.messages)
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = false,
                            // A page that adds nothing new would otherwise be asked for forever.
                            hasOlder = page.moreAvailable && it.messages.size > known,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "GetMessageHistory (older) failed")
                    _uiState.update { it.copy(isLoadingOlder = false) }
                }
        }
    }

    fun sendMessage(message: String) {
        val text = message.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            val sent = chatRooms?.send(groupId, chatId, text)
            if (sent == null) {
                _sendFailures.emit(text)
            } else {
                add(listOf(sent))
            }
        }
    }

    /**
     * Sends a sticker, which in Steam chat is a message of its own: the `/sticker` slash
     * command, which Steam turns into the sticker for everyone in the channel.
     */
    fun sendSticker(sticker: Emoticon) {
        viewModelScope.launch {
            val sent = chatRooms?.send(groupId, chatId, "/sticker ${sticker.name}")
            if (sent == null) {
                _notices.emit("Sticker not sent")
            } else {
                add(listOf(sent))
            }
        }
    }

    /**
     * Uploads the picked image to this channel. Steam posts it as a message itself, which
     * then arrives like any other member's.
     */
    fun sendImage(uri: Uri) {
        if (_uiState.value.isUploading) return
        _uiState.update { it.copy(isUploading = true) }

        viewModelScope.launch {
            val failure = connection.sendChatImage(application, uri) { media, image ->
                media.sendToChatRoom(groupId, chatId, image.bytes, image.fileName)
            }

            _uiState.update { it.copy(isUploading = false) }
            failure?.let { _notices.emit(it) }
        }
    }

    /**
     * Fetches the group's members, for the member list. Asked for each time the list is
     * opened, as people come and go; the ones from last time are shown meanwhile.
     */
    fun loadMembers() {
        val api = chatRooms
        if (api == null || _uiState.value.isLoadingMembers) return

        _uiState.update { it.copy(isLoadingMembers = true) }

        viewModelScope.launch {
            val members = runSuspendCatching { api.members(groupId) }
                .onFailure { Timber.w(it, "Loading the group's members failed") }
                .getOrNull()

            _uiState.update {
                it.copy(
                    members = members?.toImmutableList() ?: it.members,
                    isLoadingMembers = false,
                )
            }
        }
    }

    /**
     * Gives [reaction] to [message], or takes it back; it shows once Steam has agreed. Steam
     * does not push our own reaction back to this session, so it is counted here, the same
     * way as one that is pushed.
     */
    fun react(message: GroupChatMessage, reaction: String, isSticker: Boolean, add: Boolean) {
        viewModelScope.launch {
            val done = runSuspendCatching { chatRooms?.react(message, reaction, isSticker, add) }
                .getOrNull() == true

            if (done) {
                applyChange(
                    GroupReactionChange(
                        groupId = groupId,
                        chatId = chatId,
                        timestamp = message.timestamp,
                        ordinal = message.ordinal,
                        reaction = reaction,
                        isSticker = isSticker,
                        isAdd = add,
                        fromLocal = true,
                    ),
                )
            } else {
                _notices.emit(if (add) "Reaction not added" else "Reaction not removed")
            }
        }
    }

    /**
     * Counts a reaction given or taken back, whether Steam pushed it or it was made here.
     * One of our own that is already counted is left alone, so that it is not counted twice
     * when it comes in both ways.
     */
    private fun applyChange(change: GroupReactionChange) {
        updateReactions("${change.timestamp}:${change.ordinal}") { reactions ->
            val current = reactions.find {
                it.reaction == change.reaction && it.isSticker == change.isSticker
            }
            val mine = current?.mine ?: false

            if (change.fromLocal && mine == change.isAdd) {
                reactions
            } else {
                reactions.with(
                    MessageReaction(
                        reaction = change.reaction,
                        isSticker = change.isSticker,
                        count = (current?.count ?: 0) + if (change.isAdd) 1 else -1,
                        mine = if (change.fromLocal) change.isAdd else mine,
                    ),
                )
            }
        }
    }

    /** Changes the reactions of the loaded message with [key], if it is loaded. */
    private fun updateReactions(
        key: String,
        change: (List<MessageReaction>) -> List<MessageReaction>,
    ) {
        _uiState.update { state ->
            val messages = state.messages.map { message ->
                if (message.key == key) {
                    message.copy(reactions = change(message.reactions).toImmutableList())
                } else {
                    message
                }
            }
            state.copy(messages = messages.toImmutableList())
        }
    }

    /**
     * The reactions with [changed] in the place of the one it stands for, or last when it is
     * new. One that nobody gives any more goes.
     */
    private fun List<MessageReaction>.with(changed: MessageReaction): List<MessageReaction> {
        fun MessageReaction.isSame() =
            reaction == changed.reaction && isSticker == changed.isSticker

        return (if (any { it.isSame() }) this else this + changed)
            .map { if (it.isSame()) changed else it }
            .filter { it.count > 0 }
    }

    /** Merges [incoming] into the list, dropping ones already there and keeping it sorted. */
    private suspend fun add(incoming: List<GroupChatMessage>) {
        // Parse BB Code off the main thread; the rows then find it in the cache.
        withContext(Dispatchers.Default) {
            incoming.forEach { if (BBCode.hasMarkup(it.message)) BBCode.parse(it.message) }
        }

        _uiState.update { state ->
            val merged = (state.messages + incoming)
                .distinctBy { it.key }
                .sortedWith(
                    compareByDescending<GroupChatMessage> { it.timestamp }
                        .thenByDescending { it.ordinal },
                )
            state.copy(messages = merged.toImmutableList())
        }
    }
}
