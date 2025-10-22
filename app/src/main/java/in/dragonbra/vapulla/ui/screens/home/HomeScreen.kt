package `in`.dragonbra.vapulla.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.composables.search.SearchAppBar
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendList
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavDrawerAction: () -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    val friendsList by viewModel.friends.collectAsState()
    val stickyHeaders by viewModel.stickyHeaders.collectAsState()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val scope = rememberCoroutineScope()

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
                        val filteredFriends = friendsList.values.flatten().filter { friend ->
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
                                    SearchResultMessage(
                                        message = "No friends found matching \"$searchQuery\""
                                    )
                                }
                            }
                        }
                    } else {
                        SearchResultMessage(message = "Start typing to search friends")
                    }
                }
            )
        },
        content = { padding ->
            FriendList(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                friendsList = friendsList,
                stickyHeaders = stickyHeaders,
                onStickyHeaderAction = viewModel::onStickyHeaderAction,
                onFriendClick = onFriendClick,
                onFriendLongClick = onFriendLongClick,
            )
        }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        HomeScreen(
            viewModel = koinViewModel(),
            onNavDrawerAction = { },
            onFriendClick = { },
            onFriendLongClick = { },
        )
    }
}