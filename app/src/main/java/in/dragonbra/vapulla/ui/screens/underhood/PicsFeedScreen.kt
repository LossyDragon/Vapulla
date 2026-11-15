package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.PicsChange
import `in`.dragonbra.vapulla.service.PicsFeed
import `in`.dragonbra.vapulla.service.PicsFeedStatus
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.FollowNewestEffect
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Apps and packages changing anywhere on Steam, newest first, from `picsGetChangesSince`.
 * The library sync makes that call anyway and keeps only what is ours; this shows the rest.
 * While the screen is open the poll runs every few seconds rather than once a minute.
 */
@Composable
fun PicsFeedScreen(onBack: () -> Unit, onOpen: (id: Int, isApp: Boolean) -> Unit) {
    val status by PicsFeed.status.collectAsStateWithLifecycle()
    val entries by PicsFeed.entries.collectAsStateWithLifecycle()

    // Only while the screen is in front: an app left in the background with the feed open
    // has nobody watching it.
    LifecycleStartEffect(Unit) {
        PicsFeed.watching.value = true
        onStopOrDispose { PicsFeed.watching.value = false }
    }

    PicsFeedContent(
        status = status,
        entries = entries,
        onBack = onBack,
        onClear = PicsFeed::clear,
        onOpen = onOpen,
    )
}

@Composable
private fun PicsFeedContent(
    status: PicsFeedStatus,
    entries: ImmutableList<PicsChange>,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onOpen: (id: Int, isApp: Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    FollowNewestEffect(listState, entries.lastOrNull()?.id)

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
                Hint(text = note, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }

            if (entries.isEmpty()) {
                EmptyMessage(
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
                            index = index,
                            count = newestFirst.size,
                            onClick = { onOpen(change.targetId, change.isApp) },
                            onHistory = { uriHandler.openUri(change.historyUrl()) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Where SteamDB lists what each change to this app or package changed. Steam only says that
 * something changed, not what, so the feed cannot show it itself.
 */
private fun PicsChange.historyUrl(): String =
    "https://steamdb.info/${if (isApp) "app" else "sub"}/$targetId/history/"

/**
 * One change. Tapping it opens what Steam holds on the app or package now; the button opens
 * its history on SteamDB.
 */
@Composable
private fun ChangeRow(
    change: PicsChange,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onHistory: () -> Unit,
) {
    val kind = if (change.isApp) "App" else "Package"

    GroupedListItem(
        index = index,
        count = count,
        color = if (change.inLibrary) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        onClick = onClick,
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
                contentDescription = kind,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = change.name.decodeHtml().ifEmpty { "$kind ${change.targetId}" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        // The ID is the title when there is no name to show.
                        "$kind ${change.targetId}".takeIf { change.name.isNotEmpty() },
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
            IconButton(onClick = onHistory, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "What changed, on SteamDB",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
            onOpen = { _, _ -> },
        )
    }
}
