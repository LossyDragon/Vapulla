package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.PicsChange
import `in`.dragonbra.vapulla.service.PicsFeed
import `in`.dragonbra.vapulla.service.PicsFeedStatus
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Rows from the top within which the list still follows new changes. */
private const val FOLLOW_THRESHOLD = 3

/**
 * Apps and packages changing anywhere on Steam, newest first, from `picsGetChangesSince`.
 * The library sync makes that call anyway and keeps only what is ours; this shows the rest.
 * While the screen is open the poll runs every few seconds rather than once a minute.
 */
@Composable
fun PicsFeedScreen(onBack: () -> Unit) {
    val status by PicsFeed.status.collectAsStateWithLifecycle()
    val entries by PicsFeed.entries.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        PicsFeed.watching.value = true
        onDispose { PicsFeed.watching.value = false }
    }

    PicsFeedContent(status = status, entries = entries, onBack = onBack, onClear = PicsFeed::clear)
}

@Composable
private fun PicsFeedContent(
    status: PicsFeedStatus,
    entries: ImmutableList<PicsChange>,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Keyed rows stay put when new ones are inserted above them, so the list would drift
    // away from the top. Follow new changes unless the user has scrolled down.
    LaunchedEffect(entries.lastOrNull()?.id) {
        if (listState.firstVisibleItemIndex <= FOLLOW_THRESHOLD && !listState.isScrollInProgress) {
            listState.scrollToItem(0)
        }
    }

    UnderHoodScaffold(
        title = "PICS Change Feed",
        onBack = onBack,
        actions = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear feed")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (status.changeNumber > 0) {
                    InfoPill(
                        text = "Change ${status.changeNumber}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    InfoPill(text = "checked ${status.checkedAt.toTimeString()}")
                }
                InfoPill(text = "${entries.count { it.isApp }} apps")
                InfoPill(text = "${entries.count { !it.isApp }} packages")
            }

            status.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }

            if (entries.isEmpty()) {
                SearchResultMessage(
                    message = if (status.checks == 0) {
                        "Waiting for Steam's answer"
                    } else {
                        "Nothing has changed since the feed was opened"
                    },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val newestFirst = entries.asReversed()
                    itemsIndexed(newestFirst, key = { _, change -> change.id }) { index, change ->
                        ChangeRow(
                            change = change,
                            shape = groupedListShape(index, newestFirst.size),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: PicsChange, shape: Shape) {
    Surface(
        shape = shape,
        color = if (change.inLibrary) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (change.isApp) {
                    Icons.Outlined.SportsEsports
                } else {
                    Icons.Outlined.Inventory2
                },
                contentDescription = if (change.isApp) "App" else "Package",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.name.ifEmpty {
                        "${if (change.isApp) "App" else "Package"} ${change.targetId}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        // The ID is the title when there is no name to show.
                        "App ${change.targetId}".takeIf { change.name.isNotEmpty() },
                        "change ${change.changeNumber}",
                        "in your library".takeIf { change.inLibrary },
                        "details need a token".takeIf { change.needsToken },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = change.timestamp.toTimeString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    val now = 1_760_722_982_000

    fun change(id: Long, app: Boolean, target: Int, name: String = "", mine: Boolean = false) =
        PicsChange(
            id = id,
            timestamp = now + id * 5_000,
            isApp = app,
            targetId = target,
            changeNumber = 31_234_560 + id.toInt(),
            needsToken = !app,
            name = name,
            inLibrary = mine,
        )

    VapullaTheme {
        PicsFeedContent(
            status = PicsFeedStatus(changeNumber = 31_234_563, checkedAt = now, checks = 4),
            entries = persistentListOf(
                change(0, app = true, target = 2_456_740),
                change(1, app = true, target = 440, name = "Team Fortress 2", mine = true),
                change(2, app = false, target = 1_139_975),
                change(3, app = true, target = 3_017_860),
            ),
            onBack = {},
            onClear = {},
        )
    }
}
