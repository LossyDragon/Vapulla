package `in`.dragonbra.vapulla.ui.screens.networklog

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.vapulla.service.NetworkLog
import `in`.dragonbra.vapulla.service.NetworkLogEntry
import `in`.dragonbra.vapulla.ui.composables.FabClearance
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

/** Rows from the top within which the list still follows new packets. */
private const val FOLLOW_THRESHOLD = 3

/**
 * Live view of the packets JavaSteam sends and receives, newest first. Backed directly by
 * [NetworkLog], which the service feeds through the client's debug network listener. The
 * list follows new packets while it sits near the top and stays put once scrolled away.
 */
@Composable
fun NetworkLogScreen(onNavDrawerAction: () -> Unit) {
    val entries by NetworkLog.entries.collectAsStateWithLifecycle()

    NetworkLogContent(
        entries = entries,
        onNavDrawerAction = onNavDrawerAction,
        onClear = NetworkLog::clear,
    )
}

@Composable
private fun NetworkLogContent(
    entries: ImmutableList<NetworkLogEntry>,
    onNavDrawerAction: () -> Unit,
    onClear: () -> Unit,
) {
    val incoming = entries.count { it.incoming }
    val outgoing = entries.size - incoming
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrolledAway by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >
                FOLLOW_THRESHOLD
        }
    }

    // Keyed items keep the first visible row anchored when new rows are inserted above it,
    // so the list would quietly drift down. Snap back unless the user has scrolled away.
    LaunchedEffect(entries.firstOrNull()?.id, entries.lastOrNull()?.id) {
        if (listState.firstVisibleItemIndex <= FOLLOW_THRESHOLD && !listState.isScrollInProgress) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Log") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
                actions = {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear log")
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = scrolledAway, enter = scaleIn(), exit = scaleOut()) {
                FloatingActionButton(onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                }) {
                    Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = "Back to top")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                InfoPill(text = "$incoming in", icon = Icons.Default.ArrowDownward)
                InfoPill(text = "$outgoing out", icon = Icons.Default.ArrowUpward)
                InfoPill(text = "last ${entries.size}")
            }

            if (entries.isEmpty()) {
                SearchResultMessage(message = "No packets yet")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding, extraBottom = FabClearance),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val newestFirst = entries.asReversed()
                    itemsIndexed(newestFirst, key = { _, entry -> entry.id }) { index, entry ->
                        LogRow(entry = entry, shape = groupedListShape(index, newestFirst.size))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: NetworkLogEntry, shape: Shape) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (entry.incoming) {
                    Icons.Default.ArrowDownward
                } else {
                    Icons.Default.ArrowUpward
                },
                contentDescription = if (entry.incoming) "Incoming" else "Outgoing",
                modifier = Modifier.size(16.dp),
                tint = if (entry.incoming) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
            Text(
                text = entry.msg.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = entry.size.toLong().toFileSize(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = entry.timestamp.toTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun entry(id: Long, incoming: Boolean, msg: EMsg, size: Int) = NetworkLogEntry(
        id = id,
        timestamp = 1_760_722_982_000 + id * 1_000,
        incoming = incoming,
        msg = msg,
        size = size,
    )

    VapullaTheme {
        NetworkLogContent(
            entries = persistentListOf(
                entry(0, false, EMsg.ClientLogon, 512),
                entry(1, true, EMsg.ClientLogOnResponse, 148),
                entry(2, true, EMsg.ClientLicenseList, 20_480),
                entry(3, true, EMsg.ClientFriendsList, 3_072),
                entry(4, true, EMsg.ClientPersonaState, 1_536),
                entry(5, false, EMsg.ClientHeartBeat, 24),
            ),
            onNavDrawerAction = {},
            onClear = {},
        )
    }
}
