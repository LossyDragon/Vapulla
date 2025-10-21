package `in`.dragonbra.vapulla.ui.screens.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import `in`.dragonbra.vapulla.data.dao.SteamAppDao
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.manager.AccountManager
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
    val localAccountId by viewModel.localAccountId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val uriHandler = LocalUriHandler.current


    val view = LocalView.current
    if (view.isInEditMode) {
        showSheet = true
    }

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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(text = "Filter") },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null
                    )
                },
                onClick = { showSheet = !showSheet }
            )
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
                        key = steamApps.itemKey { it.id }
                    ) { index ->
                        steamApps[index]?.let { app ->
                            GameListItem(
                                app = app,
                                localAccountId = localAccountId,
                                onGameClicked = {
                                    uriHandler.openUri(Utils.Constants.BASE_STEAM_STORE_URL + app.id)
                                }
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
                    ModalBottomSheet(
                        onDismissRequest = { showSheet = false },
                        sheetState = sheetState,
                        content = {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center,
                                content = {
                                    Text(
                                        text = "Not all chips will filter anything",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SteamApp.AppType.entries.forEach { category ->
                                    val selected = viewModel.appTypes.value.contains(category)
                                    FilterChip(
                                        modifier = Modifier.padding(4.dp),
                                        selected = selected,
                                        onClick = { viewModel.updateAppTypes(category) },
                                        label = { Text(category.name.capitalize()) },
                                        leadingIcon = if (selected) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    )
}

private val previewModule = module {
    single<AccountManager> { AccountManager(androidContext()) }
    single<SteamAppDao> { mockSteamAppDao }
    viewModel { GamesViewModel(get(), get()) }
}

@Preview
@Composable
private fun Preview() {
    val context = LocalContext.current

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