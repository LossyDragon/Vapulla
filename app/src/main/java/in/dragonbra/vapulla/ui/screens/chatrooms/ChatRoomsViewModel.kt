package `in`.dragonbra.vapulla.ui.screens.chatrooms

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.ChatGroup
import `in`.dragonbra.vapulla.service.api.ChatRoomsApi
import `in`.dragonbra.vapulla.service.api.GroupChatMessage
import `in`.dragonbra.vapulla.util.toChatPreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class ChatRoomsUiState(
    val groups: ImmutableList<ChatGroup> = persistentListOf(),
    val expanded: PersistentSet<Long> = persistentSetOf(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Group chats the account belongs to, from the `ChatRoom` unified service. A group that is
 * expanded shows its channels with their last message, which is kept live: while expanded,
 * Steam is asked to push the group's messages, and each one replaces its channel's preview.
 */
class ChatRoomsViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomsUiState())
    val uiState: StateFlow<ChatRoomsUiState> = _uiState.asStateFlow()

    private val chatRooms: ChatRoomsApi?
        get() = connection.steamService?.apis?.chatRooms

    init {
        refresh()

        viewModelScope.launch {
            chatRooms?.incoming?.collect(::showAsLastMessage)
        }
    }

    override fun onCleared() {
        // Stop the pushes this screen asked for; an open channel keeps its own hold.
        _uiState.value.expanded.forEach { chatRooms?.releaseGroup(it) }
    }

    fun refresh() = load(quiet = false)

    /**
     * Fetches the groups. A [quiet] load is the one made when a group is expanded: it shows
     * no loading state and leaves the list as it is if it fails.
     */
    private fun load(quiet: Boolean) {
        viewModelScope.launch {
            Timber.d("Refreshing group chats (quiet=$quiet)")
            if (!quiet) _uiState.update { it.copy(isLoading = true, error = null) }
            val api = chatRooms
            if (api == null) {
                if (quiet) return@launch
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            runCatching { api.myGroups() }
                .onSuccess { groups ->
                    _uiState.update {
                        it.copy(
                            groups = groups.sortedBy { g -> g.name.lowercase() }.toImmutableList(),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "GetMyChatRoomGroups failed")
                    if (quiet) return@onFailure
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "Request failed")
                    }
                }
        }
    }

    fun toggleExpanded(groupId: Long) {
        val expand = groupId !in _uiState.value.expanded

        // Live previews only make sense for channels that are on screen. Messages sent
        // while the group was collapsed were never pushed, so its previews are fetched again.
        if (expand) {
            chatRooms?.holdGroup(groupId)
            load(quiet = true)
        } else {
            chatRooms?.releaseGroup(groupId)
        }

        _uiState.update {
            it.copy(
                expanded = if (expand) {
                    it.expanded.adding(groupId)
                } else {
                    it.expanded.removing(groupId)
                },
            )
        }
    }

    /** Puts a pushed message into its channel's row as the new last message. */
    private fun showAsLastMessage(message: GroupChatMessage) {
        _uiState.update { state ->
            state.copy(
                groups = state.groups.map { group ->
                    if (group.groupId != message.groupId) return@map group

                    group.copy(
                        rooms = group.rooms.map { room ->
                            if (room.chatId != message.chatId) return@map room

                            room.copy(
                                lastMessage = message.message.toChatPreview(),
                                timeLastMessage = message.timestamp / 1000,
                            )
                        }.toImmutableList(),
                    )
                }.toImmutableList(),
            )
        }
    }
}
