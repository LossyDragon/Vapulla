package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.ConnectionInfo
import `in`.dragonbra.vapulla.service.SteamDiagnostics
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import kotlin.math.abs
import kotlinx.collections.immutable.persistentListOf

/**
 * Where the client is connected and what Steam said in its answer to the logon
 * (`LoggedOnCallback`): the values the library otherwise uses silently.
 */
@Composable
fun ConnectionScreen(onBack: () -> Unit) {
    val info by SteamDiagnostics.connection.collectAsStateWithLifecycle()

    ConnectionContent(info = info, onBack = onBack)
}

@Composable
private fun ConnectionContent(info: ConnectionInfo?, onBack: () -> Unit) {
    UnderHoodScaffold(title = "Connection", onBack = onBack) { padding ->
        if (info == null) {
            SearchResultMessage(
                message = "Not signed in to Steam right now",
                modifier = Modifier.padding(padding),
            )
            return@UnderHoodScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom())
                .verticalScroll(rememberScrollState())
                .padding(listContentPadding(padding)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(title = "Server") {
                DetailList(
                    listOf(
                        "CM server" to info.server,
                        "Protocols allowed" to info.allowedProtocols,
                        "Cell ID" to info.cellId.toString(),
                        "Signed in" to info.signedInAt.toTimeAgo(),
                    ),
                )
            }

            Section(title = "Session") {
                DetailList(
                    listOf(
                        "Steam ID" to info.steamId.toULong().toString(),
                        "Session ID" to info.sessionId?.toUInt()?.toString().orEmpty(),
                        "Heartbeat" to "every ${info.heartbeatSeconds} s",
                        "Steam's clock" to info.serverTimeOffsetMs?.let(::clockOffset).orEmpty(),
                    ),
                )
            }

            Section(title = "As Steam sees you") {
                DetailList(
                    listOf(
                        "Public IP" to info.publicIp,
                        "IP country" to info.ipCountry,
                        "Email domain" to info.emailDomain,
                        "Profile name in URL" to info.vanityUrl,
                        "Family group" to info.familyGroupId.takeIf { it != 0L }
                            ?.toString().orEmpty(),
                    ),
                )
            }

            if (info.accountFlags.isNotEmpty()) {
                Section(title = "Account flags") {
                    DetailList(info.accountFlags.map { it to "Set" })
                }
            }
        }
    }
}

private fun clockOffset(offsetMs: Long): String = when {
    abs(offsetMs) < 1_000 -> "Same as this device"
    offsetMs > 0 -> "${offsetMs / 1_000} s ahead of this device"
    else -> "${-offsetMs / 1_000} s behind this device"
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        ConnectionContent(
            info = ConnectionInfo(
                server = "cmp1-ord1.steamserver.net:443",
                allowedProtocols = "TCP, WEB_SOCKET",
                steamId = 76561197960287930,
                sessionId = 1234567,
                cellId = 33,
                publicIp = "203.0.113.7",
                ipCountry = "US",
                heartbeatSeconds = 9,
                serverTimeOffsetMs = -2_300,
                accountFlags = persistentListOf("PersonaNameSet", "EmailValidated"),
                emailDomain = "example.com",
                vanityUrl = "vapulla",
                familyGroupId = 0,
                signedInAt = System.currentTimeMillis() - 3_600_000,
            ),
            onBack = {},
        )
    }
}
