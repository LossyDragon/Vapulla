package `in`.dragonbra.vapulla.ui.screens.gamedetail.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.download.DownloadPlan
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.screens.downloads.components.fractionWith
import `in`.dragonbra.vapulla.ui.screens.downloads.components.statusLine
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.helpers.capitalize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * The download part of the game detail screen. Before anything was started it is a single
 * Download button; after that it shows where the download stands, the control that fits its
 * state, and the way to the Downloads screen.
 *
 * Shows nothing for an app that has no depots to download and no download on record.
 */
@Composable
fun DownloadSection(
    download: Download?,
    progress: DownloadProgress?,
    plans: ImmutableList<DownloadPlan>,
    freeBytes: Long,
    onDownload: (SteamApp.OS) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenDepots: () -> Unit = {},
) {
    if (download == null && plans.isEmpty()) return

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DownloadDialog(
            plans = plans,
            freeBytes = freeBytes,
            onConfirm = { os ->
                showDialog = false
                onDownload(os)
            },
            onDismiss = { showDialog = false },
        )
    }

    Section(title = "Download") {
        if (download == null) {
            Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Download, contentDescription = null)
                Text(text = "  Download · ${plans.first().installBytes.toFileSize()}")
            }
        } else {
            DownloadCard(
                download = download,
                progress = progress,
                canDownloadAgain = plans.isNotEmpty(),
                onDownloadAgain = { showDialog = true },
                onPause = onPause,
                onResume = onResume,
                onOpenDownloads = onOpenDownloads,
            )
        }

        // For the curious: what the download is made of.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onOpenDepots) {
                Text(text = "Depots and branches")
            }
        }
    }
}

@Composable
private fun DownloadCard(
    download: Download,
    progress: DownloadProgress?,
    canDownloadAgain: Boolean,
    onDownloadAgain: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    val failed = download.state == DownloadState.FAILED

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = download.statusLine(progress),
                style = MaterialTheme.typography.bodyMedium,
                color = if (failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            if (download.state != DownloadState.COMPLETED) {
                LinearProgressIndicator(
                    progress = { download.fractionWith(progress) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            Text(
                text = listOfNotNull(
                    download.os.capitalize().takeIf { download.os != SteamApp.OS.none.name },
                    "${download.depotIds.size} depot(s)",
                    download.installDir,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (download.state) {
                    DownloadState.DOWNLOADING, DownloadState.QUEUED ->
                        FilledTonalButton(onClick = onPause, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Pause, contentDescription = null)
                            Text(text = "  Pause")
                        }

                    DownloadState.PAUSED ->
                        FilledTonalButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                            Text(text = "  Resume")
                        }

                    DownloadState.FAILED ->
                        FilledTonalButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text(text = "  Retry")
                        }

                    // Picks up an update: files that are still current are not fetched again.
                    DownloadState.COMPLETED -> if (canDownloadAgain) {
                        FilledTonalButton(
                            onClick = onDownloadAgain,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Text(text = "  Update")
                        }
                    }
                }

                OutlinedButton(onClick = onOpenDownloads, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Download, contentDescription = null)
                    Text(text = "  Downloads")
                }
            }
        }
    }
}

/** Picks the platform to download, with what each takes and what the device has free. */
@Composable
private fun DownloadDialog(
    plans: ImmutableList<DownloadPlan>,
    freeBytes: Long,
    onConfirm: (SteamApp.OS) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(plans) { mutableStateOf(plans.first()) }
    val fits = selected.installBytes <= freeBytes

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
        title = { Text("Download") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plans.forEach { plan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = plan == selected,
                                role = Role.RadioButton,
                                onClick = { selected = plan },
                            )
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row is the click target; the button only shows the state.
                        RadioButton(selected = plan == selected, onClick = null)
                        Column {
                            Text(
                                text = if (plan.os == SteamApp.OS.none) {
                                    "All platforms"
                                } else {
                                    plan.os.name.capitalize()
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "${plan.installBytes.toFileSize()} on disk · " +
                                    "${plan.downloadBytes.toFileSize()} to download · " +
                                    "${plan.depotIds.size} depot(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = if (fits) {
                        "${freeBytes.toFileSize()} available on this device."
                    } else {
                        "Not enough space: only ${freeBytes.toFileSize()} available on this device."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fits) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = fits, onClick = { onConfirm(selected.os) }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private val previewPlans = persistentListOf(
    DownloadPlan(SteamApp.OS.windows, listOf(441, 442), 12_345_678_901, 9_000_000_000),
    DownloadPlan(SteamApp.OS.linux, listOf(441, 443), 12_000_000_000, 8_800_000_000),
)

private val previewDownload = Download(
    appId = 440,
    name = "Team Fortress 2",
    headerUrl = "",
    os = "windows",
    depotIds = listOf(441, 442),
    installDir = "/storage/emulated/0/Android/data/in.dragonbra.vapulla/files/steamapps/common/tf2",
    totalBytes = 12_345_678_901,
    downloadedBytes = 4_000_000_000,
    state = DownloadState.PAUSED,
    addedAt = 0,
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DownloadSection(
                    download = null,
                    progress = null,
                    plans = previewPlans,
                    freeBytes = 64_000_000_000,
                    onDownload = {},
                    onPause = {},
                    onResume = {},
                    onOpenDownloads = {},
                )
                DownloadSection(
                    download = previewDownload,
                    progress = null,
                    plans = previewPlans,
                    freeBytes = 64_000_000_000,
                    onDownload = {},
                    onPause = {},
                    onResume = {},
                    onOpenDownloads = {},
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_Dialog() {
    VapullaTheme {
        DownloadDialog(
            plans = previewPlans,
            freeBytes = 10_000_000_000,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
