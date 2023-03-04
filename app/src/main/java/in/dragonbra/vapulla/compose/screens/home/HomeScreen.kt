package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.components.MinContrastOfPrimaryVsSurface
import `in`.dragonbra.vapulla.compose.components.ScrollBackUp
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.contrastAgainst
import `in`.dragonbra.vapulla.compose.components.pullrefresh.ExperimentalMaterialApi
import `in`.dragonbra.vapulla.compose.components.pullrefresh.PullRefreshIndicator
import `in`.dragonbra.vapulla.compose.components.pullrefresh.pullRefresh
import `in`.dragonbra.vapulla.compose.components.pullrefresh.rememberPullRefreshState
import `in`.dragonbra.vapulla.compose.components.rememberDominantColorState
import `in`.dragonbra.vapulla.compose.components.verticalGradientScrim
import `in`.dragonbra.vapulla.compose.ui.theme.Shapes
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.friendOnline
import `in`.dragonbra.vapulla.compose.ui.theme.getAccountStatusColor
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onProfileSelected: (friend: FriendListItem) -> Unit
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.onPostCreate(lifecycleOwner)
    }

    HomeScreenContent(
        state = state,
        searchTextState = viewModel.searchText,
        onChatSelected = { onChatSelected(it) },
        onLogout = { viewModel.onLogout() },
        onPersonAdd = { viewModel.onAddFriend() },
        onProfileSelected = { onProfileSelected(it) },
        onRefresh = { viewModel.onSwipeRefresh(true) },
        onSearchClosed = { viewModel.setSearching(false) },
        onSearchOpened = { viewModel.setSearching(true) },
        onSettings = { viewModel.onSettings() },
        onStatusChange = { viewModel.onStatusUpdate(it) }
    )
}

@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
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
    onStatusChange: (EPersonaState) -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val keyboard = LocalSoftwareKeyboardController.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeScreenDrawer(
                state = state,
                drawerState = drawerState,
                onStatusChange = { onStatusChange(it) },
                onPersonAdd = onPersonAdd,
                onSettings = onSettings,
                onLogout = onLogout
            )
        }
    ) {
        Scaffold(
            topBar = {
                VapullaAppbar(
                    drawerState = drawerState,
                    actions = {
                        IconButton(onClick = onSearchOpened) {
                            Icon(
                                imageVector = Icons.Filled.Search,
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
            val pullRefreshState = rememberPullRefreshState(state.isRefreshing, { onRefresh() })

            Box(
                modifier = Modifier
                    .pullRefresh(pullRefreshState)
                    .padding(paddingValues)
            ) {
                val listState = rememberLazyListState()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (state.isSearching) {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }

                    state.filteredFriendsList.forEach { (header, friends) ->
                        stickyHeader(contentType = header) {
                            StickyHeaderItem(header, friends.size)
                        }

                        items(friends, key = { it.id }) { friend ->
                            FriendItem(
                                modifier = Modifier.animateItemPlacement(),
                                friend = friend,
                                onClickChat = { onChatSelected(friend) },
                                onClickProfile = { onProfileSelected(friend) },
                                onClickAccept = { TODO() },
                                onClickIgnore = { TODO() }
                            )
                        }
                    }
                }

                val showUpButton by remember {
                    derivedStateOf { listState.firstVisibleItemIndex > 5 }
                }
                ScrollBackUp(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enabled = showUpButton,
                    onClicked = {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )

                PullRefreshIndicator(
                    state.isRefreshing,
                    pullRefreshState,
                    Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun HomeScreenDrawer(
    state: HomeState,
    drawerState: DrawerState,
    onStatusChange: (EPersonaState) -> Unit,
    onPersonAdd: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dominantColorState = rememberDominantColorState(
        defaultColor = MaterialTheme.colorScheme.surface,
        isColorValid = {
            it.contrastAgainst(surfaceColor) >= MinContrastOfPrimaryVsSurface
        }
    )

    LaunchedEffect(state.avatarHash) {
        dominantColorState.updateColorsFromImageUrl(Utils.getAvatarUrl(state.avatarHash))
    }

    ModalDrawerSheet(modifier = Modifier.fillMaxHeight()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .verticalGradientScrim(
                    color = dominantColorState.color.copy(alpha = 0.15f),
                    startYPercentage = 1f,
                    endYPercentage = 0f
                ),
            color = Color.Transparent,
            shape = Shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DrawerAccountInfo(state = state)
                DrawerStatusButtons(
                    drawerState = drawerState,
                    onStatusChange = { onStatusChange(it) }
                )
                Divider(
                    Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth()
                )
                DrawerMenuButtons(
                    onPersonAdd = onPersonAdd,
                    onSettings = onSettings,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
private fun DrawerAccountInfo(state: HomeState) {
    val borderStroke = BorderStroke(4.dp, getAccountStatusColor(state.status))
    val cornerShape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StaticImage(
            modifier = Modifier
                .size(150.dp)
                .border(borderStroke, cornerShape)
                .clip(cornerShape),
            avatarUrl = Utils.getAvatarUrl(state.avatarHash)
        )

        Text(state.nickname, Modifier.padding(6.dp))
    }
}

@Composable
private fun DrawerStatusButtons(
    drawerState: DrawerState,
    onStatusChange: (EPersonaState) -> Unit
) {
    val items = mapOf(
        EPersonaState.Online to friendOnline,
        EPersonaState.Invisible to friendOffline
    )
    val statusButtonColors = NavigationDrawerItemDefaults.colors(
        unselectedContainerColor = Color.Transparent
    )

    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf(items.entries.first()) }

    items.forEach { item ->
        NavigationDrawerItem(
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            colors = statusButtonColors,
            icon = {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    tint = item.value
                )
            },
            label = { Text(item.key.name) },
            selected = item == selectedItem,
            onClick = {
                scope.launch { drawerState.close() }
                selectedItem = item
                onStatusChange(item.key)
            }
        )
    }
}

@Composable
private fun DrawerMenuButtons(
    onPersonAdd: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val menuButtonColors = NavigationDrawerItemDefaults.colors(
        unselectedContainerColor = Color.Transparent
    )
    NavigationDrawerItem(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = menuButtonColors,
        icon = { Icon(Icons.Default.PersonAdd, null) },
        label = { Text("Add Friend", fontSize = 18.sp) },
        selected = false,
        onClick = onPersonAdd
    )

    NavigationDrawerItem(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = menuButtonColors,
        icon = { Icon(Icons.Default.Settings, null) },
        label = { Text("Settings", fontSize = 18.sp) },
        selected = false,
        onClick = onSettings
    )

    NavigationDrawerItem(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = menuButtonColors,
        icon = { Icon(Icons.Default.Logout, null) },
        label = { Text("Log Out", fontSize = 18.sp) },
        selected = false,
        onClick = onLogout
    )
}

@Preview
@Composable
private fun Preview_HomeScreenContent() {
    val friendsList = mutableListOf<FriendListItem>()
    repeat(10) {
        friendsList.add(
            FriendListItem(
                id = it.toLong(),
                state = EPersonaState.Online.code(),
                avatar = null,
                gameAppId = 440,
                gameName = "Team Fortess 2",
                lastLogOff = 0L,
                lastLogOn = 0L,
                lastMessage = null,
                lastMessageTime = null,
                name = "Name $it",
                newMessageCount = null,
                nickname = null,
                relation = 0,
                stateFlags = 0,
                typingTs = 0L
            )
        )
    }

    VapullaTheme {
        HomeScreenContent(
            state = HomeState(friendsList = mapOf("Online" to friendsList)),
            searchTextState = MutableStateFlow(TextFieldValue("")),
            onChatSelected = {},
            onLogout = {},
            onPersonAdd = {},
            onProfileSelected = {},
            onRefresh = {},
            onSearchClosed = {},
            onSearchOpened = {},
            onSettings = {},
            onStatusChange = {}
        )
    }
}

@Preview
@Composable
private fun Preview_HomeScreenDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val state = HomeState(nickname = "Some Cool Name")
    VapullaTheme {
        HomeScreenDrawer(
            state = state,
            drawerState = drawerState,
            onStatusChange = {},
            onPersonAdd = {},
            onSettings = {},
            onLogout = {}
        )
    }
}
