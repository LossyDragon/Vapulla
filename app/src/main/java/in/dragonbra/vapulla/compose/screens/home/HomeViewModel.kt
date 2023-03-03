package `in`.dragonbra.vapulla.compose.screens.home

import android.app.Application
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val gameSchemaManager: GameSchemaManager,
    // private val paperPlane: PaperPlane,
    private val steamFriendDao: SteamFriendDao
) : AndroidViewModel(application) {

    private val vmApplication: Application
        get() = getApplication()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText: MutableStateFlow<TextFieldValue> = _searchText

    private lateinit var friendsData: LiveData<List<FriendListItem>>
    private val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        val updateTime = System.currentTimeMillis()
        if (!state.value.isSearching) {
            swap(list, updateTime)
        }
    }

    init {
        viewModelScope.launch {
            searchText.collectLatest {
                search(it.text)
            }
        }
    }

    fun setSearching(isSearching: Boolean) {
        // Force an update when closing search
        if (!isSearching) {
            search("")
        }

        _state.update { it.copy(isSearching = isSearching) }
    }

    private fun swap(list: List<FriendListItem>, updateTime: Long) {
        if (!state.value.isSearching) {
            list.forEach {
                if (it.gameAppId > 0) {
                    CoroutineScope(Dispatchers.IO).launch {
                        gameSchemaManager.touch(it.gameAppId)
                    }
                }
            }
        }

        // TODO pref
        val prefs = PreferenceManager.getDefaultSharedPreferences(vmApplication)
        val recentTimeout =
            prefs.getString("pref_friends_list_recents", "604800000")?.toLong() ?: 0L

        // Sort friends by name and the following:
        //      Request -> Recent -> In-Game + In-Game-Away -> Online + Away -> Offline.
        val sortedList = list.sortedWith(
            compareBy(
                { it.isRequestRecipient().not() },
                { it.isItemRecentChat(recentTimeout, updateTime).not() },
                { it.isInGame().not() },
                { it.isInGameAwayOrSnooze() },
                { it.isOnline().not() },
                { it.isAwayOrSnooze() },
                { it.isOffline().not() },
                { it.friendName.lowercase() }
            )
        )

        // Map friends to their status
        val groupedList = sortedList.groupBy {
            when {
                it.isRequestRecipient() -> "Request Recipients"
                it.isItemRecentChat(recentTimeout, updateTime) -> "Recent Chats"
                it.isInGame() || it.isInGameAwayOrSnooze() -> "In Game"
                it.isOnline() || it.isAwayOrSnooze() -> "Online"
                else -> "Offline"
            }
        }

        if (state.value.isSearching) {
            _state.update { it.copy(filteredFriendsList = groupedList, updateTime = updateTime) }
        } else {
            _state.update {
                it.copy(
                    friendsList = groupedList,
                    filteredFriendsList = groupedList,
                    updateTime = updateTime
                )
            }
        }
    }

    private fun search(query: String) {
        val isSearching = state.value.isSearching && searchText.value.text.isNotEmpty()
        val list = if (isSearching) {
            val friendsValues = state.value.friendsList.values.flatten()
            friendsValues.filter {
                val name = it.name
                val nickname = it.nickname
                val trimmedQuery = query.trim().lowercase()
                name?.contains(trimmedQuery, true) ?: false || nickname?.contains(
                    trimmedQuery,
                    true
                ) ?: false
            }
        } else {
            state.value.friendsList.values.flatten()
        }

        swap(list, System.currentTimeMillis())
    }

    fun clearStates() {
        steamFriendDao.clearOnlineState()
    }

    fun onPostCreate(owner: LifecycleOwner) {
        friendsData = steamFriendDao.getLive()
        friendsData.observe(owner, dataObserver)

        val updateTime = System.currentTimeMillis()
        swap(friendsData.value.orEmpty(), updateTime)
    }

    fun onDestroy() {
        friendsData.removeObserver(dataObserver)
    }

    fun onSwipeRefresh(isRefreshing: Boolean) {
        _state.update { it.copy(isRefreshing = isRefreshing) }
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.Refresh)
            delay(1000L)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun onUpdateAccount(nickname: String, status: EPersonaState, avatarHash: String) {
        _state.update { it.copy(nickname = nickname, status = status, avatarHash = avatarHash) }
    }

    fun onStatusUpdate(status: EPersonaState) {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.ChangeStatus(status))
        }
    }

    fun onAddFriend() {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.AddFriend)
        }
    }

    fun onLogout() {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.LogOut)
        }
    }

    fun onSettings() {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.Settings)
        }
    }
}
