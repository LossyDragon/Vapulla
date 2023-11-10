package `in`.dragonbra.vapulla.compose.screens.home

import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.components.MinContrastOfPrimaryVsSurface
import `in`.dragonbra.vapulla.compose.components.ScrollToButton
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.contrastAgainst
import `in`.dragonbra.vapulla.compose.components.pullrefresh.ExperimentalMaterialApi
import `in`.dragonbra.vapulla.compose.components.pullrefresh.PullRefreshIndicator
import `in`.dragonbra.vapulla.compose.components.pullrefresh.pullRefresh
import `in`.dragonbra.vapulla.compose.components.pullrefresh.rememberPullRefreshState
import `in`.dragonbra.vapulla.compose.components.rememberDominantColorState
import `in`.dragonbra.vapulla.compose.components.verticalGradientScrim
import `in`.dragonbra.vapulla.compose.screens.chat.ChatActivity
import `in`.dragonbra.vapulla.compose.screens.profile.ProfileActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.friendOnline
import `in`.dragonbra.vapulla.compose.ui.theme.getAccountStatusColor
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl
import `in`.dragonbra.vapulla.model.FriendListItem
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
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val keyboard = LocalSoftwareKeyboardController.current

    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeScreenDrawer(
                state = state,
                imageLoader = imageLoader,
                drawerState = drawerState,
                onStatusChange = onStatusChange,
                onPersonAdd = onPersonAdd,
                onSettings = onSettings,
                onLogout = onLogout
            )
        }
    ) {
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
        val listState = rememberLazyListState()

        LaunchedEffect(state.isSearching) {
            if (state.isSearching) {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                VapullaAppbar(
                    scrollBehavior = scrollBehavior,
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

                // TODO when swap happens, it seems it forget the collapse status
                val collapsedState = remember(state.filteredFriendsList) {
                    state.filteredFriendsList.map { it.collapsed }.toMutableStateList()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    state.filteredFriendsList.forEachIndexed { index, group ->
                        val collapsed = collapsedState[index]
                        stickyHeader(
                            key = "header_$index",
                            contentType = group.headerTitle
                        ) {
                            StickyHeaderItem(
                                isCollapsed = collapsed,
                                header = group.headerTitle,
                                count = group.headerCount,
                                onHeaderAction = {
                                    collapsedState[index] = !collapsed
                                    onHeaderAction(group.headerTitle, !collapsed)
                                }
                            )
                        }

                        if (!collapsed) {
                            items(group.items, key = { it.id }) { friend ->
                                FriendItem(
                                    modifier = Modifier.animateItemPlacement(),
                                    imageLoader = imageLoader,
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
}

@Composable
private fun HomeScreenDrawer(
    state: HomeState,
    drawerState: DrawerState,
    imageLoader: ImageLoader,
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
        val avatarUrl = getAvatarUrl(state.avatarHash)
        dominantColorState.updateColorsFromImageUrl(avatarUrl)
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
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DrawerAccountInfo(state = state, imageLoader = imageLoader)
                DrawerStatusButtons(
                    drawerState = drawerState,
                    onStatusChange = onStatusChange
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
private fun DrawerAccountInfo(state: HomeState, imageLoader: ImageLoader) {
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
            imageLoader = imageLoader,
            url = getAvatarUrl(state.avatarHash)
        )

        Text(state.nickname, Modifier.padding(6.dp))
    }
}

@Composable
private fun DrawerStatusButtons(
    drawerState: DrawerState,
    onStatusChange: (EPersonaState) -> Unit
) {
    val statusButtonColors = NavigationDrawerItemDefaults.colors(
        unselectedContainerColor = Color.Transparent
    )

    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableStateOf(EPersonaState.Online) }

    NavigationDrawerItem(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = statusButtonColors,
        icon = {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = friendOnline
            )
        },
        label = { Text(EPersonaState.Online.name) },
        selected = selectedItem == EPersonaState.Online,
        onClick = {
            scope.launch { drawerState.close() }
            selectedItem = EPersonaState.Online
            onStatusChange(EPersonaState.Online)
        }
    )

    NavigationDrawerItem(
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = statusButtonColors,
        icon = {
            Icon(
                imageVector = Icons.Default.Circle,
                contentDescription = null,
                tint = friendOffline
            )
        },
        label = { Text(EPersonaState.Invisible.name) },
        selected = selectedItem == EPersonaState.Invisible,
        onClick = {
            scope.launch { drawerState.close() }
            selectedItem = EPersonaState.Invisible
            onStatusChange(EPersonaState.Invisible)
        }
    )
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
    val friendsList = (0..10).map {
        FriendListItem(
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
    val group = CollapsableStatusGroup(
        headerTitle = "Online",
        headerCount = friendsList.size,
        items = friendsList,
        collapsed = false
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

@Preview
@Composable
private fun Preview_HomeScreenDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val state = HomeState(nickname = "Some Cool Name")
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()

    VapullaTheme {
        HomeScreenDrawer(
            state = state,
            imageLoader = imageLoader,
            drawerState = drawerState,
            onStatusChange = {},
            onPersonAdd = {},
            onSettings = {},
            onLogout = {}
        )
    }
}
