package `in`.dragonbra.vapulla.ui.screens.networklog

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import `in`.dragonbra.vapulla.service.NetworkLogTotal
import `in`.dragonbra.vapulla.ui.composables.BackButton
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

/** Rows from the top within which the list still follows new packets. */
private const val FOLLOW_THRESHOLD = 3

/**
 * Live view of the packets JavaSteam sends and receives, newest first. Opened from Under
 * the Hood. Backed directly by
 * [NetworkLog], which the service feeds through the client's debug network listener. The
 * list follows new packets while it sits near the top and stays put once scrolled away.
 *
 * Service calls are listed by method name, and tapping a packet opens [PacketDetailSheet].
 *
 * The list only holds the most recent packets. The totals view counts every packet since the
 * log was cleared, grouped by message type.
 */
@Composable
fun NetworkLogScreen(onBack: () -> Unit) {
    val entries by NetworkLog.entries.collectAsStateWithLifecycle()
    val totals by NetworkLog.totals.collectAsStateWithLifecycle()

    // Largest first: the point of the view is to see what the traffic is made of.
    val sortedTotals = remember(totals) {
        totals.values.sortedByDescending { it.bytes }.toImmutableList()
    }

    NetworkLogContent(
        entries = entries,
        totals = sortedTotals,
        onBack = onBack,
        onClear = NetworkLog::clear,
    )
}

@Composable
private fun NetworkLogContent(
    entries: ImmutableList<NetworkLogEntry>,
    totals: ImmutableList<NetworkLogTotal>,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    var showTotals by rememberSaveable { mutableStateOf(false) }
    var opened by remember { mutableStateOf<NetworkLogEntry?>(null) }

    opened?.let { entry -> PacketDetailSheet(entry = entry, onDismiss = { opened = null }) }
    val bytesIn = totals.sumOf { it.bytesIn }
    val bytesOut = totals.sumOf { it.bytesOut }
    val packets = totals.sumOf { it.packetsIn + it.packetsOut }
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
                navigationIcon = { BackButton(onClick = onBack) },
                actions = {
                    IconButton(onClick = { showTotals = !showTotals }) {
                        if (showTotals) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "Show packets",
                            )
                        } else {
                            Icon(Icons.Default.BarChart, contentDescription = "Show totals")
                        }
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear log")
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = scrolledAway && !showTotals,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
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
                InfoPill(text = bytesIn.toFileSize(), icon = Icons.Default.ArrowDownward)
                InfoPill(text = bytesOut.toFileSize(), icon = Icons.Default.ArrowUpward)
                InfoPill(
                    text = if (showTotals) {
                        "$packets packets · ${totals.size} types"
                    } else {
                        "last ${entries.size} of $packets"
                    },
                )
            }

            if (entries.isEmpty() && totals.isEmpty()) {
                SearchResultMessage(message = "No packets yet")
            } else if (showTotals) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(totals, key = { _, total -> total.msg }) { index, total ->
                        TotalRow(total = total, shape = groupedListShape(index, totals.size))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding, extraBottom = FabClearance),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val newestFirst = entries.asReversed()
                    itemsIndexed(newestFirst, key = { _, entry -> entry.id }) { index, entry ->
                        LogRow(
                            entry = entry,
                            shape = groupedListShape(index, newestFirst.size),
                            onClick = { opened = entry },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalRow(total: NetworkLogTotal, shape: Shape) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = total.msg.name,
                    style = MaterialTheme.typography.bodyMedium
                        .copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${total.packetsIn} in · ${total.packetsOut} out",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = total.bytes.toFileSize(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun LogRow(entry: NetworkLogEntry, shape: Shape, onClick: () -> Unit) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Show packet", onClick = onClick),
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
            // A service call is named by its method; the EMsg only says that it is one.
            Text(
                text = entry.label,
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
            totals = persistentListOf(
                NetworkLogTotal(EMsg.ClientLicenseList, packetsIn = 1, bytesIn = 20_480),
                NetworkLogTotal(EMsg.ClientHeartBeat, packetsOut = 12, bytesOut = 288),
            ),
            onBack = {},
            onClear = {},
        )
    }
}
