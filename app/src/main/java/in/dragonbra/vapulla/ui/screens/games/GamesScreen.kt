package `in`.dragonbra.vapulla.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.search.SearchAppBar
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.screens.games.components.GameBottomSheet
import `in`.dragonbra.vapulla.ui.screens.games.components.GameFilterButton
import `in`.dragonbra.vapulla.ui.screens.games.components.GameListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf

@Composable
fun GamesScreen(viewModel: GamesViewModel, onNavDrawerAction: () -> Unit) {
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesScreenContent(
    steamApps: LazyPagingItems<SteamApp>,
    appTypes: ImmutableSet<SteamApp.AppType>,
    localAccountId: Long?,
    searchQuery: String,
    onNavDrawerAction: () -> Unit,
    onAppTypeClicked: (SteamApp.AppType) -> Unit,
    onSearchQuery: (String) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),

    )

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val uriHandler = LocalUriHandler.current

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
                        val filteredApps by remember(searchQuery, steamApps.itemSnapshotList) {
                            derivedStateOf {
                                steamApps.itemSnapshotList.items.filter { app ->
                                    app.name.contains(searchQuery, ignoreCase = true)
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(
                                items = filteredApps,
                                key = { index -> index.id },
                            ) { app ->
                                Text(app.name)
                            }

                            if (filteredApps.isEmpty()) {
                                item {
                                    SearchResultMessage(
                                        message = "No friends found matching \"$searchQuery\"",
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
        floatingActionButton = {
            GameFilterButton(onClick = { showSheet = !showSheet })
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    stickyHeader {
                        ListItem(
                            headlineContent = { Text(text = "${steamApps.itemCount} games") },
                        )
                    }

                    items(
                        count = steamApps.itemCount,
                        key = steamApps.itemKey { it.id },
                    ) { index ->
                        steamApps[index]?.let { app ->
                            GameListItem(
                                app = app,
                                localAccountId = localAccountId,
                                onGameClicked = {
                                    uriHandler.openUri(
                                        Utils.Constants.BASE_STEAM_STORE_URL + app.id,
                                    )
                                },
                            )

                            if (index < steamApps.itemCount) {
                                HorizontalDivider()
                            }
                        }
                    }

                    when {
                        steamApps.loadState.refresh is LoadState.Loading -> {
                            item { LoadingBox(modifier = Modifier.fillMaxWidth()) }
                        }

                        steamApps.loadState.append is LoadState.Loading -> {
                            item { LoadingBox(modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }

                if (showSheet) {
                    GameBottomSheet(
                        sheetState = sheetState,
                        appTypes = appTypes,
                        onDismissRequest = { showSheet = !showSheet },
                        onAppTypeClicked = onAppTypeClicked,
                    )
                }
            }
        },
    )
}

@Preview
@Composable
private fun GamesScreenContentPreview() {
    val sampleApps = flowOf(
        PagingData.from(
            listOf(
                SteamApp(
                    id = 440,
                    name = "Team Fortress 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1191999600,
                    metacriticScore = 92,
                    developer = "Valve",
                    publisher = "Valve",
                ),
                SteamApp(
                    id = 730,
                    name = "Counter-Strike 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1695254400,
                    metacriticScore = 87,
                    developer = "Valve",
                    publisher = "Valve",
                ),
                SteamApp(
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
