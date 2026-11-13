package `in`.dragonbra.vapulla.ui.screens.sessions

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.AuthSession
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.SkeletonList
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import kotlinx.collections.immutable.persistentListOf

@Composable
fun SessionsScreen(viewModel: SessionsViewModel, onNavDrawerAction: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    SessionsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onRevoke = viewModel::revoke,
    )
}

@Composable
private fun SessionsContent(
    uiState: SessionsUiState,
    snackbarHostState: SnackbarHostState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onRevoke: (AuthSession) -> Unit,
) {
    var pendingRevoke by remember { mutableStateOf<AuthSession?>(null) }

    pendingRevoke?.let { session ->
        SignOutDialog(
            session = session,
            onConfirm = {
                onRevoke(session)
                pendingRevoke = null
            },
            onDismiss = { pendingRevoke = null },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshContent(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            when {
                uiState.isLoading && uiState.sessions.isEmpty() ->
                    SkeletonList(contentPadding = listContentPadding(padding))

                uiState.error != null -> RefreshableMessage(message = uiState.error)

                uiState.sessions.isEmpty() -> RefreshableMessage(message = "No sessions reported")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Stays at the top while the devices scroll under it.
                    stickyHeader(key = "sign-out-note") { SignOutNote() }

                    itemsIndexed(uiState.sessions, key = { _, session ->
                        session.tokenId
                    }) { index, session ->
                        SessionRow(
                            session = session,
                            shape = groupedListShape(index, uiState.sessions.size),
                            onRevoke = { pendingRevoke = session },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Confirms signing [session] out. That cannot be undone from here, so it takes two deliberate
 * steps: ticking the box, then the button it enables.
 */
@Composable
private fun SignOutDialog(session: AuthSession, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var understood by remember(session.tokenId) { mutableStateOf(false) }
    val name = session.description.ifBlank { session.platform }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
        title = { Text("Sign out device?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$name will be signed out of your account and has to sign in again.")
                Text(
                    text = "Do not do this to a primary device, such as the phone that " +
                        "holds your Steam Guard authenticator.",
                    color = MaterialTheme.colorScheme.error,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = understood,
                            role = Role.Checkbox,
                            onValueChange = { understood = it },
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The row is the click target; the box only shows the state.
                    Checkbox(checked = understood, onCheckedChange = null)
                    Text("I understand, and this is not one of my primary devices")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = understood, onClick = onConfirm) { Text("Sign out") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * What the sign-out button on each row does, and the warning that goes with it. The outer
 * surface is the screen's background colour so rows scrolling beneath do not show through.
 */
@Composable
private fun SignOutNote() {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Warning, contentDescription = null)
                Text(
                    text = "The sign-out button removes that device's access to your account. " +
                        "Do not sign out your primary devices, such as the phone with your " +
                        "Steam Guard authenticator or your main PC.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: AuthSession,
    shape: androidx.compose.ui.graphics.Shape,
    onRevoke: () -> Unit,
) {
    val icon = when (session.platform) {
        "Mobile app" -> Icons.Outlined.PhoneAndroid
        "Web browser" -> Icons.Outlined.Language
        else -> Icons.Outlined.Computer
    }
    val place = listOf(session.lastSeenCity, session.lastSeenCountry).filter {
        it.isNotBlank()
    }.joinToString(", ")
    val seen = if (session.lastSeen > 0) (session.lastSeen * 1000).toTimeAgo() else "never"

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = session.description.ifBlank { session.platform },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (place.isEmpty()) "Last seen $seen" else "Last seen $seen · $place",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoPill(text = session.platform)
                    if (session.isThisDevice) {
                        InfoPill(
                            text = "This device",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (session.loggedIn) InfoPill(text = "Online")
                }
            }
            if (!session.isThisDevice) {
                IconButton(onClick = onRevoke) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = "Sign out this device",
                    )
                }
            }
        }
    }
}

private val previewSessions = persistentListOf(
    AuthSession(
        tokenId = 1,
        description = "Pixel 9",
        platform = "Mobile app",
        loggedIn = true,
        isThisDevice = true,
        firstSeen = 1_760_000_000,
        lastSeen = 1_760_722_982,
        lastSeenCity = "Seattle",
        lastSeenCountry = "United States",
    ),
    AuthSession(
        tokenId = 2,
        description = "DESKTOP-GAMING",
        platform = "Steam client",
        loggedIn = false,
        isThisDevice = false,
        firstSeen = 1_750_000_000,
        lastSeen = 1_760_600_000,
        lastSeenCity = "",
        lastSeenCountry = "United States",
    ),
    AuthSession(
        tokenId = 3,
        description = "",
        platform = "Web browser",
        loggedIn = false,
        isThisDevice = false,
        firstSeen = 1_740_000_000,
        lastSeen = 0,
        lastSeenCity = "",
        lastSeenCountry = "",
    ),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        SessionsContent(
            uiState = SessionsUiState(sessions = previewSessions, isLoading = false),
            snackbarHostState = remember { SnackbarHostState() },
            onNavDrawerAction = {},
            onRefresh = {},
            onRevoke = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_SignOutDialog() {
    VapullaTheme {
        SignOutDialog(session = previewSessions[1], onConfirm = {}, onDismiss = {})
    }
}
