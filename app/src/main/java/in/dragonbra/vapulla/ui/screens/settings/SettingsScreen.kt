package `in`.dragonbra.vapulla.ui.screens.settings

import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.service.download.InstallLocation
import `in`.dragonbra.vapulla.ui.composables.IconLabel
import `in`.dragonbra.vapulla.ui.composables.MenuButton
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.StorageFolders
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavDrawerAction: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val installLocations by viewModel.installLocations.collectAsStateWithLifecycle()

    SettingsContent(
        settings = settings,
        installLocations = installLocations,
        onNavDrawerAction = onNavDrawerAction,
        onChange = viewModel::update,
        onReset = viewModel::resetToDefaults,
    )
}

/**
 * @param onChange changes settings: it is handed a function from the stored settings to the
 *   new ones, usually a `copy` with one value replaced.
 */
@Composable
private fun SettingsContent(
    settings: AppSettings,
    installLocations: ImmutableList<InstallLocation>,
    onNavDrawerAction: () -> Unit,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { MenuButton(onClick = onNavDrawerAction) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
            contentPadding = listContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SettingsGroup(
                    "Appearance",
                    { shape ->
                        SwitchRow(
                            title = "Dark theme",
                            subtitle = "Use the dark grey palette",
                            checked = settings.darkTheme,
                            shape = shape,
                            onCheckedChange = { on -> onChange { it.copy(darkTheme = on) } },
                        )
                    },
                    { shape ->
                        SwitchRow(
                            title = "Dynamic colour",
                            subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Follow the wallpaper palette"
                            } else {
                                "Needs Android 12 or newer"
                            },
                            checked = settings.dynamicColor,
                            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                            shape = shape,
                            onCheckedChange = { on -> onChange { it.copy(dynamicColor = on) } },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Server browser",
                    { shape ->
                        SliderRow(
                            title = "Servers per query",
                            subtitle = "How many servers to ask the master server for",
                            value = settings.maxServers,
                            range = AppSettings.MAX_SERVERS_RANGE,
                            step = AppSettings.MAX_SERVERS_STEP,
                            label = { "$it" },
                            shape = shape,
                            onValueChange = { count -> onChange { it.copy(maxServers = count) } },
                        )
                    },
                    { shape ->
                        SliderRow(
                            title = "Server reply timeout",
                            subtitle = "Wait this long for each server's A2S_INFO reply",
                            value = settings.serverTimeoutMs,
                            range = AppSettings.SERVER_TIMEOUT_RANGE,
                            step = AppSettings.SERVER_TIMEOUT_STEP,
                            label = { "$it ms" },
                            shape = shape,
                            onValueChange = { ms -> onChange { it.copy(serverTimeoutMs = ms) } },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Friends",
                    { shape ->
                        SwitchRow(
                            title = "Append nicknames",
                            subtitle = if (settings.appendNicknames) {
                                "Shown as Name (Nickname)"
                            } else {
                                "Shown as Nickname*"
                            },
                            checked = settings.appendNicknames,
                            shape = shape,
                            onCheckedChange = { on -> onChange { it.copy(appendNicknames = on) } },
                        )
                    },
                    { shape ->
                        ChoiceRow(
                            title = "Recent chats: days to look back",
                            options = AppSettings.RECENT_CHATS_DAYS,
                            selected = settings.recentChatsDays,
                            shape = shape,
                            onSelect = { days -> onChange { it.copy(recentChatsDays = days) } },
                        )
                    },
                    { shape ->
                        SwitchRow(
                            title = "Show blocked friends",
                            subtitle = if (settings.showBlockedFriends) {
                                "Listed in a Blocked section at the bottom"
                            } else {
                                "Hidden from the friends list"
                            },
                            checked = settings.showBlockedFriends,
                            shape = shape,
                            onCheckedChange = { on ->
                                onChange { it.copy(showBlockedFriends = on) }
                            },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Chat",
                    { shape ->
                        SwitchRow(
                            title = "Show raw BBCode",
                            subtitle = if (settings.rawBBCode) {
                                "Messages are shown as Steam sent them, tags and all"
                            } else {
                                "Messages are shown formatted"
                            },
                            checked = settings.rawBBCode,
                            shape = shape,
                            onCheckedChange = { on -> onChange { it.copy(rawBBCode = on) } },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Steam web pages",
                    { shape ->
                        SwitchRow(
                            title = "Open signed in, inside the app",
                            subtitle = if (settings.signedInWebPages) {
                                "Community and store pages open here, signed in to your account"
                            } else {
                                "Community and store pages open in your browser"
                            },
                            checked = settings.signedInWebPages,
                            shape = shape,
                            onCheckedChange = { on ->
                                onChange { it.copy(signedInWebPages = on) }
                            },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Workshop",
                    { shape ->
                        ChoiceRow(
                            title = "Items per page",
                            options = AppSettings.WORKSHOP_PAGE_SIZES,
                            selected = settings.workshopPageSize,
                            shape = shape,
                            onSelect = { size -> onChange { it.copy(workshopPageSize = size) } },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Downloads",
                    { shape ->
                        SwitchRow(
                            title = "Wi-Fi only",
                            subtitle = if (settings.downloadWifiOnly) {
                                "Downloads wait while on mobile data or another metered network"
                            } else {
                                "Downloads use whatever network there is"
                            },
                            checked = settings.downloadWifiOnly,
                            shape = shape,
                            onCheckedChange = { on ->
                                onChange { it.copy(downloadWifiOnly = on) }
                            },
                        )
                    },
                    { shape ->
                        ChoiceRow(
                            title = "Speed limit, in MB per second",
                            options = AppSettings.DOWNLOAD_SPEED_LIMITS,
                            selected = settings.downloadSpeedLimit,
                            shape = shape,
                            label = { if (it == 0) "Off" else "$it" },
                            onSelect = { limit ->
                                onChange { it.copy(downloadSpeedLimit = limit) }
                            },
                        )
                    },
                    { shape ->
                        ChoiceRow(
                            title = "Connections: chunks downloaded at once",
                            options = AppSettings.DOWNLOAD_CONNECTIONS,
                            selected = settings.downloadConnections,
                            shape = shape,
                            onSelect = { count ->
                                onChange { it.copy(downloadConnections = count) }
                            },
                        )
                    },
                    { shape ->
                        ChoiceRow(
                            title = "File writes: chunks written to disk at once",
                            options = AppSettings.DOWNLOAD_FILE_WRITES,
                            selected = settings.downloadFileWrites,
                            shape = shape,
                            onSelect = { count ->
                                onChange { it.copy(downloadFileWrites = count) }
                            },
                        )
                    },
                    { shape ->
                        ChoiceRow(
                            title = "Decompress threads: chunks unpacked at once",
                            options = AppSettings.DOWNLOAD_DECOMPRESS_THREADS,
                            selected = settings.downloadDecompressThreads,
                            shape = shape,
                            label = { if (it == 0) "Auto" else "$it" },
                            onSelect = { count ->
                                onChange { it.copy(downloadDecompressThreads = count) }
                            },
                        )
                    },
                    { shape ->
                        LocationRow(
                            locations = installLocations,
                            selected = settings.downloadLocation,
                            shape = shape,
                            onSelect = { path -> onChange { it.copy(downloadLocation = path) } },
                        )
                    },
                )
            }

            item {
                SettingsGroup(
                    "Network log",
                    { shape ->
                        ChoiceRow(
                            title = "Packets to keep",
                            options = AppSettings.NETWORK_LOG_SIZES,
                            selected = settings.networkLogSize,
                            shape = shape,
                            onSelect = { size -> onChange { it.copy(networkLogSize = size) } },
                        )
                    },
                    { shape ->
                        SwitchRow(
                            title = "Keep packet contents",
                            subtitle = "Lets you open a packet to see what was in it. Contents " +
                                "stay in memory until the log is cleared; sign-in messages are " +
                                "never kept.",
                            checked = settings.networkLogBodies,
                            shape = shape,
                            onCheckedChange = { on ->
                                onChange { it.copy(networkLogBodies = on) }
                            },
                        )
                    },
                )
            }

            item {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    IconLabel(icon = Icons.Outlined.RestartAlt, text = "Reset to defaults")
                }
            }
        }
    }
}

/**
 * A titled group of settings. Each of [rows] is handed the shape for its place in the group,
 * so a row can be added or moved without renumbering the others.
 */
@Composable
private fun SettingsGroup(title: String, vararg rows: @Composable (Shape) -> Unit) {
    Section(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            rows.forEachIndexed { index, row -> row(groupedListShape(index, rows.size)) }
        }
    }
}

/** The shared container for one setting: title, optional subtitle, and a control below or beside. */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    shape: Shape,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                trailing?.invoke()
            }
            below?.invoke()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    shape: Shape,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    SettingRow(
        title = title,
        subtitle = subtitle,
        shape = shape,
        trailing = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
    )
}

/** A slider that snaps to [step] and only persists when the drag ends. */
@Composable
private fun SliderRow(
    title: String,
    subtitle: String?,
    value: Int,
    range: IntRange,
    step: Int,
    label: (Int) -> String,
    shape: Shape,
    onValueChange: (Int) -> Unit,
) {
    // Keyed on value so the thumb follows changes made outside the drag.
    val sliderState = remember(value) {
        SliderState(
            value = value.toFloat(),
            steps = (range.last - range.first) / step - 1,
            trackRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
    val snapped = (sliderState.value / step).toInt() * step

    SettingRow(
        title = title,
        subtitle = subtitle,
        shape = shape,
        trailing = {
            Text(
                text = label(snapped),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        below = {
            Slider(
                state = sliderState,
                onValueChange = { sliderState.value = it },
                onValueChangeFinished = { onValueChange(snapped) },
            )
        },
    )
}

/** A connected button group for a short list of fixed choices. */
@Composable
private fun ChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    shape: Shape,
    onSelect: (Int) -> Unit,
    label: (Int) -> String = { "$it" },
) {
    SettingRow(
        title = title,
        subtitle = null,
        shape = shape,
        below = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween,
                ),
            ) {
                options.forEachIndexed { index, option ->
                    ToggleButton(
                        checked = option == selected,
                        onCheckedChange = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(text = label(option))
                    }
                }
            }
        },
    )
}

/**
 * Where new downloads are installed: one of the storage volumes, each with the space it has
 * left. An empty [selected] stands for the first, the device's own storage.
 */
@Composable
private fun LocationRow(
    locations: ImmutableList<InstallLocation>,
    selected: String,
    shape: Shape,
    onSelect: (String) -> Unit,
) {
    val current = locations.find { it.path == selected } ?: locations.firstOrNull()

    SettingRow(
        title = "Install location",
        subtitle = "New downloads go to the app's own folder on the storage picked here, or " +
            "to a folder of your own. Downloads already made stay where they are; the " +
            "Downloads screen can move them.",
        shape = shape,
        below = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                locations.forEach { location ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .selectable(
                                selected = location == current,
                                role = Role.RadioButton,
                                onClick = { onSelect(location.path) },
                            )
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row is the click target; the button only shows the state.
                        RadioButton(selected = location == current, onClick = null)
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

                if (StorageFolders.isSupported) FolderPicker(onSelect = onSelect)
            }
        },
    )
}

/**
 * Picks any folder as the install location. That takes access to all files, which Android
 * has the user give on a settings screen of its own, so the first time round this explains
 * why, sends the user there, and opens the folder picker once they are back with it given.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderPicker(onSelect: (String) -> Unit) {
    val context = LocalContext.current
    var explaining by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { tree ->
        if (tree != null) {
            val path = StorageFolders.pathOf(tree)
            if (path == null) {
                problem = "That folder is not on this device's storage or a card."
            } else {
                problem = null
                onSelect(path)
            }
        }
    }

    val accessSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (StorageFolders.hasAccess()) {
            folderPicker.launch(null)
        } else {
            problem = "Without access to all files a folder of your own cannot be used."
        }
    }

    if (explaining) {
        AlertDialog(
            onDismissRequest = { explaining = false },
            title = { Text("Access to all files") },
            text = {
                Text(
                    "Android only lets an app write to a folder of your choice when it has " +
                        "access to all files. The next screen is where you give it to " +
                        "Vapulla. It is used for nothing but the folder you pick.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        explaining = false
                        accessSettings.launch(StorageFolders.accessSettingsIntent(context))
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { explaining = false }) { Text("Cancel") }
            },
        )
    }

    OutlinedButton(
        onClick = {
            problem = null
            if (StorageFolders.hasAccess()) folderPicker.launch(null) else explaining = true
        },
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        IconLabel(icon = Icons.Outlined.FolderOpen, text = "Choose a folder")
    }

    problem?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        SettingsContent(
            settings = AppSettings(),
            installLocations = persistentListOf(
                InstallLocation("/storage/emulated/0/Android/data", "Device", 64_000_000_000),
                InstallLocation("/storage/1A2B-3C4D/Android/data", "SD card", 8_000_000_000),
            ),
            onNavDrawerAction = {},
            onChange = {},
            onReset = {},
        )
    }
}
