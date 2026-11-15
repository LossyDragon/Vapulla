package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.Routes
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/** @property needsAccount it shows something of the account, so not in an anonymous session. */
private data class Tool(
    val route: Routes,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val needsAccount: Boolean = false,
)

private val TOOLS = listOf(
    Tool(
        route = Routes.ConnectionInfo,
        title = "Connection",
        description = "The server you are on and what Steam said when it signed you in",
        icon = Icons.Outlined.SettingsEthernet,
    ),
    Tool(
        route = Routes.CmServers,
        title = "CM servers",
        description = "Every connection manager server the client knows about",
        icon = Icons.Outlined.Dns,
    ),
    Tool(
        route = Routes.NetworkLog,
        title = "Network log",
        description = "Packets as they are sent and received, and totals per message type",
        icon = Icons.Outlined.Terminal,
    ),
    Tool(
        route = Routes.Licenses,
        title = "Licenses",
        description = "The packages on your account, and which one grants a given app",
        icon = Icons.Outlined.Key,
        needsAccount = true,
    ),
    Tool(
        route = Routes.Depots(),
        title = "Depots",
        description = "An app's depots, branches, build IDs and manifests",
        icon = Icons.Outlined.Storage,
    ),
    Tool(
        route = Routes.DownloadInternals,
        title = "Download internals",
        description = "The running download in chunks, bytes and connections",
        icon = Icons.Outlined.Speed,
        needsAccount = true,
    ),
    Tool(
        route = Routes.PicsExplorer(),
        title = "PICS explorer",
        description = "Everything Steam's catalogue holds on any app or package",
        icon = Icons.Outlined.AccountTree,
    ),
    Tool(
        route = Routes.PicsFeed,
        title = "PICS change feed",
        description = "Apps and packages changing across Steam, as they change",
        icon = Icons.Outlined.DynamicFeed,
    ),
    Tool(
        route = Routes.WebApi,
        title = "Web API",
        description = "The interfaces and methods of api.steampowered.com",
        icon = Icons.Outlined.Api,
    ),
    Tool(
        route = Routes.IdDecoder,
        title = "ID decoder",
        description = "Break a Steam ID, game ID or global ID into its parts",
        icon = Icons.Outlined.Pin,
    ),
    Tool(
        route = Routes.AuthTicket,
        title = "Auth tickets",
        description = "Make the ticket a game hands to a server, and see what is in it",
        icon = Icons.Outlined.ConfirmationNumber,
        needsAccount = true,
    ),
    Tool(
        route = Routes.Diagnostics,
        title = "Diagnostics",
        description = "Connection history, uptime and how earlier runs of the app ended",
        icon = Icons.Outlined.MonitorHeart,
    ),
)

/**
 * Table of contents for the screens that show how Steam and JavaSteam work underneath.
 *
 * It is also the whole app in an anonymous session, started from the sign-in screen: the
 * tools that need an account are left out then, and going back ends the session.
 *
 * @param onLeave ends an anonymous session; only used when [anonymous].
 */
@Composable
fun UnderTheHoodScreen(
    onNavDrawerAction: () -> Unit,
    onOpen: (Routes) -> Unit,
    anonymous: Boolean = false,
    onLeave: () -> Unit = {},
) {
    val tools = remember(anonymous) {
        if (anonymous) TOOLS.filterNot { it.needsAccount } else TOOLS
    }

    BackHandler(enabled = anonymous, onBack = onLeave)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Under the Hood") },
                navigationIcon = {
                    if (anonymous) {
                        BackButton(onClick = onLeave)
                    } else {
                        IconButton(onClick = onNavDrawerAction) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                        }
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
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (anonymous) {
                item(key = "anonymous") {
                    Text(
                        text = "You are signed in anonymously, the way SteamCMD is: no account, " +
                            "and only what Steam shows anyone. The tools that need an " +
                            "account are left out. Go back to sign in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
                    )
                }
            }
            itemsIndexed(tools, key = { _, tool -> tool.title }) { index, tool ->
                Surface(
                    shape = groupedListShape(index, tools.size),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(tool.route) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = tool.icon, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = tool.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared frame of the Under the Hood detail screens: a title, a back button, and a body that
 * is handed the scaffold padding. A screen may add a floating button.
 */
@Composable
internal fun UnderHoodScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = { actions() },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        UnderTheHoodScreen(onNavDrawerAction = {}, onOpen = {})
    }
}
