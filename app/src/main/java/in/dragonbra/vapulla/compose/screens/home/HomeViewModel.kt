package `in`.dragonbra.vapulla.compose.screens.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.chat.PaperPlane
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val gameSchemaManager: GameSchemaManager,
    private val paperPlane: PaperPlane,
    val accountManager: AccountManager,
    private val steamFriendDao: SteamFriendDao
) : AndroidViewModel(application) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(vmApplication)

    private val vmApplication: Application
        get() = getApplication()

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    var homeState by mutableStateOf(HomeState())
        private set

    private lateinit var friendsData: LiveData<List<FriendListItem>>
    private val dataObserver: Observer<List<FriendListItem>> = Observer { list ->
        Timber.d("Observe: ${list.size}")
        val updateTime = System.currentTimeMillis()
        swap(list, updateTime)
    }

    fun onEvent(event: HomeEvent) {
        Timber.d("Event: ${event.javaClass}")
        when (event) {
            is HomeEvent.SwipeRefresh -> {
                homeState = homeState.copy(isRefreshing = event.isRefreshing)
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

            is HomeEvent.StatusChange -> {
                viewModelScope.launch {
                    _uiEvent.emit(HomeUiEvent.ChangeStatus(event.status))
                }
            }

            HomeEvent.AddFriend -> {
                viewModelScope.launch {
                    _uiEvent.emit(HomeUiEvent.AddFriend)
                }
            }

            HomeEvent.Logout -> {
                viewModelScope.launch {
                    _uiEvent.emit(HomeUiEvent.LogOut)
                }
            }

            HomeEvent.Settings -> {
                viewModelScope.launch {
                    _uiEvent.emit(HomeUiEvent.Settings)
                }
            }
        }
    }

    private fun swap(list: List<FriendListItem>, updateTime: Long) {
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

        val recentTimeout =
            prefs.getString("pref_friends_list_recents", "604800000")?.toLong() ?: 0L

        val sortedList = list.sortedWith(compareBy(
            { it.isRequestRecipient().not() },
            { it.isItemRecentChat(recentTimeout, updateTime).not() },
            { it.isInGame().not() },
            { it.isInGameAwayOrSnooze() },
            { it.isOnline().not() },
            { it.isAwayOrSnooze() },
            { it.isOffline().not() },
            { it.friendName.lowercase(Locale.getDefault()) }
        ))

        Timber.d("Friends List Size: ${sortedList.size}")
        homeState = homeState.copy(friendsList = sortedList, updateTime = updateTime)
    }

//    fun search(query: String) {
//        val trimmedQuery = query.trim()
//        friendsData.let { list ->
//            val updateTime = System.currentTimeMillis()
//            if (Strings.isNullOrEmpty(trimmedQuery)) {
//                val sorted = list.value!!.sortedWith(FriendsComparator(vmApplication, updateTime))
//                onEvent(HomeEvent.UpdateFriends(sorted, updateTime))
//
//                return@let
//            }
//
//            val filteredList = list.value!!.filter {
//                val nameFiltered = it.name?.contains(trimmedQuery, true) == true
//                val nickFiltered = it.nickname?.contains(trimmedQuery, true) == true
//                nameFiltered || nickFiltered
//            }.sortedWith(FriendsComparator(vmApplication, updateTime))
//
//            onEvent(HomeEvent.UpdateFriends(filteredList, updateTime))
//        }
//    }

    fun clearStates() {
        steamFriendDao.clearOnlineState()
    }

    fun onPostCreate(owner: LifecycleOwner) {
        Timber.d("onPostCreate")
        friendsData = steamFriendDao.getLive()
        friendsData.observe(owner, dataObserver)

        val updateTime = System.currentTimeMillis()
        swap(friendsData.value.orEmpty(), updateTime)
    }

    fun onDestroy() {
        Timber.d("onDestroy")
        friendsData.removeObserver(dataObserver)
        paperPlane.clearAll()
    }
}
