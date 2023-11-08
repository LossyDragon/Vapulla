package `in`.dragonbra.vapulla.compose.screens.invites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.model.InviteTokenItem
import kotlin.random.Random
import kotlinx.coroutines.launch

@Composable
fun InvitesScreen(
    viewModel: InvitesViewModel,
    onBackPressed: () -> Unit,
    onGenerateLink: () -> Unit,
    onDeleteInvite: (token: String) -> Unit
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    var deleteInviteToken by remember { mutableStateOf("") }
    var deleteInviteDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        icon = Icons.Default.Delete,
        title = "Delete link",
        message = "Are you sure you want to delete this invite link?",
        openDialog = deleteInviteDialog,
        onPositive = {
            onDeleteInvite(deleteInviteToken)
            deleteInviteToken = ""
            deleteInviteDialog = false
        },
        positiveText = stringResource(id = R.string.confirm),
        onNegative = {
            deleteInviteToken = ""
            deleteInviteDialog = false
        },
        negativeText = stringResource(id = R.string.cancel)
    )

    InvitesContent(
        state = state.value,
        onBackPressed = onBackPressed,
        onGenerateLink = onGenerateLink,
        onDeleteInvite = {
            deleteInviteToken = it
            deleteInviteDialog = true
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitesContent(
    state: InvitesState,
    onBackPressed: () -> Unit,
    onGenerateLink: () -> Unit,
    onDeleteInvite: (token: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            VapullaAppbar(
                toolbarText = stringResource(id = R.string.title_activity_invites),
                onBackPressed = onBackPressed
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.Transparent //
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.invites_tab_pending),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = {
                        Text(
                            text = stringResource(id = R.string.invites_tab_quick),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            val msg = stringResource(id = R.string.snackbar_create_invite_link)
            when (tabIndex) {
                0 -> PendingInvitesScreen()
                1 -> QuickInvitesScreen(
                    state = state,
                    scrollState = scrollState,
                    onGenerateLink = {
                        onGenerateLink()
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    onDeleteInvite = onDeleteInvite
                )
            }
        }
    }
}

@Composable
private fun PendingInvitesScreen() {
    // TODO
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Blep")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickInvitesScreen(
    state: InvitesState,
    scrollState: LazyListState,
    onGenerateLink: () -> Unit,
    onDeleteInvite: (token: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp),
        state = scrollState

    ) {
        item {
            InviteGenerateCard(onGenerateLink = onGenerateLink)
        }

        item {
            val messageInfo = if (state.inviteTokens.isEmpty()) {
                stringResource(id = R.string.quick_invite_no_items)
            } else {
                stringResource(id = R.string.quick_invite_expire_info)
            }
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
                fontSize = 12.sp,
                text = messageInfo,
                textAlign = TextAlign.Center
            )
        }

        items(items = state.inviteTokens, key = { it.inviteToken }) { token ->
            if (token.isValid) {
                InviteLinkCard(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .animateItemPlacement(),
                    state = state,
                    token = token,
                    onDeleteInvite = onDeleteInvite
                )
            }
        }
    }
}

@Preview
@Composable
private fun InvitesContent_Preview() {
    fun inviteToken(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..8)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    fun timeCreated(): Long {
        val minValue = 1_000_000_000L // 10^9
        val maxValue = 9_999_999_999L // 10^10 - 1
        return Random.nextLong(minValue, maxValue + 1)
    }

    fun inviteDuration(): Long {
        return 30L * 24 * 60 * 60 * 1000
    }

    val inviteTokens = mutableListOf<InviteTokenItem>()
    repeat(12) { number ->
        inviteTokens.add(
            InviteTokenItem(
                inviteToken = inviteToken(),
                inviteLimit = if (number % 2 == 0) 1 else 0,
                inviteDuration = if (number % 2 == 0) inviteDuration() else 0,
                timeCreated = timeCreated(),
                isValid = number % 2 == 0
            )
        )
    }

    val state = InvitesState(
        inviteTokens = inviteTokens,
        loggedInSteamID = SteamID(76561198003805806),
        loggedInUniverse = EUniverse.Public
    )
    VapullaTheme {
        InvitesContent(
            state = state,
            onBackPressed = {},
            onGenerateLink = {},
            onDeleteInvite = {}
        )
    }
}
