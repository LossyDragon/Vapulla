package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.components.MinContrastOfPrimaryVsSurface
import `in`.dragonbra.vapulla.compose.components.ScrollBackUp
import `in`.dragonbra.vapulla.compose.components.VapullaToolbar
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
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onProfileSelected: (friend: FriendListItem) -> Unit
) {
    val state by viewModel::homeState

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.onPostCreate(lifecycleOwner)
    }

    HomeScreenContent(
        state = state,
        onRefresh = { viewModel.onEvent(HomeEvent.SwipeRefresh(true)) },
        onStatusChange = { viewModel.onEvent(HomeEvent.StatusChange(it)) },
        onPersonAdd = { viewModel.onEvent(HomeEvent.AddFriend) },
        onSettings = { viewModel.onEvent(HomeEvent.Settings) },
        onLogout = { viewModel.onEvent(HomeEvent.Logout) },
        onChatSelected = { onChatSelected(it) },
        onProfileSelected = { onProfileSelected(it) }
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    onRefresh: () -> Unit,
    onStatusChange: (EPersonaState) -> Unit,
    onPersonAdd: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onProfileSelected: (friend: FriendListItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    Surface {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HomeScreenDrawer(
                    state,
                    drawerState,
                    { onStatusChange(it) },
                    onPersonAdd,
                    onSettings,
                    onLogout
                )
            }
        ) {
            Scaffold(
                topBar = {
                    VapullaToolbar(
                        drawerState = drawerState,
                        actions = {
                            IconButton(onClick = { TODO() }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search"
                                )
                            }
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

                    LaunchedEffect(state.friendsList) {
                        Timber.d("Recomping List")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // TODO: Sticky Header
                        items(state.friendsList, key = { it.id }) { friend ->
                            FriendItem(
                                modifier = Modifier.animateItemPlacement(),
                                friend = friend,
                                onClickChat = { onChatSelected(friend) },
                                onClickProfile = { onProfileSelected(friend) },
                                onClickAccept = { TODO() },
                                onClickIgnore = { TODO() },
                                onClickBlock = { TODO() }
                            )
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
                                listState.scrollToItem(0)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val context = LocalContext.current
        CoilImage(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp)),
            imageRequest = {
                ImageRequest.Builder(context)
                    .data(Utils.getAvatarUrl(state.avatarHash))
                    .crossfade(true)
                    .build()
            },
            previewPlaceholder = R.drawable.vapulla,
            imageOptions = ImageOptions(
                requestSize = IntSize(150, 150)
            )
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
            state = HomeState(friendsList = friendsList),
            onRefresh = {},
            onStatusChange = {},
            onPersonAdd = {},
            onSettings = {},
            onLogout = {},
            onChatSelected = {},
            onProfileSelected = {}
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
