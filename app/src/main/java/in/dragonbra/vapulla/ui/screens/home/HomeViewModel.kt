package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.screens.login.LoginUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    db: VapullaDatabase,
    private val service: ServiceConnection,
    private val accountManager: AccountManager,
) : ViewModel() {

    private val _stickyHeaders = MutableStateFlow(emptySet<String>())
    val stickyHeaders: StateFlow<Set<String>> = _stickyHeaders.asStateFlow()

    val friends: StateFlow<Map<String, List<SteamFriend>>> = db.steamFriendDao()
        .getFriendsFlow()
        .map { friends ->
            friends.filter { it.isFriend && !it.isBlocked }
                .sortedWith(
                    compareBy(
                        { it.isRequestRecipient.not() },
                        { it.isPlayingGame.not() },
                        { it.isInGameAwayOrSnooze },
                        { it.isOnline.not() },
                        { it.isAwayOrSnooze },
                        { it.isOffline.not() },
                        { it.nameOrNickname.lowercase() },
                    ),
                )
                .groupBy { friend ->
                    when {
                        friend.isRequestRecipient -> "Friend Requests"
                        friend.isPlayingGame || friend.isInGameAwayOrSnooze -> "In Game"
                        friend.isOnline || friend.isAwayOrSnooze -> "Online"
                        else -> "Offline"
                    }
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )


    fun onStickyHeaderAction(value: String) {
        val list = stickyHeaders.value.toMutableSet()
        if (value in list) {
            list.remove(value)
        } else {
            list.add(value)
        }

        _stickyHeaders.update { list }
    }
}