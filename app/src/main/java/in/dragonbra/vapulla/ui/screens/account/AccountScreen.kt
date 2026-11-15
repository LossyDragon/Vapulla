package `in`.dragonbra.vapulla.ui.screens.account

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.AccountDetails
import `in`.dragonbra.vapulla.service.api.FriendInvite
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.IconLabel
import `in`.dragonbra.vapulla.ui.composables.MenuButton
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.toDateString
import `in`.dragonbra.vapulla.util.helpers.toSteamIdString
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf

/**
 * The signed-in account, as Steam describes it to its own client: profile, wallet and email.
 * The profile name can be changed, and friend invite links made, shared and revoked.
 */
@Composable
fun AccountScreen(viewModel: AccountViewModel, onNavDrawerAction: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    AccountContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onChangeName = viewModel::changeName,
        onCreateInvite = viewModel::createInvite,
        onRevokeInvite = viewModel::revokeInvite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountContent(
    uiState: AccountUiState,
    snackbarHostState: SnackbarHostState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onChangeName: (String) -> Unit,
    onCreateInvite: () -> Unit,
    onRevokeInvite: (FriendInvite) -> Unit,
) {
    var editingName by remember { mutableStateOf(false) }
    var pendingRevoke by remember { mutableStateOf<FriendInvite?>(null) }

    if (editingName) {
        NameDialog(
            current = uiState.name,
            onConfirm = {
                onChangeName(it)
                editingName = false
            },
            onDismiss = { editingName = false },
        )
    }

    pendingRevoke?.let { invite ->
        AlertDialog(
            onDismissRequest = { pendingRevoke = null },
            title = { Text("Revoke invite link?") },
            text = { Text("The link stops working for anyone who has not used it yet.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRevokeInvite(invite)
                        pendingRevoke = null
                    },
                ) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevoke = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = { MenuButton(onClick = onNavDrawerAction) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshContent(
            isRefreshing = uiState.isLoadingInvites,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(listContentPadding(padding)),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                uiState.playingElsewhere?.let { PlayingElsewhere(game = it) }
                ProfileSection(uiState = uiState, onEditName = { editingName = true })
                WalletSection(uiState.details)
                EmailSection(uiState.details)
                InvitesSection(
                    invites = uiState.invites,
                    onCreate = onCreateInvite,
                    onRevoke = { pendingRevoke = it },
                )
            }
        }
    }
}

/**
 * Says that the account is in [game] on another device. Steam lets an account play on one
 * device at a time and tells the others; here it is only shown.
 */
@Composable
private fun PlayingElsewhere(game: String) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.SportsEsports, contentDescription = null)
            Column {
                Text(text = "Playing elsewhere", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$game is running on another device signed in to this account.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileSection(uiState: AccountUiState, onEditName: () -> Unit) {
    val details = uiState.details

    // The custom URL when the profile has one, the address by ID otherwise.
    val profileUrl = when {
        details.customUrl.isNotEmpty() -> "${Utils.Constants.CUSTOM_URL}${details.customUrl}/"
        uiState.steamId != 0L -> Utils.getProfileUrl(uiState.steamId)
        else -> ""
    }

    Section(title = "Profile") {
        DetailList(
            listOf(
                "Name" to uiState.name,
                "Steam ID" to uiState.steamId.takeIf { it != 0L }?.toSteamIdString().orEmpty(),
                "Country" to countryName(details.country),
                "Authorized computers" to details.authorizedComputers.takeIf { it > 0 }
                    ?.toString().orEmpty(),
                "Profile" to profileUrl,
            ),
        )
        FilledTonalButton(onClick = onEditName, shapes = ButtonDefaults.shapes()) {
            IconLabel(Icons.Outlined.Edit, "Change name")
        }
    }
}

@Composable
private fun WalletSection(details: AccountDetails) {
    Section(title = "Wallet") {
        when (details.hasWallet) {
            null -> Hint("Steam has not sent the wallet yet.")

            false -> Hint("This account has no Steam Wallet.")

            true -> DetailList(
                listOf(
                    "Balance" to Utils.formatMoney(details.walletCents, details.walletCurrency),
                    "On its way" to details.walletPendingCents.takeIf { it > 0 }
                        ?.let { Utils.formatMoney(it, details.walletCurrency) }.orEmpty(),
                    "Currency" to details.walletCurrency,
                ),
            )
        }
    }
}

@Composable
private fun EmailSection(details: AccountDetails) {
    Section(title = "Email") {
        if (details.emailValidated == null) {
            Hint("Steam has not sent the email address yet.")
        } else {
            DetailList(
                listOf(
                    "Address" to details.email,
                    "Status" to if (details.emailValidated) "Verified" else "Not verified",
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InvitesSection(
    invites: List<FriendInvite>,
    onCreate: () -> Unit,
    onRevoke: (FriendInvite) -> Unit,
) {
    val context = LocalContext.current

    Section(title = "Friend invite links") {
        Hint(
            "Whoever opens a link becomes your friend straight away, with no request to " +
                "accept. A new link is for one person and lasts 30 days.",
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            invites.forEachIndexed { index, invite ->
                InviteRow(
                    invite = invite,
                    index = index,
                    count = invites.size,
                    onShare = { context.share(invite.link) },
                    onRevoke = { onRevoke(invite) },
                )
            }
        }

        FilledTonalButton(onClick = onCreate, shapes = ButtonDefaults.shapes()) {
            IconLabel(Icons.Outlined.AddLink, "New invite link")
        }
    }
}

@Composable
private fun InviteRow(
    invite: FriendInvite,
    index: Int,
    count: Int,
    onShare: () -> Unit,
    onRevoke: () -> Unit,
) {
    val people = if (invite.uses == 1) "1 person" else "${invite.uses} people"
    val days = invite.durationSeconds / SECONDS_PER_DAY

    GroupedListItem(index = index, count = count) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = invite.link.removePrefix("https://"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Made ${invite.createdAt.toDateString()} · $people · $days days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, contentDescription = "Share this link")
            }
            IconButton(onClick = onRevoke) {
                Icon(Icons.Outlined.Delete, contentDescription = "Revoke this link")
            }
        }
    }
}

/** Asks for a new profile name, starting from the [current] one. */
@Composable
private fun NameDialog(current: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change profile name") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This is the name friends see. Steam keeps a history of earlier names.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && name.trim() != current,
                onClick = { onConfirm(name) },
            ) { Text("Change") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/** Hands [text] to the system's share sheet, which also offers to copy it. */
private fun Context.share(text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}

/** "Germany (DE)" for "DE"; the code alone when it is not a country the device knows. */
private fun countryName(code: String): String {
    if (code.isEmpty()) return ""

    // Steam may send something that is not a country code at all.
    val name = runCatching { Locale.Builder().setRegion(code).build().displayCountry }
        .getOrDefault("")
    return if (name.isEmpty() || name == code) code else "$name ($code)"
}

private const val SECONDS_PER_DAY = 24 * 60 * 60

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        AccountContent(
            uiState = AccountUiState(
                name = "Vapulla",
                steamId = 76561197960287930,
                details = AccountDetails(
                    country = "DE",
                    authorizedComputers = 3,
                    hasWallet = true,
                    walletCents = 1234,
                    walletCurrency = "EUR",
                    email = "someone@example.com",
                    emailValidated = true,
                    customUrl = "vapulla",
                ),
                invites = persistentListOf(
                    FriendInvite(
                        token = "ABCDEFGH",
                        link = "https://s.team/p/cgbw-dqkq/ABCDEFGH",
                        uses = 1,
                        durationSeconds = 30L * SECONDS_PER_DAY,
                        createdAt = 1_760_722_982,
                    ),
                ),
                isLoadingInvites = false,
                playingElsewhere = "Portal 2",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavDrawerAction = {},
            onRefresh = {},
            onChangeName = {},
            onCreateInvite = {},
            onRevokeInvite = {},
        )
    }
}
