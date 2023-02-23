package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.request.ImageRequest
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.components.*
import `in`.dragonbra.vapulla.compose.components.ScrollBackUp
import `in`.dragonbra.vapulla.compose.components.pullrefresh.ExperimentalMaterialApi
import `in`.dragonbra.vapulla.compose.components.pullrefresh.PullRefreshIndicator
import `in`.dragonbra.vapulla.compose.components.pullrefresh.pullRefresh
import `in`.dragonbra.vapulla.compose.components.pullrefresh.rememberPullRefreshState
import `in`.dragonbra.vapulla.compose.ui.theme.Shapes
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
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

    HomeScreenContent(
        state = state,
        onRefresh = { viewModel.onEvent(HomeEvent.SwipeRefresh(true)) },
        onChatSelected = { onChatSelected(it) },
        onProfileSelected = { onProfileSelected(it) },
        onGameTouch = { viewModel.gameSchemaManager.touch(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun HomeScreenContent(
    state: HomeState,
    onRefresh: () -> Unit,
    onGameTouch: (friendId: Int) -> Unit,
    onChatSelected: (friend: FriendListItem) -> Unit,
    onProfileSelected: (friend: FriendListItem) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() } // TODO Not used
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    Surface {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                HomeScreenDrawer(drawerState)
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackBarHostState) },
                topBar = {
                    // TODO search
                    CenterAlignedTopAppBar(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .shadow(elevation = 3.dp, shape = Shapes.medium),
                        title = {
                            Text(
                                text = stringResource(id = R.string.app_name),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Toggle Drawer Menu"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { /* TODO */ }) {
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
                    val friends by state.list.collectAsState()

                    LaunchedEffect(key1 = friends, block = {
                        Timber.d("UPDATE: ${friends.size}")
                    })

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(friends) { friend ->
                            if (friend.gameAppId > 0) {
                                onGameTouch(friend.gameAppId)
                            }

                            FriendItem(
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
    drawerState: DrawerState
) {
    val scope = rememberCoroutineScope()
    val items = listOf("Online", "Away", "Invisible")
    val selectedItem = remember { mutableStateOf(items[0]) }

    val avatarUrl = Utils.getAvatarUrl("f166a7f5d9038b4e9414dd77045e5b55002df73f")
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dominantColorState = rememberDominantColorState(
        defaultColor = MaterialTheme.colorScheme.surface,
        isColorValid = {
            it.contrastAgainst(surfaceColor) >= MinContrastOfPrimaryVsSurface
        }
    )

    LaunchedEffect(avatarUrl) {
        dominantColorState.updateColorsFromImageUrl(avatarUrl)
    }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight()
    ) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .verticalGradientScrim(
                    color = dominantColorState.color.copy(alpha = 0.15f),
                    startYPercentage = 1f,
                    endYPercentage = 0f
                ),
            shape = Shapes.extraLarge
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(.25f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val context = LocalContext.current
                    CoilImage(
                        modifier = Modifier.size(150.dp),
                        imageRequest = {
                            ImageRequest.Builder(context)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build()
                        },
                        previewPlaceholder = R.drawable.vapulla,
                        imageOptions = ImageOptions(
                            requestSize = IntSize(150, 150)
                        )
                    )

                    Text("Some Cool Name")
                }

                Spacer(Modifier.height(36.dp))
                items.forEach { item ->
                    NavigationDrawerItem(
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        icon = {
                            val tint =
                                if (item == selectedItem.value) Color.Black else Color.Gray
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null,
                                tint = tint
                            )
                        },
                        label = { Text(item) },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem.value = item
                        }
                    )
                }

                Divider(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .fillMaxWidth()
                )

                Column(
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Add Freind", fontSize = 18.sp)
                        }
                    }
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Settings", fontSize = 18.sp)
                        }
                    }
                    TextButton(
                        modifier = Modifier.padding(vertical = 2.dp),
                        onClick = { /*TODO*/ }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("Log Out", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_HomeScreenContent() {
    val state = HomeState()
    VapullaTheme {
        HomeScreenContent(
            state = state,
            onRefresh = {},
            onChatSelected = {},
            onProfileSelected = {},
            onGameTouch = {}
        )
    }
}

@Preview
@Composable
private fun Preview_HomeScreenDrawer() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    VapullaTheme {
        HomeScreenDrawer(drawerState = drawerState)
    }
}
