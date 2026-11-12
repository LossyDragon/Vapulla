package `in`.dragonbra.vapulla.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.manager.AppSettings
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavDrawerAction: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    SettingsContent(
        settings = settings,
        onNavDrawerAction = onNavDrawerAction,
        onDarkTheme = viewModel::setDarkTheme,
        onDynamicColor = viewModel::setDynamicColor,
        onMaxServers = viewModel::setMaxServers,
        onServerTimeout = viewModel::setServerTimeoutMs,
        onWorkshopPageSize = viewModel::setWorkshopPageSize,
        onNetworkLogSize = viewModel::setNetworkLogSize,
        onReset = viewModel::resetToDefaults,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsContent(
    settings: AppSettings,
    onNavDrawerAction: () -> Unit,
    onDarkTheme: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onMaxServers: (Int) -> Unit,
    onServerTimeout: (Int) -> Unit,
    onWorkshopPageSize: (Int) -> Unit,
    onNetworkLogSize: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
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
                Section(title = "Appearance") {
                    SwitchRow(
                        title = "Dark theme",
                        subtitle = "Use the dark grey palette",
                        checked = settings.darkTheme,
                        shape = groupedListShape(0, 2),
                        onCheckedChange = onDarkTheme,
                    )
                    SwitchRow(
                        title = "Dynamic colour",
                        subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            "Follow the wallpaper palette"
                        } else {
                            "Needs Android 12 or newer"
                        },
                        checked = settings.dynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                        shape = groupedListShape(1, 2),
                        onCheckedChange = onDynamicColor,
                    )
                }
            }

            item {
                Section(title = "Server browser") {
                    SliderRow(
                        title = "Servers per query",
                        subtitle = "How many servers to ask the master server for",
                        value = settings.maxServers,
                        range = AppSettings.MAX_SERVERS_RANGE,
                        step = AppSettings.MAX_SERVERS_STEP,
                        label = { "$it" },
                        shape = groupedListShape(0, 2),
                        onValueChange = onMaxServers,
                    )
                    SliderRow(
                        title = "Server reply timeout",
                        subtitle = "Wait this long for each server's A2S_INFO reply",
                        value = settings.serverTimeoutMs,
                        range = AppSettings.SERVER_TIMEOUT_RANGE,
                        step = AppSettings.SERVER_TIMEOUT_STEP,
                        label = { "$it ms" },
                        shape = groupedListShape(1, 2),
                        onValueChange = onServerTimeout,
                    )
                }
            }

            item {
                Section(title = "Workshop") {
                    ChoiceRow(
                        title = "Items per page",
                        options = AppSettings.WORKSHOP_PAGE_SIZES,
                        selected = settings.workshopPageSize,
                        shape = groupedListShape(0, 1),
                        onSelect = onWorkshopPageSize,
                    )
                }
            }

            item {
                Section(title = "Network log") {
                    ChoiceRow(
                        title = "Packets to keep",
                        options = AppSettings.NETWORK_LOG_SIZES,
                        selected = settings.networkLogSize,
                        shape = groupedListShape(0, 1),
                        onSelect = onNetworkLogSize,
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null)
                    Text(text = "  Reset to defaults")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        content()
    }
}

/** The shared container for one setting: title, optional subtitle, and a control below or beside. */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    shape: androidx.compose.ui.graphics.Shape,
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
    shape: androidx.compose.ui.graphics.Shape,
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
    shape: androidx.compose.ui.graphics.Shape,
    onValueChange: (Int) -> Unit,
) {
    var dragValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val snapped = (dragValue / step).toInt() * step

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
                value = dragValue,
                onValueChange = { dragValue = it },
                onValueChangeFinished = { onValueChange(snapped) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first) / step - 1,
            )
        },
    )
}

/** A connected button group for a short list of fixed choices. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    shape: androidx.compose.ui.graphics.Shape,
    onSelect: (Int) -> Unit,
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
                        Text(text = "$option")
                    }
                }
            }
        },
    )
}
