package `in`.dragonbra.vapulla.compose.screens.home

import android.app.Application
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.data.repository.SteamFriendRepository
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.util.OfflineStatusUpdater
import `in`.dragonbra.vapulla.util.recyclerview.FriendsComparator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    val account: AccountManager,
    val gameSchemaManager: GameSchemaManager,
    private val paperPlane: PaperPlane,
    private val steamFriendRepository: SteamFriendRepository
) : AndroidViewModel(application) {

    companion object {
        private const val UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS
    }

    private val vmApplication: Application
        get() = getApplication()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    var homeState by mutableStateOf(HomeState())
        private set
    val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        if (!homeState.isSearching) {
            val updateTime = System.currentTimeMillis()
            val sorted = list.sortedWith(FriendsComparator(vmApplication, updateTime))
            onEvent(HomeEvent.UpdateFriends(sorted, updateTime))
        }
    }

    private var offlineStatusUpdater: OfflineStatusUpdater = OfflineStatusUpdater(vmApplication)

    fun onEvent(event: HomeEvent) {
        Timber.d("Event: $event")
        when (event) {
            is HomeEvent.SwipeRefresh -> {
                homeState = homeState.copy(isRefreshing = event.isRefreshing)
                updateFriendsList()
                updateOfflineFriends()
                viewModelScope.launch {
                    delay(1000L)
                    homeState = homeState.copy(isRefreshing = false)
                }
            }

            is HomeEvent.UpdateFriends -> {
                homeState = homeState.copy(
                    friendsList = MutableStateFlow(event.list),
                    updateTime = event.updateTime
                )
            }

            is HomeEvent.Search -> {
                homeState = homeState.copy(isSearching = event.isSearching)
            }

            HomeEvent.RefreshFriendsList -> {
                // TODO -.- why not just combine SwipeRefresh, but not set isRefreshing
                viewModelScope.launch {
                    // Let the Activity know we need to tell Steam to give us a new friends list
                    _uiEvent.emit(HomeUiEvent.Refresh)

                    // TODO nothing on screen.
                    val updateTime = System.currentTimeMillis()
                    val friends = getLive().sortedWith(FriendsComparator(vmApplication, updateTime))
                    homeState = homeState.copy(updateTime = updateTime)
                    delay(1000L)
                    homeState.friendsList.emit(friends)
                }
            }

            is HomeEvent.UpdateAccount -> {
                with(event) {
                    homeState = homeState.copy(
                        nickname = nickname,
                        status = status,
                        avatarHash = avatarHash
                    )
                }
            }
        }
    }

    fun updateFriendsList() {
        viewModelScope.launch {
            clearOnlineState()
            onEvent(HomeEvent.RefreshFriendsList)
        }
    }

    fun updateOfflineFriends() {
        viewModelScope.launch {
            while (isActive) {
                offlineStatusUpdater.updateAll()
                delay(UPDATE_INTERVAL)
            }
        }
    }

    fun search(query: String) {
        val trimmedQuery = query.trim()
        homeState.friendsList.value.let { list ->
            val updateTime = System.currentTimeMillis()
            if (Strings.isNullOrEmpty(trimmedQuery)) {
                val sorted = list.sortedWith(FriendsComparator(vmApplication, updateTime))
                onEvent(HomeEvent.UpdateFriends(sorted, updateTime))

                return@let
            }

            val filteredList = list.filter {
                val nameFiltered = it.name?.contains(trimmedQuery, true) == true
                val nickFiltered = it.nickname?.contains(trimmedQuery, true) == true
                nameFiltered || nickFiltered
            }.sortedWith(FriendsComparator(vmApplication, updateTime))

            onEvent(HomeEvent.UpdateFriends(filteredList, updateTime))
        }
    }

    fun onDestroy() {
        // homeState.friendsList.removeObserver(dataObserver)
        paperPlane.clearAll()
        offlineStatusUpdater.clear()
    }

    // region [REGION] Repository Functions
    fun insert(vararg steamFriends: SteamFriend) {
        steamFriendRepository.insert(steamFriends = steamFriends)
    }

    fun find(id: Long): SteamFriend? {
        steamFriendRepository.find(id = id)
        return steamFriendRepository.foundFriend.value
    }

    fun findLive(id: Long): FriendListItem? {
        steamFriendRepository.findLive(id = id)
        return steamFriendRepository.findLiveFriends.value
    }

    fun update(vararg steamFriends: SteamFriend) {
        steamFriendRepository.update(steamFriends = steamFriends)
    }

    fun getLive(): List<FriendListItem> {
        steamFriendRepository.getLive()
        return steamFriendRepository.getLiveFriends.value ?: listOf()
    }

    fun clearNicknames() {
        steamFriendRepository.clearNicknames()
    }

    fun remove(vararg friends: SteamFriend) {
        steamFriendRepository.remove(friends = friends)
    }

    fun delete() {
        steamFriendRepository.delete()
    }

    fun clearOnlineState() {
        steamFriendRepository.clearOnlineState()
    }
// endregion
}
