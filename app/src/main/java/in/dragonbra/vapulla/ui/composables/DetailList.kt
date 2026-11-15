package `in`.dragonbra.vapulla.ui.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** A titled block of a detail screen. */
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        content()
    }
}

/** A line of small grey text that explains what is shown above or below it. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/**
 * Key/value rows in one grouped container. Values that are empty are skipped. A value that
 * is a web address is shown as a link on a line of its own, under its label, and tapping
 * the row opens it.
 */
@Composable
fun DetailList(rows: List<Pair<String, String>>) {
    val uriHandler = LocalUriHandler.current
    val visible = rows.filter { it.second.isNotBlank() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        visible.forEachIndexed { index, (label, value) ->
            val isLink = value.startsWith("https://") || value.startsWith("http://")
            val open: (() -> Unit)? = if (isLink) ({ uriHandler.openUri(value) }) else null

            GroupedListItem(index = index, count = visible.size, onClick = open) {
                if (isLink) {
                    // An address is too long to share a line with its label.
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Label(label)
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Label(label)
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            Section(title = "Details") {
                DetailList(
                    listOf(
                        "Developer" to "Valve",
                        "Publisher" to "",
                        "App ID" to "440",
                        "Store page" to "https://store.steampowered.com/app/440",
                    ),
                )
            }
        }
    }
}
