package `in`.dragonbra.vapulla.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.SearchAppBar
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendList
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavDrawerAction: () -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    val friends by viewModel.friends.collectAsState()
    val stickyHeaders by viewModel.stickyHeaders.collectAsState()
    HomeScreenContent(
        friends = friends,
        stickyHeaders = stickyHeaders,
        onStickyHeaderAction = viewModel::onStickyHeaderAction,
        onNavDrawerAction = onNavDrawerAction,
        onFriendClick = onFriendClick,
        onFriendLongClick = onFriendLongClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    friends: Map<String, List<SteamFriend>>,
    stickyHeaders: Set<String>,
    onStickyHeaderAction: (String) -> Unit,
    onNavDrawerAction: () -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier,
        topBar = {
            SearchAppBar(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                scrollBehavior = scrollBehavior,
                onNavDrawerAction = onNavDrawerAction,
                onAccountAction = {
                    TODO()
                },
                expandedSearchBar = {
                    val searchQuery = textFieldState.text.toString()

                    if (searchQuery.isNotEmpty()) {
                        val filteredFriends = friends.values.flatten().filter { friend ->
                            friend.name.contains(searchQuery, ignoreCase = true) ||
                            friend.nickname.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredFriends, key = { it.id }) { friend ->
                                FriendListItem(
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                onFriendClick(friend.id)
                                                textFieldState.clearText()
                                                scope.launch { searchBarState.animateToCollapsed() }
                                            },
                                            onLongClick = {
                                                onFriendLongClick(friend.id)
                                                textFieldState.clearText()
                                                scope.launch { searchBarState.animateToCollapsed() }
                                            }
                                        ),
                                    friend = friend,
                                )
                            }

                            if (filteredFriends.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No friends found matching \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = "Start typing to search friends",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        content = { padding ->
            FriendList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                friends = friends,
                stickyHeaders = stickyHeaders,
                onStickyHeaderAction = onStickyHeaderAction,
                onFriendClick = onFriendClick,
                onFriendLongClick = onFriendLongClick,
            )
        }
    )
}

@Preview(
    showBackground = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun Preview() {
    val friends = mapOf(
        "In Game" to List(10) {
            SteamFriend(
                id = it.toLong(),
                name = "Friend Name $it",
                avatar = null,
                relation = EFriendRelationship.Friend,
                state = EPersonaState.from(1),
                gameAppId = 440,
                gameName = "Team Fortress 2",
                lastLogOn = 0,
                lastLogOff = 0,
                stateFlags = EPersonaStateFlag.from(2048),
                typingTs = 0,
                lastMessage = "Beans are yummy!",
                lastMessageTime = 0,
                newMessageCount = it,
                nickname = "Nick Name $it",
            )
        }
    )
    VapullaTheme {
        HomeScreenContent(
            friends = friends,
            stickyHeaders = setOf(),
            onStickyHeaderAction = { },
            onNavDrawerAction = { },
            onFriendClick = { },
            onFriendLongClick = { },
        )
    }
}