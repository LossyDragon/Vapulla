package `in`.dragonbra.vapulla.ui.screens.chatrooms

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.api.ChatGroup
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

/** Group chats the account belongs to, from the `ChatRoom` unified service. */
class ChatRoomsViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomsUiState())
    val uiState: StateFlow<ChatRoomsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            Timber.d("Refreshing group chats")
            _uiState.update { it.copy(isLoading = true, error = null) }
            val apis = connection.steamService?.apis
            if (apis == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not connected to Steam") }
                return@launch
            }

            runCatching { apis.chatRooms.myGroups() }
                .onSuccess { groups ->
                    _uiState.update {
                        it.copy(
                            groups = groups.sortedBy { g ->
                                g.name.lowercase()
                            }.toImmutableList(),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { error ->
                    Timber.w(error, "GetMyChatRoomGroups failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error =
                                error.message ?: "Request failed",
                        )
                    }
                }
        }
    }

    fun toggleExpanded(groupId: Long) {
        _uiState.update {
            val expanded = if (groupId in
                it.expanded
            ) {
                it.expanded.remove(groupId)
            } else {
                it.expanded.add(groupId)
            }
            it.copy(expanded = expanded)
        }
    }
}
