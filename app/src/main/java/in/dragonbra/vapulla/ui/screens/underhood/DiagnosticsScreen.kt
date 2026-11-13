package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.service.ConnectionEvent
import `in`.dragonbra.vapulla.service.SteamDiagnostics
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.ProcessExit
import `in`.dragonbra.vapulla.util.ProcessExits
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import `in`.dragonbra.vapulla.util.Utils.toTimeString
import java.text.DateFormat
import java.util.Date
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val EXITS_TO_SHOW = 10

/**
 * How the app itself is doing: this run's connection history, and how earlier runs ended
 * according to the system. The place to look after the app went quiet in the background.
 */
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val events by SteamDiagnostics.events.collectAsStateWithLifecycle()
    val signedIn by SteamService.isLoggedIn.collectAsStateWithLifecycle()
    val exits = remember { ProcessExits.recent(context, EXITS_TO_SHOW).toImmutableList() }

    DiagnosticsContent(
        processStartedAt = SteamDiagnostics.processStartedAt,
        signedIn = signedIn,
        events = events,
        exits = exits,
        onBack = onBack,
    )
}

@Composable
private fun DiagnosticsContent(
    processStartedAt: Long,
    signedIn: Boolean,
    events: ImmutableList<ConnectionEvent>,
    exits: ImmutableList<ProcessExit>,
    onBack: () -> Unit,
) {
    UnderHoodScaffold(title = "Diagnostics", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom())
                .verticalScroll(rememberScrollState())
                .padding(listContentPadding(padding)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(title = "This run") {
                DetailList(
                    listOf(
                        "App started" to processStartedAt.toTimeAgo(),
                        "Steam" to if (signedIn) "Signed in" else "Not signed in",
                        "Reconnects" to events.count { it.text.startsWith("Reconnecting") }
                            .toString(),
                        "App version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})",
                        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    ),
                )
            }

            Section(title = "Connection history") {
                if (events.isEmpty()) {
                    Note("Nothing has happened on the connection in this run yet.")
                } else {
                    // Newest first, like the network log.
                    val newestFirst = events.asReversed()
                    TimelineList(
                        rows = newestFirst.map { it.timestamp.toTimeString() to it.text },
                    )
                }
            }

            Section(title = "Earlier runs") {
                if (exits.isEmpty()) {
                    Note("The system has no record of earlier runs (it keeps one from Android 11).")
                } else {
                    val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    TimelineList(
                        rows = exits.map { exit ->
                            format.format(Date(exit.timestamp)) to
                                (if (exit.abnormal) "⚠ " else "") + "Ended: ${exit.reason}" +
                                exit.detail.takeIf { it.isNotEmpty() }?.let { "\n$it" }.orEmpty()
                        },
                    )
                }
            }
        }
    }
}

/** Rows of a time and what happened then, in one grouped container. */
@Composable
private fun TimelineList(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { index, (time, text) ->
            Surface(
                shape = groupedListShape(index, rows.size),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    val now = System.currentTimeMillis()

    VapullaTheme {
        DiagnosticsContent(
            processStartedAt = now - 7_200_000,
            signedIn = true,
            events = persistentListOf(
                ConnectionEvent(0, now - 7_190_000, "Connecting"),
                ConnectionEvent(1, now - 7_189_000, "Connected to cmp1-ord1.steamserver.net:443"),
                ConnectionEvent(2, now - 7_188_000, "Signed in"),
                ConnectionEvent(3, now - 600_000, "Disconnected"),
                ConnectionEvent(4, now - 600_000, "Reconnecting, attempt 1 of 3"),
                ConnectionEvent(5, now - 597_000, "Signed in"),
            ),
            exits = persistentListOf(
                ProcessExit(now - 86_400_000, "killed for memory", abnormal = true, detail = ""),
                ProcessExit(now - 172_800_000, "stopped by the user", false, detail = ""),
            ),
            onBack = {},
        )
    }
}
