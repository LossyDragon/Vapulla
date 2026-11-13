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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Shape
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
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.ShimmerBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.downloads.components.fractionWith
import `in`.dragonbra.vapulla.ui.screens.downloads.components.liveProgress
import `in`.dragonbra.vapulla.ui.screens.downloads.components.statusLine
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
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

    DownloadsContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onOpenGame = onOpenGame,
        onOpenLibrary = onOpenLibrary,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onRemove = viewModel::remove,
        onClearFinished = viewModel::clearFinished,
    )
}

@Composable
private fun DownloadsContent(
    uiState: DownloadsUiState,
    onNavDrawerAction: () -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRemove: (appId: Int, deleteFiles: Boolean) -> Unit,
    onClearFinished: () -> Unit,
) {
    var pendingRemove by remember { mutableStateOf<Download?>(null) }

    pendingRemove?.let { download ->
        RemoveDialog(
            download = download,
            onConfirm = { deleteFiles ->
                onRemove(download.appId, deleteFiles)
                pendingRemove = null
            },
            onDismiss = { pendingRemove = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
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
                    onRemove = { pendingRemove = it },
                )

                section("Active", uiState.active, uiState.progress, rowActions)
                section("Paused", uiState.paused, uiState.progress, rowActions)
                section("Finished", uiState.finished, uiState.progress, rowActions)
            }
        }
    }
}

private class RowActions(
    val onOpen: (Int) -> Unit,
    val onPause: (Int) -> Unit,
    val onResume: (Int) -> Unit,
    val onRemove: (Download) -> Unit,
)

private fun LazyListScope.section(
    title: String,
    downloads: ImmutableList<Download>,
    progress: DownloadProgress?,
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

    itemsIndexed(downloads, key = { _, download -> download.appId }) { index, download ->
        DownloadRow(
            download = download,
            progress = download.liveProgress(progress),
            shape = groupedListShape(index, downloads.size),
            onOpen = { actions.onOpen(download.appId) },
            onPause = { actions.onPause(download.appId) },
            onResume = { actions.onResume(download.appId) },
            onRemove = { actions.onRemove(download) },
        )
    }
}

@Composable
private fun DownloadRow(
    download: Download,
    progress: DownloadProgress?,
    shape: Shape,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRemove: () -> Unit,
) {
    val failed = download.state == DownloadState.FAILED

    Surface(
        onClick = onOpen,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
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
                        .aspectRatio(460f / 215f)
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
                        text = download.name.decodeHtml(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = download.statusLine(progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = if (failed) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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

                        DownloadState.COMPLETED -> Unit
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove")
                    }
                }
            }

            if (download.state != DownloadState.COMPLETED) {
                LinearProgressIndicator(
                    progress = { download.fractionWith(progress) },
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
 * Confirms taking [download] off the list. Whether its files go too is the user's call:
 * offered by default for anything unfinished, since a partial download is of no use.
 */
@Composable
private fun RemoveDialog(
    download: Download,
    onConfirm: (deleteFiles: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var deleteFiles by remember(download.appId) {
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
                text = "No downloads yet. Open a game in your library to download it.",
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
                    fraction = 0.42f,
                    downloadedBytes = 5_185_185_138,
                    totalBytes = 12_345_678_901,
                    bytesPerSecond = 3_400_000,
                    status = "Downloading",
                ),
                isLoading = false,
            ),
            onNavDrawerAction = {},
            onOpenGame = {},
            onOpenLibrary = {},
            onPause = {},
            onResume = {},
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
            onNavDrawerAction = {},
            onOpenGame = {},
            onOpenLibrary = {},
            onPause = {},
            onResume = {},
            onRemove = { _, _ -> },
            onClearFinished = {},
        )
    }
}
