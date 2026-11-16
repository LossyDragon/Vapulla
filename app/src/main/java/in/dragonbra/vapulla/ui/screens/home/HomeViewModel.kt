package `in`.dragonbra.vapulla.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.dao.ChatMessageDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.api.SteamFavorites
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Friends list state: friends grouped into the sticky-header sections (requests, recent chats,
 * in game, online, offline), sorted within each, plus which sections are collapsed.
 */
class HomeViewModel(
    friendDao: SteamFriendDao,
    chatDao: ChatMessageDao,
    settingsManager: SettingsManager,
    private val connection: SteamServiceConnection,
) : ViewModel() {

    /** The collapsed sections. Blocked friends start out of the way. */
    private val _stickyHeaders = MutableStateFlow(persistentSetOf(R.string.headerFriendBlocked))
    val stickyHeaders: StateFlow<ImmutableSet<Int>> = _stickyHeaders.asStateFlow()

    /**
     * The favourite friends, in Steam's order, for the row above the list. A favourite who
     * is no longer a friend is left out.
     */
    val favorites: StateFlow<ImmutableList<SteamFriend>> = combine(
        friendDao.getFriendsFlow(),
        SteamFavorites.friendIds,
    ) { friends, favoriteIds ->
        val byId = friends.filter { it.isFriend }.associateBy { it.id }
        favoriteIds.mapNotNull { byId[it] }.toImmutableList()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    /** Emits at once and then every minute, so the Recent Chats cut-off is re-evaluated in time. */
    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(1.minutes)
        }
    }

    val friends: StateFlow<ImmutableMap<Int, ImmutableList<SteamFriend>>> = combine(
        friendDao.getFriendsFlow(),
        chatDao.chatSummaries(),
        settingsManager.settings.distinctUntilChangedBy {
            it.recentChatsDays to it.showBlockedFriends
        },
        minuteTicker,
    ) { friends, chats, settings, _ ->
        // Friends with a stored message newer than the cut-off, mapped to that message's
        // time. The cut-off is taken each time the list is rebuilt: on any presence change
        // or new message, and at least once a minute. An unread message keeps a chat
        // listed whatever its age, so it cannot go unnoticed.
        val cutOff = System.currentTimeMillis() - settings.recentChatsDays.days.inWholeMilliseconds
        val recentChats = chats
            .filter { it.timestamp >= cutOff || it.unread > 0 }
            .associate { it.friendId to it.timestamp }

        val unreadCounts = chats
            .filter { it.unread > 0 }
            .associate { it.friendId to it.unread }

        val lastMessages = chats.associate { it.friendId to it.timestamp }

        friends.filter { it.isFriend || (settings.showBlockedFriends && it.isBlockedFriend) }
            // The row's counter and the time of its last message. The counter clears when
            // the chat is opened and its messages are read.
            .map { friend ->
                friend.copy(
                    newMessageCount = unreadCounts[friend.id] ?: 0,
                    lastMessageTime = lastMessages[friend.id] ?: friend.lastMessageTime,
                )
            }
            .sortedWith(
                compareBy(
                    { it.isRequestRecipient.not() },
                    // Blocked friends go last, whatever else is true of them.
                    { it.isBlockedFriend },
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

                    friend.isBlockedFriend -> R.string.headerFriendBlocked

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

    /**
     * The friends typing to us right now. The chat API is replaced on every sign-in, so it
     * is looked up again whenever the session comes back.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val typing: StateFlow<ImmutableSet<Long>> = SteamService.isLoggedIn
        .flatMapLatest { loggedIn ->
            val chat = connection.steamService?.apis?.chat
            if (loggedIn && chat != null) chat.typing else flowOf(emptySet())
        }
        .map { it.toImmutableSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = persistentSetOf(),
        )

    /**
     * Asks Steam which chats are still unread. A chat read on another device loses its
     * counter this way; the screen calls this while it is in front.
     */
    fun refreshUnread() {
        viewModelScope.launch {
            connection.request("GetActiveMessageSessions") { it.chat.syncUnread() }
        }
    }

    /** Collapses the section with the header [value], or opens it again. */
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
