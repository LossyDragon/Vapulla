package `in`.dragonbra.vapulla.ui.screens.home

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendListHeader
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
    HomeScreenContent(
        friends = friends,
        onNavDrawerAction = onNavDrawerAction,
        onFriendClick = onFriendClick,
        onFriendLongClick = onFriendLongClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    friends: Map<String, List<SteamFriend>>,
    onNavDrawerAction: () -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            modifier = Modifier,
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = {
                if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clearAndSetSemantics {},
                        text = "Search",
                        textAlign = TextAlign.Center,
                    )
                }
            },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = { scope.launch { searchBarState.animateToCollapsed() } },
                        content = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Spacer(modifier = Modifier.size(24.dp))
            }
        )
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            AppBarWithSearch(
                scrollBehavior = scrollBehavior,
                state = searchBarState,
                inputField = inputField,
                navigationIcon = {
                    IconButton(
                        onClick = onNavDrawerAction,
                        content = {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                        )
                    }
                },
            )

            ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
                Text("TODO THING")
                // TODO()
            }
        },
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                content = {
                    friends.forEach { (k, v) ->
                        stickyHeader {
                            FriendListHeader(
                                header = k,
                                count = v.size,
                            )
                        }

                        itemsIndexed(v, key = { _, item -> item.id }) { idx, friend ->
                            FriendListItem(
                                modifier = Modifier
                                    .animateItem()
                                    .fillParentMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 0.dp)
                                    .combinedClickable(
                                        onClick = { onFriendClick(friend.id) },
                                        onLongClick = { onFriendLongClick(friend.id) }
                                    ),
                                friend = friend,
                            )

                            if (idx < v.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
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
            onNavDrawerAction = { },
            onFriendClick = { },
            onFriendLongClick = { },
        )
    }
}