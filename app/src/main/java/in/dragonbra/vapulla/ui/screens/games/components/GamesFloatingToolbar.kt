package `in`.dragonbra.vapulla.ui.screens.games.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

/** The types worth a one-tap toggle. Everything else lives in the filter sheet. */
private val quickTypes = listOf(
    AppType.game to "Games",
    AppType.dlc to "DLC",
    AppType.demo to "Demos",
    AppType.tool to "Tools",
)

/**
 * Material 3 Expressive floating toolbar for the games list: a connected button group of
 * quick type toggles plus a FAB that opens the full filter sheet. In a connected group the
 * selected segment morphs into the full pill, the same convention as the chat screen's emoji
 * selector. An empty [appTypes] means the default "Games" filter.
 */
@Composable
fun GamesFloatingToolbar(
    appTypes: ImmutableSet<AppType>,
    onAppTypeClicked: (AppType) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onFilterClick) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = "All filters")
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            quickTypes.forEachIndexed { index, (type, label) ->
                val checked = if (appTypes.isEmpty()) type == AppType.game else type in appTypes
                ToggleButton(
                    checked = checked,
                    onCheckedChange = { onAppTypeClicked(type) },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        quickTypes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            GamesFloatingToolbar(
                appTypes = persistentSetOf(AppType.game, AppType.demo),
                onAppTypeClicked = {},
                onFilterClick = {},
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
