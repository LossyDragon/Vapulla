package `in`.dragonbra.vapulla.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.SearchAppBar
import `in`.dragonbra.vapulla.ui.composables.SearchResultMessage
import `in`.dragonbra.vapulla.ui.screens.games.components.GameListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.flow.flowOf

@Composable
fun GamesScreen(
    viewmodel: GamesViewModel,
    onNavDrawerAction: () -> Unit,
) {
    val steamApps = viewmodel.steamApps.collectAsLazyPagingItems()
    val searchQuery by viewmodel.searchQuery.collectAsState()
    GamesScreenContent(
        steamApps = steamApps,
        searchQuery = searchQuery,
        onSearchQueryChange = viewmodel::updateSearchQuery,
        onNavDrawerAction = onNavDrawerAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GamesScreenContent(
    steamApps: LazyPagingItems<SteamApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavDrawerAction: () -> Unit,
) {
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(textFieldState.text.toString()) {
        onSearchQueryChange(textFieldState.text.toString())
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
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = filteredApps,
                                key = { index -> index.id }
                            ) { app ->
                                Text(app.name)
                            }

                            if (filteredApps.isEmpty()) {
                                item {
                                    SearchResultMessage(message = "No friends found matching \"$searchQuery\"")
                                }
                            }
                        }
                    } else {
                        SearchResultMessage(message = "Start typing to search games")
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 8.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    count = steamApps.itemCount,
                    key = steamApps.itemKey { it.id }
                ) { index ->
                    steamApps[index]?.let { app ->
                        GameListItem(
                            app = app,
                            onGameClicked = {
                                uriHandler.openUri(Utils.Constants.BASE_STEAM_STORE_URL + app.id)
                            }
                        )
                    }
                }

                when {
                    steamApps.loadState.refresh is LoadState.Loading -> {
                        item { LoadingBox() }
                    }

                    steamApps.loadState.append is LoadState.Loading -> {
                        item { LoadingBox() }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        val fakeSteamApps = listOf(
            SteamApp(id = 1, name = "Counter-Strike 2", packageId = 100),
            SteamApp(id = 2, name = "Dota 2", packageId = 101),
            SteamApp(id = 3, name = "Team Fortress 2", packageId = 102),
            SteamApp(id = 4, name = "Portal 2", packageId = 103),
            SteamApp(id = 5, name = "Half-Life 2", packageId = 104),
        )
        val pagingData = flowOf(PagingData.from(fakeSteamApps))
        GamesScreenContent(
            steamApps = pagingData.collectAsLazyPagingItems(),
            searchQuery = "",
            onSearchQueryChange = { },
            onNavDrawerAction = { },
        )
    }
}