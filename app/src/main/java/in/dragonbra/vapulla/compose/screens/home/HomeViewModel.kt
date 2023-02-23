package `in`.dragonbra.vapulla.compose.screens.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import `in`.dragonbra.vapulla.util.recyclerview.FriendsComparator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeState(
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,

    val list: LiveData<List<FriendListItem>> = MutableLiveData(),
    val updateTime: Long = 0L,

    val nickname: String = "",
    val status: String = "",
    val avatarHash: String = ""
)

sealed class HomeEvent {
    data class SwipeRefresh(val isRefreshing: Boolean) : HomeEvent()
    data class Search(val isSearching: Boolean) : HomeEvent()
    data class UpdateAccount(
        val nickname: String,
        val status: String,
        val avatarHash: String
    ) : HomeEvent()

    data class UpdateFriends(
        val list: MutableLiveData<List<FriendListItem>>,
        val updateTime: Long
    ) : HomeEvent()

    object RefreshFriendsList : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    val account: AccountManager,
    private val gameSchemaManager: GameSchemaManager,
    private val paperPlane: PaperPlane,
    private val steamFriendRepository: SteamFriendRepository
) : AndroidViewModel(application) {

    private val vmApplication: Application
        get() = getApplication()

    var homeState by mutableStateOf(HomeState())
    val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        if (!homeState.isSearching) {
            val updateTime = System.currentTimeMillis()
            val sorted = list.sortedWith(FriendsComparator(vmApplication, updateTime))
            onEvent(HomeEvent.UpdateFriends(MutableLiveData(sorted), updateTime))
        }
    }

    fun onEvent(event: HomeEvent) {
        Timber.d("Event: $event")
        when (event) {
            is HomeEvent.SwipeRefresh -> {
                homeState = homeState.copy(isRefreshing = event.isRefreshing)
                viewModelScope.launch {
                    delay(2000L)
                    homeState = homeState.copy(isRefreshing = false)
                }
            }
            is HomeEvent.UpdateFriends ->
                homeState = homeState.copy(list = event.list, updateTime = event.updateTime)
            is HomeEvent.Search ->
                homeState = homeState.copy(isSearching = event.isSearching)
            HomeEvent.RefreshFriendsList -> {
                // TODO somehow handle this request to reach the Activity to call Refresh
            }
            is HomeEvent.UpdateAccount ->
                homeState = homeState.copy(
                    nickname = event.nickname,
                    status = event.status,
                    avatarHash = event.avatarHash
                )
        }
    }

    fun updateFriendsList() {
        viewModelScope.launch {
            steamFriendRepository.clearOnlineState()
            onEvent(HomeEvent.RefreshFriendsList)
        }
    }

    fun search(query: String) {
        val trimmedQuery = query.trim()
        homeState.list.value?.let { list ->
            val updateTime = System.currentTimeMillis()
            if (Strings.isNullOrEmpty(trimmedQuery)) {
                val sorted = list.sortedWith(FriendsComparator(vmApplication, updateTime))
                onEvent(HomeEvent.UpdateFriends(MutableLiveData(sorted), updateTime))

                return@let
            }

            val filteredList = list.filter {
                val nameFiltered = it.name?.contains(trimmedQuery, true) == true
                val nickFiltered = it.nickname?.contains(trimmedQuery, true) == true
                nameFiltered || nickFiltered
            }
            filteredList.sortedWith(FriendsComparator(vmApplication, updateTime))
                .also {
                    onEvent(HomeEvent.UpdateFriends(MutableLiveData(it), updateTime))
                }
        }
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
