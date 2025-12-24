package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

class HomeViewModel(db: SteamFriendDao) : ViewModel() {

    private val _stickyHeaders = MutableStateFlow(persistentSetOf<Int>())
    val stickyHeaders: StateFlow<ImmutableSet<Int>> = _stickyHeaders.asStateFlow()

    val friends: StateFlow<ImmutableMap<Int, ImmutableList<SteamFriend>>> = db
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
                    // Group with Sticky Headers
                    when {
                        friend.isRequestRecipient -> R.string.headerFriendRecent
                        friend.isPlayingGame || friend.isInGameAwayOrSnooze -> R.string.headerFriendInGame
                        friend.isOnline || friend.isAwayOrSnooze -> R.string.headerFriendOnline
                        else -> R.string.headerFriendOffline
                    }
                }
                .mapValues { it.value.toImmutableList() }
                .toImmutableMap()
        }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentMapOf(),
        )

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }

    fun onStickyHeaderAction(value: Int) {
        _stickyHeaders.update { current ->
            if (value in current) {
                current.remove(value)
            } else {
                current.add(value)
            }
        }
    }
}
