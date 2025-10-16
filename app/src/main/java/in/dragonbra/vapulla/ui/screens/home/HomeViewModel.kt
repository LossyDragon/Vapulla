package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceManager
import `in`.dragonbra.vapulla.util.recyclerview.FriendsComparator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    db: VapullaDatabase,
    private val serviceManager: ServiceManager,
    private val accountManager: AccountManager,
) : ViewModel() {

    val friendsList: StateFlow<List<FriendListItem>> = db.steamFriendDao()
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

}