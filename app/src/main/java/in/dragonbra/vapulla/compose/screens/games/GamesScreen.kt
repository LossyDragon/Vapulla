package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.ScrollBackUp
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.retrofit.response.Games
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onItemClick: (Int) -> Unit
) {
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsState()

    GamesScreenContent(
        state = state,
        searchTextState = viewModel.searchText,
        onBackPressed = { activity.finish() },
        onItemClick = onItemClick,
        onSearchClosed = viewModel::isNotSearching,
        onSearchOpened = viewModel::isSearching
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GamesScreenContent(
    state: GamesState,
    searchTextState: MutableStateFlow<TextFieldValue>,
    onBackPressed: () -> Unit,
    onItemClick: (Int) -> Unit,
    onSearchOpened: () -> Unit,
    onSearchClosed: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(
        topBar = {
            VapullaAppbar(
                toolbarText = stringResource(id = R.string.title_activity_games, state.name),
                onBackPressed = {
                    onBackPressed()
                    keyboard?.show()
                },
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
        Box(modifier = Modifier.padding(paddingValues)) {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (state.isSearching) {
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                }

                items(state.filteredGamesList, key = { it.appid }) {
                    GamesListItem(
                        appId = it.appid,
                        gameName = it.name,
                        hoursTwoWeeks = it.playtime_2weeks,
                        hoursAllTime = it.playtime_forever,
                        onOverflowClick = { onItemClick(it.appid) }
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
    val gamesList = (0..12).map {
        Games(
            appid = it,
            img_icon_url = null,
            name = "Game Name: $it",
            playtime_2weeks = (0..4000).random(),
            playtime_forever = (0..4000).random()
        )
    }

    val state = GamesState(name = "Mr. Friendly", gamesList = gamesList)
    VapullaTheme {
        GamesScreenContent(
            state,
            onItemClick = {},
            onBackPressed = {},
            searchTextState = MutableStateFlow(TextFieldValue("")),
            onSearchOpened = {},
            onSearchClosed = {}
        )
    }
}
