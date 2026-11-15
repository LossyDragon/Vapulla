package `in`.dragonbra.vapulla.ui.screens.downloads

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.service.download.InstallLocation
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.MenuButton
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.ShimmerBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.downloads.components.UpdateAvailablePill
import `in`.dragonbra.vapulla.ui.screens.downloads.components.WaitingForWifi
import `in`.dragonbra.vapulla.ui.screens.downloads.components.fractionWith
import `in`.dragonbra.vapulla.ui.screens.downloads.components.liveProgress
import `in`.dragonbra.vapulla.ui.screens.downloads.components.statusLine
import `in`.dragonbra.vapulla.ui.screens.games.components.HEADER_ASPECT
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onNavDrawerAction: () -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val installLocations by viewModel.installLocations.collectAsStateWithLifecycle()

    DownloadsContent(
        uiState = uiState,
        installLocations = installLocations,
        onMove = viewModel::move,
        onNavDrawerAction = onNavDrawerAction,
        onOpenGame = onOpenGame,
        onOpenLibrary = onOpenLibrary,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onVerify = viewModel::verify,
        onRemove = viewModel::remove,
        onClearFinished = viewModel::clearFinished,
    )
}

@Composable
private fun DownloadsContent(
    uiState: DownloadsUiState,
    installLocations: ImmutableList<InstallLocation>,
    onMove: (Download, toRoot: String) -> Unit,
    onNavDrawerAction: () -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
    onPause: (Download) -> Unit,
    onResume: (Download) -> Unit,
    onVerify: (Download) -> Unit,
    onRemove: (Download, deleteFiles: Boolean) -> Unit,
    onClearFinished: () -> Unit,
) {
    var pendingRemove by remember { mutableStateOf<Download?>(null) }

    pendingRemove?.let { download ->
        RemoveDialog(
            download = download,
            onConfirm = { deleteFiles ->
                onRemove(download, deleteFiles)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }

    var pendingMove by remember { mutableStateOf<Download?>(null) }

    pendingMove?.let { download ->
        MoveDialog(
            download = download,
            // Everywhere but where it is now.
            locations = installLocations.filter { !download.installDir.startsWith(it.path) },
            onConfirm = { toRoot ->
                onMove(download, toRoot)
                pendingMove = null
            },
            onDismiss = { pendingMove = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = { MenuButton(onClick = onNavDrawerAction) },
                actions = {
                    IconButton(onClick = onClearFinished, enabled = uiState.finished.isNotEmpty()) {
                        Icon(Icons.Outlined.ClearAll, contentDescription = "Clear finished")
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))

            uiState.isEmpty -> EmptyState(
                modifier = Modifier.fillMaxSize().padding(padding),
                onOpenLibrary = onOpenLibrary,
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding.exceptBottom()),
                contentPadding = listContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val rowActions = RowActions(
                    onOpen = onOpenGame,
                    onPause = onPause,
                    onResume = onResume,
                    onVerify = onVerify,
                    onMove = { pendingMove = it },
                    onRemove = { pendingRemove = it },
                )

                if (uiState.waitingForWifi && uiState.active.isNotEmpty()) {
                    item(key = "waiting-for-wifi") { WaitingForWifi() }
                }

                section("Active", uiState.active, uiState, rowActions)
                section("Paused", uiState.paused, uiState, rowActions)
                section("Finished", uiState.finished, uiState, rowActions)
            }
        }
    }
}

private class RowActions(
    val onOpen: (Int) -> Unit,
    val onPause: (Download) -> Unit,
    val onResume: (Download) -> Unit,
    val onVerify: (Download) -> Unit,
    val onMove: (Download) -> Unit,
    val onRemove: (Download) -> Unit,
)

private fun LazyListScope.section(
    title: String,
    downloads: ImmutableList<Download>,
    uiState: DownloadsUiState,
    actions: RowActions,
) {
    if (downloads.isEmpty()) return

    item(key = "header-$title") {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
        )
    }

    itemsIndexed(
        items = downloads,
        key = { _, download -> "${download.appId}/${download.workshopId}" },
    ) { index, download ->
        DownloadRow(
            download = download,
            progress = download.liveProgress(uiState.progress),
            updateAvailable = !download.isWorkshop && download.appId in uiState.updates,
            moveFraction = uiState.moving
                ?.takeIf { it.appId == download.appId && it.workshopId == download.workshopId }
                ?.fraction,
            index = index,
            count = downloads.size,
            onOpen = { actions.onOpen(download.appId) },
            onPause = { actions.onPause(download) },
            onResume = { actions.onResume(download) },
            onVerify = { actions.onVerify(download) },
            onMove = { actions.onMove(download) },
            onRemove = { actions.onRemove(download) },
        )
    }
}

@Composable
private fun DownloadRow(
    download: Download,
    progress: DownloadProgress?,
    updateAvailable: Boolean,
    moveFraction: Float?,
    index: Int,
    count: Int,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onVerify: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    val failed = download.state == DownloadState.FAILED
    val status = if (moveFraction != null) {
        "Moving · ${(moveFraction * 100).toInt()}%"
    } else {
        download.statusLine(progress)
    }

    // The row is redrawn with every progress update; decode the name once.
    val name = remember(download.name) { download.name.decodeHtml() }

    GroupedListItem(index = index, count = count, onClick = onOpen) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(
                    imageModel = { download.headerUrl },
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(HEADER_ASPECT)
                        .clip(MaterialTheme.shapes.medium),
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                    previewPlaceholder = painterResource(R.drawable.vapulla_background),
                    loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (download.isWorkshop) "Workshop item · $status" else status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = if (failed) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (updateAvailable) UpdateAvailablePill()
                }

                Row {
                    when (download.state) {
                        DownloadState.DOWNLOADING, DownloadState.QUEUED ->
                            IconButton(onClick = onPause) {
                                Icon(Icons.Outlined.Pause, contentDescription = "Pause")
                            }

                        DownloadState.PAUSED ->
                            IconButton(onClick = onResume) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = "Resume")
                            }

                        DownloadState.FAILED ->
                            IconButton(onClick = onResume) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Retry")
                            }

                        // Nothing else can be done to it while its files are on the move.
                        DownloadState.COMPLETED -> if (moveFraction == null) {
                            IconButton(onClick = onMove) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.DriveFileMove,
                                    contentDescription = "Move to another location",
                                )
                            }
                            IconButton(onClick = onVerify) {
                                Icon(Icons.Outlined.Verified, contentDescription = "Verify files")
                            }
                        }
                    }
                    if (moveFraction == null) {
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Outlined.Close, contentDescription = "Remove")
                        }
                    }
                }
            }

            if (download.state != DownloadState.COMPLETED || moveFraction != null) {
                LinearProgressIndicator(
                    progress = { moveFraction ?: download.fractionWith(progress) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }
}

/**
 * Picks the install location a finished [download] is moved to. Its files are copied there
 * and only then deleted where they were.
 */
@Composable
private fun MoveDialog(
    download: Download,
    locations: List<InstallLocation>,
    onConfirm: (toRoot: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(download.appId, download.workshopId) {
        mutableStateOf(locations.firstOrNull())
    }
    val fits = selected?.let { download.totalBytes <= it.freeBytes } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null) },
        title = { Text("Move ${download.name.decodeHtml()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (locations.isEmpty()) {
                    Text(
                        "There is nowhere else to move it to. Another storage, or a folder " +
                            "picked under Install location in Settings, would show up here.",
                    )
                }

                locations.forEach { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = location == selected,
                                role = Role.RadioButton,
                                onClick = { selected = location },
                            )
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row is the click target; the button only shows the state.
                        RadioButton(selected = location == selected, onClick = null)
                        Column {
                            Text(text = location.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${location.freeBytes.toFileSize()} free · ${location.path}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (selected != null && !fits) {
                    Text(
                        text = "Not enough space there: " +
                            "${download.totalBytes.toFileSize()} needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = fits, onClick = { selected?.let { onConfirm(it.path) } }) {
                Text("Move")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Confirms taking [download] off the list. Whether its files go too is the user's call:
 * offered by default for anything unfinished, since a partial download is of no use.
 */
@Composable
private fun RemoveDialog(
    download: Download,
    onConfirm: (deleteFiles: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteFiles by remember(download.appId, download.workshopId) {
        mutableStateOf(download.state != DownloadState.COMPLETED)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove download?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${download.name.decodeHtml()} is taken off this list.")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = deleteFiles,
                            role = Role.Checkbox,
                            onValueChange = { deleteFiles = it },
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The row is the click target; the box only shows the state.
                    Checkbox(checked = deleteFiles, onCheckedChange = null)
                    Text("Also delete its files from this device")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteFiles) }) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyState(modifier: Modifier, onOpenLibrary: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No downloads yet.\nOpen a game in your library to download it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onOpenLibrary) { Text("Open library") }
        }
    }
}

private fun previewDownload(appId: Int, name: String, state: DownloadState, done: Long) = Download(
    appId = appId,
    name = name,
    headerUrl = "",
    os = "windows",
    depotIds = listOf(appId + 1),
    installDir = "",
    totalBytes = 12_345_678_901,
    downloadedBytes = done,
    state = state,
    message = "Stalled: nothing arrived for 2 minutes. Retry to continue."
        .takeIf { state == DownloadState.FAILED },
    addedAt = appId.toLong(),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        DownloadsContent(
            uiState = DownloadsUiState(
                active = persistentListOf(
                    previewDownload(440, "Team Fortress 2", DownloadState.DOWNLOADING, 0),
                    previewDownload(730, "Counter-Strike 2", DownloadState.QUEUED, 0),
                ),
                paused = persistentListOf(
                    previewDownload(570, "Dota 2", DownloadState.PAUSED, 4_000_000_000),
                ),
                finished = persistentListOf(
                    previewDownload(620, "Portal 2", DownloadState.COMPLETED, 12_345_678_901),
                    previewDownload(400, "Portal", DownloadState.FAILED, 1_000_000_000),
                ),
                progress = DownloadProgress(
                    appId = 440,
                    workshopId = 0,
                    fraction = 0.42f,
                    downloadedBytes = 5_185_185_138,
                    totalBytes = 12_345_678_901,
                    bytesPerSecond = 3_400_000,
                    status = DownloadProgress.STATUS_DOWNLOADING,
                ),
                isLoading = false,
            ),
            installLocations = persistentListOf(),
            onMove = { _, _ -> },
            onNavDrawerAction = {},
            onOpenGame = {},
            onOpenLibrary = {},
            onPause = {},
            onResume = {},
            onVerify = {},
            onRemove = { _, _ -> },
            onClearFinished = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_Empty() {
    VapullaTheme {
        DownloadsContent(
            uiState = DownloadsUiState(isLoading = false),
            installLocations = persistentListOf(),
            onMove = { _, _ -> },
            onNavDrawerAction = {},
            onOpenGame = {},
            onOpenLibrary = {},
            onPause = {},
            onResume = {},
            onVerify = {},
            onRemove = { _, _ -> },
            onClearFinished = {},
        )
    }
}
