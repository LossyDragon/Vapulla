package `in`.dragonbra.vapulla.compose.screens.home

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.components.ScrollToButton
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.VapullaProfileDialog
import `in`.dragonbra.vapulla.compose.components.pullrefresh.ExperimentalMaterialApi
import `in`.dragonbra.vapulla.compose.components.pullrefresh.PullRefreshIndicator
import `in`.dragonbra.vapulla.compose.components.pullrefresh.pullRefresh
import `in`.dragonbra.vapulla.compose.components.pullrefresh.rememberPullRefreshState
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.getAccountStatusColor
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.model.FriendListGroup
import `in`.dragonbra.vapulla.model.FriendListItem
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        searchTextState = viewModel.searchText,
        onChatSelected = onChatSelected,
        onLogout = viewModel::onLogout,
        onPersonAdd = viewModel::onAddFriend,
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
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    searchTextState: MutableStateFlow<TextFieldValue>,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onLogout: () -> Unit,
    onPersonAdd: () -> Unit,
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
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

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
        onStatusChange = onStatusChange,
        onPersonAdd = onPersonAdd,
        onSettings = onSettings,
        onLogout = onLogout,
        onDismiss = {
            subMenuDialog = false
        }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            VapullaAppbar(
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSearchOpened) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }

                    IconButton(onClick = { subMenuDialog = true }) {
                        val borderStroke = BorderStroke(1.dp, getAccountStatusColor(state.status))
                        StaticImage(
                            modifier = Modifier
                                .border(borderStroke, iconSmallCornerShape)
                                .clip(iconSmallCornerShape),
                            url = getAvatarUrl(state.avatarHash)
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
        val pullRefreshState = rememberPullRefreshState(state.isRefreshing, onRefresh)

        Box(
            modifier = Modifier
                .pullRefresh(pullRefreshState)
                .padding(paddingValues)
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
                                modifier = Modifier.animateItemPlacement(),
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

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

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
            searchTextState = MutableStateFlow(TextFieldValue("")),
            onChatSelected = {},
            onLogout = {},
            onPersonAdd = {},
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
