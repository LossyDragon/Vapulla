package `in`.dragonbra.vapulla.compose.screens.games

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.compose.components.VapullaToolbar
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.stringResource
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.retrofit.response.Games

@Composable
fun GamesScreen(
    viewModel: GamesViewModel,
    onItemClick: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    GamesScreenContent(
        state = state,
        onItemClick = onItemClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesScreenContent(
    state: GamesState,
    onItemClick: (Int) -> Unit,
) {
    Scaffold(
        topBar = {
            VapullaToolbar(
                toolbarText = stringResource(id = R.string.title_activity_games, state.name),
                onSearch = { TODO() }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                val sortedList = when (state.sortDirection) {
                    GamesActivity.SORT_PLAYTIME -> state.gamesList.sortedBy { it.playtime_forever }
                    else -> state.gamesList.sortedBy { it.name.lowercase() }
                }

                items(sortedList, key = { it.appid }) {
                    GamesListItem(
                        imageUrl = it.img_icon_url,
                        appId = it.appid,
                        gameName = it.name,
                        hoursTwoWeeks = it.playtime_2weeks ?: 0,
                        hoursAllTime = it.playtime_forever,
                        onOverflowClick = { onItemClick(it.appid) }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_GamesScreenContent() {
    val gamesList = mutableListOf<Games>()
    repeat(12) {
        gamesList.add(
            Games(
                appid = it,
                img_icon_url = null,
                name = "Game Name: $it",
                playtime_2weeks = (0..4000).random(),
                playtime_forever = (0..4000).random(),
            )
        )
    }
    val state = GamesState(name = "Mr. Friendly", sortDirection = 1, gamesList = gamesList)

    VapullaTheme {
        GamesScreenContent(state, onItemClick = {})
    }
}