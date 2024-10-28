package `in`.dragonbra.vapulla.compose.screens.home

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.components.ScrollToButton
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.VapullaProfileDialog
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.model.FriendListGroup
import `in`.dragonbra.vapulla.model.FriendListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val refreshState = rememberPullToRefreshState()

    val onProfileSelected = remember<(FriendListItem) -> Unit> {
        {
            Intent(context, ProfileActivity::class.java).apply {
                putExtra(ProfileActivity.INTENT_STEAM_ID, it.id)
            }.also(context::startActivity)
        }
    }

    val onChatSelected = remember<(FriendListItem) -> Unit> {
        {
            if (it.isRequestRecipient) {
                onProfileSelected(it)
            } else {
                Intent(context, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.INTENT_STEAM_ID, it.id)
                }.also(context::startActivity)
            }
        }
    }

    HomeScreenContent(
        state = state,
        refreshState = refreshState,
        searchTextState = viewModel.searchText,
        onChatSelected = onChatSelected,
        onLogout = viewModel::onLogout,
        onPendingInvites = viewModel::onPendingInvites,
        onInviteLinks = viewModel::onInviteLinks,
        onProfileSelected = onProfileSelected,
        onRefresh = { viewModel.onSwipeRefresh(true) },
        onSearchClosed = viewModel::isNotSearching,
        onSearchOpened = viewModel::isSearching,
        onSettings = viewModel::onSettings,
        onStatusChange = viewModel::onStatusUpdate,
        onHeaderAction = viewModel::onHeaderAction
    )
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    refreshState: PullToRefreshState,
    searchTextState: MutableStateFlow<TextFieldValue>,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onLogout: () -> Unit,
    onInviteLinks: () -> Unit,
    onPendingInvites: () -> Unit,
    onProfileSelected: (friend: FriendListItem) -> Unit,
    onRefresh: () -> Unit,
    onSearchClosed: () -> Unit,
    onSearchOpened: () -> Unit,
    onSettings: () -> Unit,
    onStatusChange: (EPersonaState) -> Unit,
    onHeaderAction: (String, Boolean) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isSearching) {
        if (state.isSearching) {
            scope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    // Profile + Menu dialogs
    var subMenuDialog by remember { mutableStateOf(false) }
    VapullaProfileDialog(
        openDialog = subMenuDialog,
        state = state,
        onStatusChange = {
            onStatusChange(it)
            // subMenuDialog = false
        },
        onInvites = {
            onPendingInvites()
            subMenuDialog = false
        },
        onInviteLinks = {
            onInviteLinks()
            subMenuDialog = false
        },
        onSettings = {
            onSettings()
            subMenuDialog = false
        },
        onLogout = {
            onLogout()
            subMenuDialog = false
        },
        onDismiss = {
            subMenuDialog = false
        }
    )

    Scaffold(
        topBar = {
            VapullaAppbar(
                actions = {
                    IconButton(onClick = onSearchOpened) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { subMenuDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Search"
                        )
                    }
                },
                searchTextState = searchTextState,
                isSearching = state.isSearching,
                onSearchClose = {
                    onSearchClosed()
                    keyboard?.hide()
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            state = refreshState,
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
        ) {
            if (state.filteredFriendsList.isEmpty()) {
                Card(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        text = "No friends to display"
                    )
                }
            }

            val collapsedState = remember(state.filteredFriendsList) {
                state.filteredFriendsList.map { it.isCollapsed }.toMutableStateList()
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                state.filteredFriendsList.forEachIndexed { index, group ->
                    stickyHeader {
                        StickyHeaderItem(
                            isCollapsed = collapsedState[index],
                            header = group.groupName,
                            count = group.groupCount,
                            onHeaderAction = {
                                onHeaderAction(group.groupName, !collapsedState[index])
                                collapsedState[index] = !collapsedState[index]
                            }
                        )
                    }

                    if (!collapsedState[index]) {
                        items(group.groupList, key = { it.id }) { friend ->
                            FriendItem(
                                modifier = Modifier.animateItem(),
                                friend = friend,
                                onClickChat = { onChatSelected(friend) },
                                onClickProfile = { onProfileSelected(friend) }
                            )
                        }
                    }
                }
            }

            val showUpButton by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 5 }
            }
            ScrollToButton(
                modifier = Modifier.align(Alignment.BottomCenter),
                label = "Scroll Up",
                buttonIcon = Icons.Default.ArrowUpward,
                buttonText = "Scroll Up",
                enabled = showUpButton,
                onClicked = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_HomeScreenContent() {
    val friendsList = (0..10).map {
        FriendListItem(
            id = Random.nextLong(),
            state = EPersonaState.Online.code(),
            gameAppId = 440,
            gameName = "Team Fortress 2",
            lastLogOff = 0L,
            lastLogOn = 0L,
            name = "Name $it",
            relation = 0,
            stateFlags = 0,
            typingTs = 0L
        )
    }
    val group = FriendListGroup(
        groupName = "Online",
        groupCount = friendsList.size,
        groupList = friendsList,
        isCollapsed = false
    )

    VapullaTheme {
        HomeScreenContent(
            state = HomeState(filteredFriendsList = listOf(group)),
            refreshState = rememberPullToRefreshState(),
            searchTextState = MutableStateFlow(TextFieldValue("")),
            onChatSelected = {},
            onLogout = {},
            onInviteLinks = {},
            onPendingInvites = {},
            onProfileSelected = {},
            onRefresh = {},
            onSearchClosed = {},
            onSearchOpened = {},
            onSettings = {},
            onStatusChange = {},
            onHeaderAction = { _, _ -> }
        )
    }
}
