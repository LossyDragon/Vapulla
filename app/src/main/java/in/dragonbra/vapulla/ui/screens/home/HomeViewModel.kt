package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.dao.ChatMessageDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.SettingsManager
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Friends list state: friends grouped into the sticky-header sections (requests, recent chats,
 * in game, online, offline), sorted within each, plus which sections are collapsed.
 */
class HomeViewModel(
    db: SteamFriendDao,
    chatDao: ChatMessageDao,
    settingsManager: SettingsManager,
) : ViewModel() {

    private val _stickyHeaders = MutableStateFlow(persistentSetOf<Int>())
    val stickyHeaders: StateFlow<ImmutableSet<Int>> = _stickyHeaders.asStateFlow()

    /** Emits now and then once a minute, so the Recent Chats cut-off is re-evaluated in time. */
    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    val friends: StateFlow<ImmutableMap<Int, ImmutableList<SteamFriend>>> = combine(
        db.getFriendsFlow(),
        chatDao.chatSummaries(),
        settingsManager.settings.map { it.recentChatsDays }.distinctUntilChanged(),
        minuteTicker,
    ) { friends, chats, recentDays, _ ->
        // Friends with a stored message newer than the cut-off, mapped to that message's
        // time. The cut-off is taken each time the list is rebuilt: on any presence change
        // or new message, and at least once a minute. An unread message keeps a chat
        // listed whatever its age, so it cannot go unnoticed.
        val cutOff = System.currentTimeMillis() - recentDays.days.inWholeMilliseconds
        val recentChats = chats
            .filter { it.timestamp >= cutOff || it.unread > 0 }
            .associate { it.friendId to it.timestamp }

        val unreadCounts = chats
            .filter { it.unread > 0 }
            .associate { it.friendId to it.unread }

        friends.filter { it.isFriend && !it.isBlocked }
            // The row's counter; it clears when the chat is opened and its messages are read.
            .map { friend ->
                val unread = unreadCounts[friend.id] ?: 0
                if (friend.newMessageCount ==
                    unread
                ) {
                    friend
                } else {
                    friend.copy(newMessageCount = unread)
                }
            }
            .sortedWith(
                compareBy(
                    { it.isRequestRecipient.not() },
                    { it.id !in recentChats },
                    // Newest conversation first; 0 for everyone outside Recent Chats.
                    { -(recentChats[it.id] ?: 0L) },
                    { it.isPlayingGame.not() },
                    { it.isInGameAwayOrSnooze },
                    { it.isOnline.not() },
                    { it.isAwayOrSnooze },
                    { it.isOffline.not() },
                    { it.nameOrNickname.lowercase() },
                ),
            )
            .groupBy { friend ->
                // Group with Sticky Headers. A friend sits in exactly one section, so a
                // recent chat is listed there instead of under its presence.
                when {
                    friend.isRequestRecipient -> R.string.headerFriendRequests

                    friend.id in recentChats -> R.string.headerFriendRecent

                    friend.isPlayingGame || friend.isInGameAwayOrSnooze ->
                        R.string.headerFriendInGame

                    friend.isOnline || friend.isAwayOrSnooze -> R.string.headerFriendOnline

                    else -> R.string.headerFriendOffline
                }
            }
            .mapValues { it.value.toImmutableList() }
            .toImmutableMap()
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentMapOf(),
        )

    override fun onCleared() {
        Timber.d("onCleared")
    }

    fun onStickyHeaderAction(value: Int) {
        _stickyHeaders.update { current ->
            if (value in current) {
                current.removing(value)
            } else {
                current.adding(value)
            }
        }
    }
}
