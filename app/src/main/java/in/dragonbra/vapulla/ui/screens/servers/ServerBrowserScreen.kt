package `in`.dragonbra.vapulla.ui.screens.servers

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.javasteam.enums.ERegionCode
import `in`.dragonbra.vapulla.service.api.GameServer
import `in`.dragonbra.vapulla.service.api.ServerInfo
import `in`.dragonbra.vapulla.ui.composables.FabClearance
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Height of the game list inside the picker sheet. */
private val PICKER_HEIGHT = 480.dp

private val REGIONS = listOf(
    ERegionCode.World,
    ERegionCode.USEast,
    ERegionCode.USWest,
    ERegionCode.Europe,
    ERegionCode.Asia,
    ERegionCode.Australia,
    ERegionCode.SouthAmerica,
)

@Composable
fun ServerBrowserScreen(viewModel: ServerBrowserViewModel, onNavDrawerAction: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val gameSearch by viewModel.gameSearch.collectAsStateWithLifecycle()

    ServerBrowserContent(
        uiState = uiState,
        games = games,
        gameSearch = gameSearch,
        onNavDrawerAction = onNavDrawerAction,
        onGameSearch = viewModel::setGameSearch,
        onSelectGame = viewModel::selectGame,
        onSelectRegion = viewModel::selectRegion,
        onQuery = viewModel::query,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServerBrowserContent(
    uiState: ServerBrowserUiState,
    games: ImmutableList<ServerGame>,
    gameSearch: String,
    onNavDrawerAction: () -> Unit,
    onGameSearch: (String) -> Unit,
    onSelectGame: (ServerGame) -> Unit,
    onSelectRegion: (ERegionCode) -> Unit,
    onQuery: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // The FAB shrinks to its icon once the list is scrolled away from the top.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    if (pickerOpen) {
        GamePickerSheet(
            games = games,
            search = gameSearch,
            selected = uiState.game,
            onSearch = onGameSearch,
            onSelect = { game ->
                onSelectGame(game)
                onGameSearch("")
                pickerOpen = false
            },
            onDismiss = {
                onGameSearch("")
                pickerOpen = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Browser") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!uiState.isLoading && uiState.game != null) onQuery() },
                expanded = fabExpanded,
                icon = { Icon(Icons.Outlined.Search, contentDescription = "Query servers") },
                text = { Text(if (uiState.isLoading) "Querying…" else "Query Servers") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GameSelector(game = uiState.game, onClick = { pickerOpen = true })

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    REGIONS.forEach { region ->
                        FilterChip(
                            selected = uiState.region == region,
                            onClick = { onSelectRegion(region) },
                            label = { Text(region.name) },
                        )
                    }
                }

                if (uiState.isLoading) {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            when {
                uiState.error != null -> SearchResultMessage(message = uiState.error)

                uiState.servers.isEmpty() && !uiState.isLoading ->
                    SearchResultMessage(message = "Pick a game, then tap Query Servers")

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(
                        padding,
                        vertical = 12.dp,
                        extraBottom = FabClearance,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(uiState.servers, key = { _, server ->
                        server.address
                    }) { index, server ->
                        ServerRow(
                            server = server,
                            shape = groupedListShape(index, uiState.servers.size),
                        )
                    }
                }
            }
        }
    }
}

/** The current game as a tappable row; opens the picker sheet. */
@Composable
private fun GameSelector(game: ServerGame?, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameIcon(game = game, size = 36.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Game",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = game?.name ?: "Choose a game",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.UnfoldMore,
                contentDescription = "Change game",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Searchable list of owned games that can have servers, most played first. The list comes
 * from PICS, so it fills in as app info is ingested.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamePickerSheet(
    games: ImmutableList<ServerGame>,
    search: String,
    selected: ServerGame?,
    onSearch: (String) -> Unit,
    onSelect: (ServerGame) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Choose a game", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text("Search your library") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            )

            // A fixed height keeps the sheet steady while the search narrows the list.
            if (games.isEmpty()) {
                SearchResultMessage(
                    message = "No games match. App info may still be loading.",
                    modifier = Modifier.height(PICKER_HEIGHT),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(PICKER_HEIGHT),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(games, key = { it.appId }) { game ->
                        GamePickerRow(
                            game = game,
                            selected = game.appId == selected?.appId,
                            onClick = { onSelect(game) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GamePickerRow(game: ServerGame, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameIcon(game = game, size = 28.dp)
            Text(
                text = game.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GameIcon(game: ServerGame?, size: Dp) {
    CoilImage(
        imageModel = { game?.iconUrl.orEmpty() },
        modifier = Modifier
            .size(size)
            .clip(MaterialTheme.shapes.small),
        imageOptions = ImageOptions(contentScale = ContentScale.Crop),
        failure = { Icon(Icons.Outlined.SportsEsports, contentDescription = null) },
    )
}

@Composable
private fun ServerRow(server: GameServer, shape: Shape) {
    val info = server.info
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = info?.name ?: server.address,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (info != null) {
                    InfoPill(
                        text = "${info.players}/${info.maxPlayers}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    InfoPill(text = "${server.authedPlayers} players")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (info != null) "${info.map} · ${server.address}" else "No A2S reply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (info?.vacSecured == true) {
                    InfoPill(text = "VAC", icon = Icons.Outlined.Shield)
                }
                if (info?.passwordProtected == true) {
                    InfoPill(text = "Locked", icon = Icons.Outlined.Lock)
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun info(name: String, players: Int, locked: Boolean = false) = ServerInfo(
        name = name,
        map = "ctf_2fort",
        game = "Team Fortress",
        players = players,
        maxPlayers = 24,
        bots = 0,
        passwordProtected = locked,
        vacSecured = true,
    )

    val game = ServerGame(appId = 440, name = "Team Fortress 2", iconUrl = "")

    VapullaTheme {
        ServerBrowserContent(
            uiState = ServerBrowserUiState(
                game = game,
                region = ERegionCode.Europe,
                servers = persistentListOf(
                    GameServer("192.0.2.10", 27015, 18, info("Valve Matchmaking Server", 18)),
                    GameServer("192.0.2.11", 27015, 6, info("Friends Only", 6, locked = true)),
                    GameServer("192.0.2.12", 27016, 3),
                ),
                isLoading = true,
            ),
            games = persistentListOf(game),
            gameSearch = "",
            onNavDrawerAction = {},
            onGameSearch = {},
            onSelectGame = {},
            onSelectRegion = {},
            onQuery = {},
        )
    }
}
