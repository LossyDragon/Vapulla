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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.vapulla.data.dao.SteamAppDao
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.SearchAppBar
import `in`.dragonbra.vapulla.ui.composables.SearchResultMessage
import `in`.dragonbra.vapulla.ui.mock.mockSteamAppDao
import `in`.dragonbra.vapulla.ui.screens.games.components.GameListItem
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.KoinApplicationPreview
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onNavDrawerAction: () -> Unit,
) {
    val steamApps = viewModel.steamApps.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(textFieldState.text.toString()) {
        viewModel.updateSearchQuery(textFieldState.text.toString())
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
    val context = LocalContext.current
    val previewModule = module {
        single<SteamAppDao> { mockSteamAppDao }
        viewModel { GamesViewModel(get()) }
    }
    KoinApplicationPreview(
        application = {
            androidContext(context.applicationContext)
            modules(previewModule)
        },
        content = {
            VapullaTheme {
                GamesScreen(
                    viewModel = koinViewModel(),
                    onNavDrawerAction = {}
                )
            }
        }
    )
}