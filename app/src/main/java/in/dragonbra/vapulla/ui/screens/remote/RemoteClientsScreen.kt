package `in`.dragonbra.vapulla.ui.screens.remote

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.RemoteApp
import `in`.dragonbra.vapulla.service.api.RemoteClient
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.MenuButton
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.SkeletonList
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchField
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import kotlinx.collections.immutable.persistentListOf

/**
 * The account's Steam clients on other machines, and for the one picked which games are
 * installed there and how its downloads are getting on. Looking only: nothing here changes
 * anything on the other machine.
 */
@Composable
fun RemoteClientsScreen(viewModel: RemoteClientsViewModel, onNavDrawerAction: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Downloads only move on screen while someone is looking at it.
    LifecycleResumeEffect(viewModel) {
        viewModel.startWatching()
        onPauseOrDispose { viewModel.stopWatching() }
    }

    RemoteClientsContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onSelect = viewModel::select,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteClientsContent(
    uiState: RemoteClientsUiState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Clients") },
                navigationIcon = { MenuButton(onClick = onNavDrawerAction) },
            )
        },
    ) { padding ->
        PullToRefreshContent(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            val client = uiState.selected

            when {
                uiState.isLoading && uiState.clients.isEmpty() ->
                    SkeletonList(contentPadding = listContentPadding(padding))

                uiState.error != null && uiState.clients.isEmpty() ->
                    RefreshableMessage(message = uiState.error)

                client == null -> RefreshableMessage(
                    message = "No Steam client is running on another machine of yours",
                )

                else -> {
                    val shown = remember(uiState.games, filter) {
                        uiState.games.filter { it.name.contains(filter.trim(), ignoreCase = true) }
                    }
                    // Downloads first, then what is installed, then the rest.
                    val downloading = shown.filter { it.changing }
                    val installed = shown.filter { it.installed && !it.changing }
                    val available = shown.filter { !it.installed && !it.changing }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listContentPadding(padding),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        item(key = "clients") {
                            ClientHeader(
                                clients = uiState.clients,
                                selected = client,
                                games = uiState.games,
                                onSelect = onSelect,
                            )
                        }
                        item(key = "filter") {
                            SearchField(
                                value = filter,
                                onValueChange = { filter = it },
                                placeholder = "Game name",
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }

                        if (uiState.error != null) {
                            item(key = "error") { Hint(text = uiState.error) }
                        }

                        gameSection("Downloading", downloading)
                        gameSection("Installed", installed)
                        gameSection("Not installed", available)
                    }
                }
            }
        }
    }
}

/** A titled run of game rows; nothing at all when [games] is empty. */
private fun LazyListScope.gameSection(title: String, games: List<RemoteApp>) {
    if (games.isEmpty()) return

    item(key = "title-$title") {
        Text(
            text = "$title · ${games.size}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
        )
    }
    itemsIndexed(games, key = { _, game -> "$title-${game.appId}" }) { index, game ->
        GameRow(game = game, index = index, count = games.size)
    }
}

/** The clients to pick from, and what the picked one says about its machine. */
@Composable
private fun ClientHeader(
    clients: List<RemoteClient>,
    selected: RemoteClient,
    games: List<RemoteApp>,
    onSelect: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            clients.forEach { client ->
                FilterChip(
                    selected = client.id == selected.id,
                    onClick = { onSelect(client.id) },
                    label = { Text(client.machineName.ifBlank { "Steam client" }) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected.osName.isNotBlank()) InfoPill(text = selected.osName)
            InfoPill(text = "${selected.freeBytes.toFileSize()} free")
        }

        // The client names what is running by app ID; its game list has the names.
        val running = selected.runningAppIds.map { appId ->
            games.firstOrNull { it.appId == appId }?.name ?: "App $appId"
        }
        if (running.isNotEmpty()) {
            Hint(text = "Running now: ${running.joinToString()}")
        }
    }
}

@Composable
private fun GameRow(game: RemoteApp, index: Int, count: Int) {
    GroupedListItem(index = index, count = count) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (game.running) InfoPill(text = "Running")
            }

            if (game.changing) {
                // Nothing to download means the client is still working out what to do.
                if (game.bytesToDownload > 0) {
                    LinearProgressIndicator(
                        progress = {
                            (game.bytesDownloaded.toFloat() / game.bytesToDownload).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = downloadText(game),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!game.installed && game.bytesRequired > 0) {
                Text(
                    text = "${game.bytesRequired.toFileSize()} to install",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** How far a download is, how fast it goes and how long it has left, as far as each is known. */
private fun downloadText(game: RemoteApp): String = buildList {
    if (game.bytesToDownload > 0) {
        add("${game.bytesDownloaded.toFileSize()} of ${game.bytesToDownload.toFileSize()}")
    }
    when {
        game.paused -> add("Paused")
        game.downloadRate > 0 -> add("${game.downloadRate.toFileSize()}/s")
        game.queuePosition > 0 -> add("Queued, place ${game.queuePosition + 1}")
    }
    if (!game.paused && game.secondsRemaining > 0) {
        val minutes = (game.secondsRemaining + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
        add("about $minutes min left")
    }
}.joinToString(" · ").ifEmpty { "Working…" }

private const val SECONDS_PER_MINUTE = 60

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun game(id: Int, name: String, installed: Boolean, required: Long = 0) = RemoteApp(
        appId = id,
        name = name,
        installed = installed,
        running = false,
        changing = false,
        paused = false,
        bytesDownloaded = 0,
        bytesToDownload = 0,
        downloadRate = 0,
        secondsRemaining = 0,
        queuePosition = -1,
        bytesRequired = required,
    )

    VapullaTheme {
        RemoteClientsContent(
            uiState = RemoteClientsUiState(
                clients = persistentListOf(
                    RemoteClient(
                        1,
                        "Desktop",
                        "Windows 11",
                        1_111_960_150_016,
                        persistentListOf(440),
                    ),
                    RemoteClient(2, "steamdeck", "Linux 7.x", 467_580_698_624, persistentListOf()),
                ),
                selectedId = 1,
                games = persistentListOf(
                    game(620, "Portal 2", installed = false).copy(
                        changing = true,
                        bytesDownloaded = 4_200_000_000,
                        bytesToDownload = 12_600_000_000,
                        downloadRate = 31_000_000,
                        secondsRemaining = 271,
                        queuePosition = 0,
                    ),
                    game(440, "Team Fortress 2", installed = true).copy(running = true),
                    game(400, "Portal", installed = true),
                    game(1510, "Uplink", installed = false, required = 20_859_561),
                ),
                isLoading = false,
            ),
            onNavDrawerAction = {},
            onRefresh = {},
            onSelect = {},
        )
    }
}
