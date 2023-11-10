package `in`.dragonbra.vapulla.compose.screens.games

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.ScrollToButton
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.ui.icons.SortAlpha
import `in`.dragonbra.vapulla.compose.ui.icons.SortNumeric
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun GamesScreen(viewModel: GamesViewModel) {
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    GamesScreenContent(
        state = state,
        searchTextState = viewModel.searchText,
        onBackPressed = { activity.finish() },
        onSortPressed = viewModel::onSortMethod,
        onItemClick = {
            Intent(Intent.ACTION_VIEW).apply {
                val url = String.format(Constants.STORE_PAGE_URL, it)
                data = Uri.parse(url)
            }.also { activity.startActivity(it) }
        },
        onSearchClosed = viewModel::isNotSearching,
        onSearchOpened = viewModel::isSearching
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesScreenContent(
    state: GamesState,
    searchTextState: MutableStateFlow<TextFieldValue>,
    onBackPressed: () -> Unit,
    onSortPressed: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSearchOpened: () -> Unit,
    onSearchClosed: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(state.sortMethod, state.isSearching) {
        scope.launch {
            listState.animateScrollToItem(0)
        }
    }

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            VapullaAppbar(
                scrollBehavior = scrollBehavior,
                toolbarText = stringResource(id = R.string.title_activity_games, state.name),
                onBackPressed = {
                    onBackPressed()
                    keyboard?.hide()
                },
                actions = {
                    IconButton(onClick = onSearchOpened) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = onSortPressed) {
                        Icon(
                            imageVector = when (state.sortMethod) {
                                SortOptions.Alphabetical -> Icons.SortAlpha
                                SortOptions.Playtime -> Icons.SortNumeric
                            },
                            contentDescription = null
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
        Box(modifier = Modifier.padding(paddingValues)) {
            if (state.filteredGamesList.isEmpty()) {
                Card(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        text = "No games to display"
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.filteredGamesList, key = { it.appid }) {
                    GameCardItem(
                        imageLoader = imageLoader,
                        appID = it.appid,
                        title = it.name,
                        recentPlayTime = it.playtimeTwoWeeks,
                        totalPlayTime = it.playtimeForever,
                        onItemClick = { onItemClick(it.appid) }
                    )
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
        }
    }
}

@Preview
@Composable
private fun Preview_GamesScreenContent() {
    val state = GamesState(name = "Mr. Friendly", filteredGamesList = listOf())
    VapullaTheme {
        GamesScreenContent(
            state,
            onItemClick = {},
            onBackPressed = {},
            onSortPressed = {},
            searchTextState = MutableStateFlow(TextFieldValue("")),
            onSearchOpened = {},
            onSearchClosed = {}
        )
    }
}
