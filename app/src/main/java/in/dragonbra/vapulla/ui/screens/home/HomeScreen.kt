package `in`.dragonbra.vapulla.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchAppBar
import `in`.dragonbra.vapulla.ui.mock.mockFriendsList
import `in`.dragonbra.vapulla.ui.mock.mockStickyHeaders
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendList
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How often the friends list asks Steam which chats are unread, while it is on screen. */
private val UNREAD_REFRESH = 30.seconds

/**
 * The friends list, in sections, with a search over it. Tapping a friend opens the chat; a
 * long press or a swipe opens the profile.
 *
 * @param onOpenChat opens the chat with the friend of that Steam ID.
 * @param onOpenProfile opens that friend's profile.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavDrawerAction: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
) {
    // Unread counters follow what was read on other devices while the list is in front.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshUnread()
                delay(UNREAD_REFRESH)
            }
        }
    }

    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val friendsList by viewModel.friends.collectAsStateWithLifecycle()
    val stickyHeaders by viewModel.stickyHeaders.collectAsStateWithLifecycle()
    val typing by viewModel.typing.collectAsStateWithLifecycle()

    HomeScreenContent(
        favorites = favorites,
        friendsList = friendsList,
        stickyHeaders = stickyHeaders,
        typing = typing,
        onNavDrawerAction = onNavDrawerAction,
        onOpenChat = onOpenChat,
        onOpenProfile = onOpenProfile,
        onStickyHeaderAction = viewModel::onStickyHeaderAction,
    )
}

@Composable
private fun HomeScreenContent(
    favorites: ImmutableList<SteamFriend>,
    friendsList: ImmutableMap<Int, ImmutableList<SteamFriend>>,
    stickyHeaders: ImmutableSet<Int>,
    typing: ImmutableSet<Long>,
    onNavDrawerAction: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onStickyHeaderAction: (Int) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val scope = rememberCoroutineScope()

    // A blocked friend cannot be chatted with, so a tap goes to the profile instead.
    val onFriendClick: (SteamFriend) -> Unit = { friend ->
        if (friend.isBlocked) onOpenProfile(friend.id) else onOpenChat(friend.id)
    }

    val searchQuery = textFieldState.text.toString()
    val filteredFriends = remember(searchQuery, friendsList) {
        if (searchQuery.isNotEmpty()) {
            friendsList.values.flatten().filter { friend ->
                friend.name.contains(searchQuery, ignoreCase = true) ||
                    friend.nickname.contains(searchQuery, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                scrollBehavior = scrollBehavior,
                onNavDrawerAction = onNavDrawerAction,
                expandedSearchBar = {
                    if (searchQuery.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            itemsIndexed(filteredFriends, key = { _, it ->
                                it.id
                            }) { index, friend ->
                                FriendListItem(
                                    friend = friend,
                                    shape = groupedListShape(index, filteredFriends.size),
                                    isTyping = friend.id in typing,
                                    onClick = {
                                        onFriendClick(friend)
                                        textFieldState.clearText()
                                        scope.launch { searchBarState.animateToCollapsed() }
                                    },
                                    onLongClick = {
                                        onOpenProfile(friend.id)
                                        textFieldState.clearText()
                                        scope.launch { searchBarState.animateToCollapsed() }
                                    },
                                )
                            }

                            if (filteredFriends.isEmpty()) {
                                item {
                                    EmptyMessage(
                                        message = "No friends found matching \"$searchQuery\"",
                                    )
                                }
                            }
                        }
                    } else {
                        EmptyMessage(message = "Start typing to search friends")
                    }
                },
            )
        },
        content = { padding ->
            FriendList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding.exceptBottom()),
                contentPadding = listContentPadding(padding, vertical = 4.dp),
                favorites = favorites,
                friendsList = friendsList,
                stickyHeaders = stickyHeaders,
                typing = typing,
                onStickyHeaderAction = onStickyHeaderAction,
                onFriendClick = onFriendClick,
                onOpenProfile = onOpenProfile,
            )
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        HomeScreenContent(
            favorites = persistentListOf(),
            friendsList = mockFriendsList,
            stickyHeaders = mockStickyHeaders,
            typing = persistentSetOf(),
            onNavDrawerAction = {},
            onOpenChat = {},
            onOpenProfile = {},
            onStickyHeaderAction = {},
        )
    }
}
