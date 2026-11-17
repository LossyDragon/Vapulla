package `in`.dragonbra.vapulla.ui.screens.gamedetail.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.service.download.DownloadPlan
import `in`.dragonbra.vapulla.service.download.DownloadPlanner
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.ui.composables.IconLabel
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.screens.downloads.components.UpdateAvailablePill
import `in`.dragonbra.vapulla.ui.screens.downloads.components.WaitingForWifi
import `in`.dragonbra.vapulla.ui.screens.downloads.components.fractionWith
import `in`.dragonbra.vapulla.ui.screens.downloads.components.statusLine
import `in`.dragonbra.vapulla.ui.screens.gamedetail.DownloadChoices
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.helpers.capitalize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
    choices: DownloadChoices,
    freeBytes: Long,
    onDownload: (SteamApp.OS) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenDownloads: () -> Unit,
    onVerify: () -> Unit = {},
    onSelectBranch: (String) -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onOpenDepots: () -> Unit = {},
    updateAvailable: Boolean = false,
    waitingForWifi: Boolean = false,
    onUnlockBranch: (branch: String, password: String) -> Unit = { _, _ -> },
) {
    val plans = choices.plans
    // Another branch or language can leave nothing to download; the dialog says so.
    val canDownload = plans.isNotEmpty() ||
        choices.branches.size > 1 ||
        choices.languages.size > 1 ||
        choices.lockedBranches.isNotEmpty()

    if (download == null && !canDownload) return

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DownloadDialog(
            choices = choices,
            freeBytes = freeBytes,
            onSelectBranch = onSelectBranch,
            onUnlockBranch = onUnlockBranch,
            onSelectLanguage = onSelectLanguage,
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
                val size = plans.firstOrNull()?.installBytes?.toFileSize()
                IconLabel(
                    icon = Icons.Outlined.Download,
                    text = listOfNotNull("Download", size).joinToString(" · "),
                )
            }
        } else {
            DownloadCard(
                download = download,
                progress = progress,
                updateAvailable = updateAvailable,
                canDownloadAgain = canDownload,
                onDownloadAgain = { showDialog = true },
                onPause = onPause,
                onResume = onResume,
                onOpenDownloads = onOpenDownloads,
            )

            // Only for a download that is waiting or running: the others are not held up.
            val held = download.state == DownloadState.QUEUED ||
                download.state == DownloadState.DOWNLOADING
            if (waitingForWifi && held) WaitingForWifi()
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            if (download?.state == DownloadState.COMPLETED) {
                TextButton(onClick = onVerify) {
                    Text(text = "Verify files")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // For the curious: what the download is made of.
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
    updateAvailable: Boolean,
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

            // Steam's catalogue has moved on from what was downloaded; Update fetches it.
            if (updateAvailable) UpdateAvailablePill()

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
                    download.branch.takeIf { it != DownloadPlanner.BRANCH },
                    download.language.takeIf { it != DownloadPlanner.LANGUAGE }?.capitalize(),
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
                            IconLabel(Icons.Outlined.Pause, "Pause")
                        }

                    DownloadState.PAUSED ->
                        FilledTonalButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                            IconLabel(Icons.Outlined.PlayArrow, "Resume")
                        }

                    DownloadState.FAILED ->
                        FilledTonalButton(onClick = onResume, modifier = Modifier.weight(1f)) {
                            IconLabel(Icons.Outlined.Refresh, "Retry")
                        }

                    // Picks up an update: files that are still current are not fetched again.
                    DownloadState.COMPLETED -> if (canDownloadAgain) {
                        FilledTonalButton(
                            onClick = onDownloadAgain,
                            modifier = Modifier.weight(1f),
                        ) {
                            IconLabel(Icons.Outlined.Refresh, "Update")
                        }
                    }
                }

                OutlinedButton(onClick = onOpenDownloads, modifier = Modifier.weight(1f)) {
                    IconLabel(Icons.Outlined.Download, "Downloads")
                }
            }
        }
    }
}

/**
 * Picks what to download: the branch and language where the game has a choice of them, and
 * the platform, with what each takes and what the device has free.
 */
@Composable
private fun DownloadDialog(
    choices: DownloadChoices,
    freeBytes: Long,
    onSelectBranch: (String) -> Unit,
    onUnlockBranch: (branch: String, password: String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onConfirm: (SteamApp.OS) -> Unit,
    onDismiss: () -> Unit,
) {
    val plans = choices.plans

    // The locked branch whose password is being asked for.
    var unlocking by remember { mutableStateOf<String?>(null) }

    unlocking?.let { branch ->
        BranchPasswordDialog(
            branch = branch,
            onConfirm = { password ->
                onUnlockBranch(branch, password)
                unlocking = null
            },
            onDismiss = { unlocking = null },
        )
    }

    // The platform is kept, not the plan: the plans are made anew for every branch and language.
    var selectedOs by remember { mutableStateOf(plans.firstOrNull()?.os) }
    val selected = plans.find { it.os == selectedOs } ?: plans.firstOrNull()
    val fits = selected != null && selected.installBytes <= freeBytes

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
        title = { Text("Download") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Locked branches are offered too; picking one asks for its password first.
                OptionPicker(
                    label = "Branch",
                    options = (choices.branches + choices.lockedBranches).toImmutableList(),
                    selected = choices.branch,
                    display = { if (it in choices.lockedBranches) "$it (password)" else it },
                    onSelect = { branch ->
                        if (branch in choices.lockedBranches) {
                            unlocking = branch
                        } else {
                            onSelectBranch(branch)
                        }
                    },
                )
                OptionPicker(
                    label = "Language",
                    options = choices.languages,
                    selected = choices.language,
                    display = { it.capitalize() },
                    onSelect = onSelectLanguage,
                )

                plans.forEach { plan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = plan == selected,
                                role = Role.RadioButton,
                                onClick = { selectedOs = plan.os },
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
                    text = when {
                        selected == null -> "Nothing to download for this branch and language."

                        fits -> "${freeBytes.toFileSize()} available on this device."

                        else -> "Not enough space: only ${freeBytes.toFileSize()} available " +
                            "on this device."
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
            TextButton(enabled = fits, onClick = { selected?.let { onConfirm(it.os) } }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Asks for the password of a [branch] that is behind one. Steam says whether it is right;
 * until then the branch's size and depots are not known.
 */
@Composable
private fun BranchPasswordDialog(
    branch: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Password for $branch") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This branch is private. The game's developer hands out its password.")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Branch password") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = password.isNotBlank(), onClick = { onConfirm(password.trim()) }) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * One setting of the download with a menu to change it. Draws nothing when there is only
 * one option, which is the case for most games.
 */
@Composable
private fun OptionPicker(
    label: String,
    options: ImmutableList<String>,
    selected: String,
    onSelect: (String) -> Unit,
    display: (String) -> String = { it },
) {
    if (options.size < 2) return

    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(text = display(selected))
                Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(display(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

private val previewChoices = DownloadChoices(
    plans = persistentListOf(
        DownloadPlan(SteamApp.OS.windows, listOf(441, 442), 12_345_678_901, 9_000_000_000),
        DownloadPlan(SteamApp.OS.linux, listOf(441, 443), 12_000_000_000, 8_800_000_000),
    ),
    branches = persistentListOf("public", "prerelease"),
    lockedBranches = persistentListOf("internal"),
    languages = persistentListOf("english", "french", "german"),
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
                    choices = previewChoices,
                    freeBytes = 64_000_000_000,
                    onDownload = {},
                    onPause = {},
                    onResume = {},
                    onOpenDownloads = {},
                )
                DownloadSection(
                    download = previewDownload,
                    progress = null,
                    choices = previewChoices,
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
        Box(Modifier.fillMaxSize()) {
            DownloadDialog(
                choices = previewChoices,
                freeBytes = 10_000_000_000,
                onSelectBranch = {},
                onUnlockBranch = { _, _ -> },
                onSelectLanguage = {},
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
