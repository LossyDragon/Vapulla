package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    db: VapullaDatabase,
    private val service: ServiceConnection,
    private val accountManager: AccountManager,
) : ViewModel() {

   private val friendsList: StateFlow<List<SteamFriend>> = db.steamFriendDao()
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

        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val friends: StateFlow<Map<String, List<SteamFriend>>> = friendsList
        .map { friends ->
            friends.groupBy { friend ->
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
}