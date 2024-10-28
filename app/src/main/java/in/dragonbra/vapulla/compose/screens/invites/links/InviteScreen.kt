package `in`.dragonbra.vapulla.compose.screens.invites.links

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.model.InviteTokenItem
import kotlin.random.Random

@Composable
fun InviteScreen(
    viewModel: InviteLinksViewModel,
    onBackPressed: () -> Unit,
    onGenerateLink: () -> Unit,
    onDeleteInvite: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InviteContent(
        state = state,
        onBackPressed = onBackPressed,
        onGenerateLink = onGenerateLink,
        onDeleteInvite = onDeleteInvite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteContent(
    state: InvitesState,
    onBackPressed: () -> Unit,
    onGenerateLink: () -> Unit,
    onDeleteInvite: (String) -> Unit,

    ) {
    val scrollState = rememberLazyListState()

    Scaffold(
        topBar = {
            VapullaAppbar(
                toolbarText = stringResource(id = R.string.title_activity_invites),
                onBackPressed = onBackPressed
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onGenerateLink) {
                Icon(Icons.Default.Add, "Generate new invite link")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 64.dp),
            state = scrollState
        ) {
            items(items = state.inviteTokens, key = { it.inviteToken }) { token ->
                if (token.isValid) {
                    InviteLinkCard(
//                        modifier = Modifier
//                            .clip(shape = RoundedCornerShape(12.dp))
//                            .padding(horizontal = 16.dp, vertical = 8.dp)
//                            .animateItem(),
                        token = token,
                        steamID = state.loggedInSteamID!!,
                        universe = state.loggedInUniverse,
                        onDelete = onDeleteInvite
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_InviteContent() {
    fun timeCreated(): Long = Random.nextLong(1_000_000_000L, 9_999_999_999L + 1)

    fun inviteDuration(): Long = 30L * 24 * 60 * 60 * 1000

    fun inviteToken(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..8)
            .map { Random.nextInt(0, characters.length) }
            .map(characters::get)
            .joinToString("")
    }

    VapullaTheme {
        InviteContent(
            state = InvitesState(
                inviteTokens = List(10) { number ->
                    InviteTokenItem(
                        inviteToken = inviteToken(),
                        inviteLimit = if (number % 2 == 0) 1 else 0,
                        inviteDuration = if (number % 2 == 0) inviteDuration() else 0,
                        timeCreated = timeCreated(),
                        isValid = number % 2 == 0
                    )
                },
                loggedInSteamID = SteamID(76561198003805806),
                loggedInUniverse = EUniverse.Public,
            ),
            onBackPressed = {},
            onGenerateLink = {},
            onDeleteInvite = {},
        )
    }
}
