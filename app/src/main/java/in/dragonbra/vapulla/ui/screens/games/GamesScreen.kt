package `in`.dragonbra.vapulla.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.search.SearchAppBar
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.screens.games.components.GameBottomSheet
import `in`.dragonbra.vapulla.ui.screens.games.components.GameListItem
import `in`.dragonbra.vapulla.ui.screens.games.components.GamesFloatingToolbar
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onNavDrawerAction: () -> Unit,
    onGameClick: (Int) -> Unit,
) {
    val steamApps = viewModel.steamApps.collectAsLazyPagingItems()
    val appTypes by viewModel.appTypes.collectAsStateWithLifecycle()
    val localAccountId by viewModel.localAccountId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()

    GamesScreenContent(
        steamApps = steamApps,
        appTypes = appTypes,
        localAccountId = localAccountId,
        searchQuery = searchQuery,
        onNavDrawerAction = onNavDrawerAction,
        onSearchQuery = viewModel::updateSearchQuery,
        onAppTypeClicked = viewModel::updateAppTypes,
        onGameClick = onGameClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesScreenContent(
    steamApps: LazyPagingItems<SteamAppListItem>,
    appTypes: ImmutableSet<SteamApp.AppType>,
    localAccountId: Long?,
    searchQuery: String,
    onNavDrawerAction: () -> Unit,
    onAppTypeClicked: (SteamApp.AppType) -> Unit,
    onSearchQuery: (String) -> Unit,
    onGameClick: (Int) -> Unit = {},
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    LaunchedEffect(textFieldState.text.toString()) {
        onSearchQuery(textFieldState.text.toString())
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
                        // The paged query is already filtered by searchQuery.
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(
                                count = steamApps.itemCount,
                                key = steamApps.itemKey { it.id },
                            ) { index ->
                                steamApps[index]?.let { app ->
                                    GameListItem(
                                        app = app,
                                        shape = groupedListShape(index, steamApps.itemCount),
                                        localAccountId = localAccountId,
                                        onGameClicked = { onGameClick(app.id) },
                                    )
                                }
                            }

                            if (steamApps.itemCount == 0 &&
                                steamApps.loadState.refresh !is LoadState.Loading
                            ) {
                                item {
                                    SearchResultMessage(
                                        message = "No games found matching \"$searchQuery\"",
                                    )
                                }
                            }
                        }
                    } else {
                        SearchResultMessage(message = "Start typing to search games")
                    }
                },
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues.exceptBottom())) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = FloatingToolbarDefaults.ScreenOffset +
                            FloatingToolbarDefaults.ContainerSize + 24.dp +
                            paddingValues.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    stickyHeader {
                        ListHeader(
                            count = steamApps.itemCount,
                            isRefreshing = steamApps.loadState.refresh is LoadState.Loading,
                        )
                    }

                    items(
                        count = steamApps.itemCount,
                        key = steamApps.itemKey { it.id },
                    ) { index ->
                        steamApps[index]?.let { app ->
                            GameListItem(
                                app = app,
                                shape = groupedListShape(index, steamApps.itemCount),
                                localAccountId = localAccountId,
                                onGameClicked = { onGameClick(app.id) },
                            )
                        }
                    }

                    if (steamApps.loadState.append is LoadState.Loading) {
                        item { LoadingBox(modifier = Modifier.fillMaxWidth()) }
                    }
                }

                GamesFloatingToolbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom =
                                FloatingToolbarDefaults.ScreenOffset +
                                    paddingValues.calculateBottomPadding(),
                        ),
                    appTypes = appTypes,
                    onAppTypeClicked = onAppTypeClicked,
                    onFilterClick = { showSheet = true },
                )

                if (showSheet) {
                    GameBottomSheet(
                        sheetState = sheetState,
                        appTypes = appTypes,
                        onDismissRequest = { showSheet = false },
                        onAppTypeClicked = onAppTypeClicked,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListHeader(count: Int, isRefreshing: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$count titles",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isRefreshing) {
                LoadingIndicator(modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Preview
@Composable
private fun GamesScreenContentPreview() {
    val sampleApps = flowOf(
        PagingData.from(
            listOf(
                SteamAppListItem(
                    id = 440,
                    name = "Team Fortress 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1191999600,
                    metacriticScore = 92,
                    developer = "Valve",
                    publisher = "Valve",
                ),
                SteamAppListItem(
                    id = 730,
                    name = "Counter-Strike 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1695254400,
                    metacriticScore = 87,
                    developer = "Valve",
                    publisher = "Valve",
                ),
                SteamAppListItem(
                    id = 570,
                    name = "Dota 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1373414400,
                    metacriticScore = 90,
                    developer = "Valve",
                    publisher = "Valve",
                ),
            ),
        ),
    ).collectAsLazyPagingItems()

    VapullaTheme {
        GamesScreenContent(
            steamApps = sampleApps,
            appTypes = persistentSetOf(),
            localAccountId = null,
            searchQuery = "",
            onNavDrawerAction = {},
            onAppTypeClicked = {},
            onSearchQuery = {},
        )
    }
}
