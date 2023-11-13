package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.model.FriendListGroup
import `in`.dragonbra.vapulla.model.FriendListItem
import javax.inject.Inject
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gameSchemaManager: GameSchemaManager,
    private val accountManager: AccountManager,
    private val steamFriendDao: SteamFriendDao
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val _searchText = MutableStateFlow(TextFieldValue(""))
    val searchText: MutableStateFlow<TextFieldValue> = _searchText

    private lateinit var friendsData: LiveData<List<FriendListItem>>
    private val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        val updateTime = System.currentTimeMillis().div(1000)
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

    fun isSearching() {
        _state.update { it.copy(isSearching = true) }
    }

    fun isNotSearching() {
        search("")
        _state.update { it.copy(isSearching = false) }
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

        // Sort friends by name and the following:
        //      Request -> Recent -> In-Game + In-Game-Away -> Online + Away -> Offline.
        val recentTimeout = accountManager.prefFriendsListRecents
        val sortedList = list.sortedWith(
            compareBy(
                { it.isRequestRecipient.not() },
                { it.isItemRecentChat(recentTimeout, updateTime).not() },
                { it.isInGame.not() },
                { it.isInGameAwayOrSnooze },
                { it.isOnline.not() },
                { it.isAwayOrSnooze() },
                { it.isOffline.not() },
                { it.friendName.lowercase() }
            )
        )

        // Map friends to their status
        val groups =
            sortedList.fold(mutableMapOf<String, MutableList<FriendListItem>>()) { acc, item ->
                when {
                    item.isRequestRecipient ->
                        acc.getOrPut("Friend Request") { mutableListOf() }.add(item)

                    item.isItemRecentChat(recentTimeout, updateTime) ->
                        acc.getOrPut("Recent Chat") { mutableListOf() }.add(item)

                    item.isInGame || item.isInGameAwayOrSnooze ->
                        acc.getOrPut("In-Game") { mutableListOf() }.add(item)

                    item.isOnline || item.isAwayOrSnooze() ->
                        acc.getOrPut("Online") { mutableListOf() }.add(item)

                    else -> acc.getOrPut("Offline") { mutableListOf() }.add(item)
                }
                acc
            }

        val groupedList = groups.map { (title, items) ->
            FriendListGroup(
                groupName = title,
                groupCount = items.size,
                groupList = items,
                isCollapsed = accountManager.getCollapsedState(title)
            )
        }.toList()

        if (state.value.isSearching) {
            _state.update {
                it.copy(
                    filteredFriendsList = groupedList,
                    updateTime = updateTime
                )
            }
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
        if (!state.value.isSearching && searchText.value.text.isEmpty()) {
            val list = state.value.friendsList.flatMap { it.groupList }
            swap(list, System.currentTimeMillis())
            return
        }

        val list = state.value.friendsList.flatMap { it.groupList }.filter { item ->
            item.name.orEmpty().contains(query.trim(), true) ||
                item.nickname.orEmpty().contains(query.trim(), true)
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

        // Queue a first time refresh to get a current friend states.
        viewModelScope.launch {
            delay(1000L)
            _uiEvent.emit(HomeUiEvent.Refresh)
        }
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

    fun onHeaderAction(header: String, value: Boolean) {
        accountManager.setCollapsedState(header, value)
    }

    fun onFriendAccept(friendListItem: FriendListItem) {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.AcceptRequest(friendListItem))
        }
    }

    fun onFriendIgnore(friendListItem: FriendListItem) {
        viewModelScope.launch {
            _uiEvent.emit(HomeUiEvent.IgnoreRequest(friendListItem))
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
