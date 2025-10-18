package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    db: SteamFriendDao,
    private val service: ServiceConnection,
    private val accountManager: AccountManager,
) : ViewModel() {

    private val _stickyHeaders = MutableStateFlow(emptySet<Int>())
    val stickyHeaders: StateFlow<Set<Int>> = _stickyHeaders.asStateFlow()

    val friends: StateFlow<Map<Int, List<SteamFriend>>> = db.getFriendsFlow().map { friends ->
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
                // Group with Sticky Headers
                when {
                    friend.isRequestRecipient -> R.string.headerFriendRecent
                    friend.isPlayingGame || friend.isInGameAwayOrSnooze -> R.string.headerFriendInGame
                    friend.isOnline || friend.isAwayOrSnooze -> R.string.headerFriendOnline
                    else -> R.string.headerFriendOffline
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun onStickyHeaderAction(value: Int) {
        val list = stickyHeaders.value.toMutableSet()
        if (value in list) {
            list.remove(value)
        } else {
            list.add(value)
        }

        _stickyHeaders.update { list }
    }
}