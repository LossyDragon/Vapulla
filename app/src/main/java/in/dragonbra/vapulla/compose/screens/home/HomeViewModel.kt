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
import androidx.preference.PreferenceManager
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    val account: AccountManager,
    private val gameSchemaManager: GameSchemaManager,
    private val paperPlane: PaperPlane,
    private val steamFriendRepository: SteamFriendRepository
) : AndroidViewModel(application) {

    private var offlineStatusUpdater = OfflineStatusUpdater(vmApplication)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(vmApplication)

    private val vmApplication: Application
        get() = getApplication()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    var homeState by mutableStateOf(HomeState())
        private set

    val friendsData: LiveData<List<FriendListItem>> = steamFriendRepository.steamFriendDao.getLive()
    val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        Timber.d("Observed!!!")
        if (!homeState.isSearching) {
            val updateTime = System.currentTimeMillis()
            swap(list, updateTime)
        }
    }

    fun onEvent(event: HomeEvent) {
        // Timber.d("Event: $event")
        when (event) {
            is HomeEvent.SwipeRefresh -> {
                homeState = homeState.copy(isRefreshing = event.isRefreshing)
                clearOnlineState()
                viewModelScope.launch {
                    _uiEvent.emit(HomeUiEvent.Refresh)
                    delay(1000L)
                    homeState = homeState.copy(isRefreshing = false)
                }
            }

            is HomeEvent.UpdateFriends -> {
                swap(event.list, event.updateTime)
            }

            is HomeEvent.Search -> {
                homeState = homeState.copy(isSearching = event.isSearching)
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

    private fun swap(list: List<FriendListItem>, updateTime: Long) {
        offlineStatusUpdater.updateAll()

        val timeout = prefs.getString("pref_friends_list_recents", "604800000")!!.toLong()
        val sorting = prefs.getString("pref_friends_list_sort", "1")!!

        if (list.isEmpty()) {
            return
        }

        list.forEach {
            if (it.gameAppId > 0) {
                CoroutineScope(Dispatchers.IO).launch {
                    gameSchemaManager.touch(it.gameAppId)
                }
            }
        }

        val newList = if (sorting == "1") {
            // Sort by Status
            list.sortedWith(
                compareBy(
                    { !it.isRequestRecipient() },
                    { !it.isItemRecentChat(timeout, updateTime) },
                    { !it.isInGame() },
                    { !it.isOnline() },
                    { it.isAwayOrSnooze() },
                    {
                        if (!it.nickname.isNullOrEmpty()) {
                            it.nickname?.lowercase()
                        } else {
                            it.name?.lowercase()
                        }
                    }
                )
            )
        } else {
            // Sort by Name
            listOf()
        }

        homeState = homeState.copy(list = MutableLiveData(newList), updateTime = updateTime)
    }

    fun search(query: String) {
        val trimmedQuery = query.trim()
        homeState.list.let { list ->
            val updateTime = System.currentTimeMillis()
            if (Strings.isNullOrEmpty(trimmedQuery)) {
                val sorted = list.value!!.sortedWith(FriendsComparator(vmApplication, updateTime))
                onEvent(HomeEvent.UpdateFriends(sorted, updateTime))

                return@let
            }

            val filteredList = list.value!!.filter {
                val nameFiltered = it.name?.contains(trimmedQuery, true) == true
                val nickFiltered = it.nickname?.contains(trimmedQuery, true) == true
                nameFiltered || nickFiltered
            }.sortedWith(FriendsComparator(vmApplication, updateTime))

            onEvent(HomeEvent.UpdateFriends(filteredList, updateTime))
        }
    }

    fun onDestroy() {
        Timber.d("on destroy")
        friendsData.removeObserver(dataObserver)
        paperPlane.clearAll()
        offlineStatusUpdater.clear()
    }

    // region [REGION] Repository Functions
    private fun insert(vararg steamFriends: SteamFriend) {
        Timber.d("insert")
        steamFriendRepository.insert(steamFriends = steamFriends)
    }

    private fun find(id: Long): SteamFriend? {
        Timber.d("find")
        steamFriendRepository.find(id = id)
        return steamFriendRepository.foundFriend.value
    }

    private fun findLive(id: Long): FriendListItem? {
        Timber.d("findLive")
        steamFriendRepository.findLive(id = id)
        return steamFriendRepository.findLiveFriends.value
    }

    private fun update(vararg steamFriends: SteamFriend) {
        Timber.d("update")
        steamFriendRepository.update(steamFriends = steamFriends)
    }

    private fun getLive(): List<FriendListItem> {
        steamFriendRepository.getLive()
        val list = steamFriendRepository.getLiveFriends.value ?: listOf()
        Timber.d("getLive: ${list.size}")
        return list
    }

    private fun clearNicknames() {
        steamFriendRepository.clearNicknames()
    }

    private fun remove(vararg friends: SteamFriend) {
        Timber.d("remove")
        steamFriendRepository.remove(friends = friends)
    }

    private fun delete() {
        Timber.d("delete")
        steamFriendRepository.delete()
    }

    private fun clearOnlineState() {
        Timber.d("clearOnlineState")
        steamFriendRepository.clearOnlineState()
    }

    // endregion
}
