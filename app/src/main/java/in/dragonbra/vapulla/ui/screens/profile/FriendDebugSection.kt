package `in`.dragonbra.vapulla.ui.screens.profile

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import java.util.Date
import java.util.EnumSet

/**
 * The friend as Steam last described them, unprocessed: what `PersonaStateCallback` carried,
 * including the flags and rich presence keys the rest of the app only reads in part. Folded
 * away until asked for, since it is of no use for talking to someone.
 */
@Composable
internal fun FriendDebugSection(friend: SteamFriend) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable(
                    onClickLabel = if (expanded) "Hide raw data" else "Show raw data",
                    onClick = { expanded = !expanded },
                )
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Under the hood",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RawList(title = "Persona state", rows = personaRows(friend))
                RawList(
                    title = "Rich presence",
                    rows = friend.richPresence.toSortedMap().toList(),
                    empty = "The game, if any, publishes no rich presence.",
                )
            }
        }
    }
}

private fun personaRows(friend: SteamFriend): List<Pair<String, String>> = listOf(
    "Steam ID" to friend.id.toULong().toString(),
    "Relationship" to friend.relation.name,
    "State" to "${friend.state.name} (${friend.state.code()})",
    // What the friend's client is: mobile, VR, Big Picture, and so on.
    "State flags" to friend.stateFlags.joinToString { it.name }.ifEmpty { "None" },
    // Which groups of fields Steam included in its last update about this friend.
    "Fields in last update" to friend.statusFlags.joinToString { it.name }.ifEmpty { "None" },
    "Game ID" to friend.gameID.toUInt64().toULong().toString(),
    "Game ID type" to (friend.gameID.appType?.name ?: "Unknown"),
    "App ID" to friend.gameAppID.toString(),
    "Game name" to friend.gameName.ifEmpty { "None" },
    "Game data blob" to "${friend.gameDataBlob.size} bytes",
    "Last log on" to friend.lastLogOn.orNever(),
    "Last log off" to friend.lastLogOff.orNever(),
)

private fun Date.orNever(): String = if (time > 0) toString() else "Not sent"

/** Label above value, in full and selectable: raw values are for reading and copying. */
@Composable
private fun RawList(title: String, rows: List<Pair<String, String>>, empty: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )

        if (rows.isEmpty()) {
            Text(
                text = empty,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        rows.forEachIndexed { index, (label, value) ->
            Surface(
                shape = groupedListShape(index, rows.size),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
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
            Column(modifier = Modifier.padding(16.dp)) {
                RawList(
                    title = "Persona state",
                    rows = personaRows(
                        SteamFriend(
                            id = 76561197960287930,
                            name = "Vapulla",
                            state = EPersonaState.Online,
                            gameAppID = 440,
                            gameName = "Team Fortress 2",
                            stateFlags = EnumSet.of(EPersonaStateFlag.ClientTypeMobile),
                        ),
                    ),
                )
            }
        }
    }
}
