package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.TicketInfo
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.service.PacketInspector
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchField
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.AuthTicketReader
import `in`.dragonbra.vapulla.util.TicketPart
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property appId the app that was picked; 0 while there is none.
 * @property title the picked app, by name where it is known.
 * @property made true once a ticket for the picked app is on show.
 * @property size the ticket's length in bytes.
 * @property parts the ticket's fields, read by [AuthTicketReader].
 * @property bytes the whole ticket as a hex dump.
 * @property cancelled true once Steam was told the ticket is no longer in use.
 * @property nameMatches the apps a search by name found; empty otherwise.
 */
@Immutable
data class AuthTicketUiState(
    val isLoading: Boolean = false,
    val appId: Int = 0,
    val title: String = "",
    val made: Boolean = false,
    val size: Int = 0,
    val parts: ImmutableList<TicketPart> = persistentListOf(),
    val bytes: String = "",
    val cancelled: Boolean = false,
    val nameMatches: ImmutableList<PicsNameMatch> = persistentListOf(),
    val message: String? = null,
)

/**
 * Makes an auth session ticket for an app with JavaSteam's `SteamAuthTicket` and shows what
 * is in it. Picking an app does not make one yet: each ticket uses up one of the few game
 * connect tokens Steam hands out at sign-in, so that takes a press of its own.
 *
 * The ticket is a working credential for that game for as long as Steam counts it as in
 * use, so it is cancelled when another app is picked and when the screen is left, and it
 * is never logged or copied.
 */
class AuthTicketViewModel(
    private val connection: SteamServiceConnection,
    private val appDao: SteamAppDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthTicketUiState())
    val uiState: StateFlow<AuthTicketUiState> = _uiState.asStateFlow()

    /** The ticket on show while Steam still counts it as in use. */
    private var ticket: TicketInfo? = null
    private var job: Job? = null

    /** Picks the app to make a ticket for; [name] is its name when it is known. */
    fun select(appId: Int, name: String? = null) {
        job?.cancel()
        discard()
        _uiState.value = AuthTicketUiState(
            appId = appId,
            title = listOfNotNull(name?.decodeHtml(), "App $appId").joinToString(" · "),
        )
    }

    /** Makes a ticket for the picked app, in place of the one on show. */
    fun make() {
        val picked = _uiState.value
        if (picked.appId == 0) return

        job?.cancel()
        job = viewModelScope.launch {
            discard()
            _uiState.value = picked.copy(isLoading = true, message = null)

            val made = connection
                .request("Auth ticket for app ${picked.appId}") {
                    it.authTickets.make(picked.appId)
                }
                .getOrElse { error ->
                    _uiState.value = picked.copy(
                        made = false,
                        message = error.message
                            ?: "Steam gave no ticket for app ${picked.appId}",
                    )
                    return@launch
                }

            ticket = made
            _uiState.value = AuthTicketUiState(
                appId = picked.appId,
                title = picked.title,
                made = true,
                size = made.ticket.size,
                parts = AuthTicketReader.read(made.ticket).toImmutableList(),
                bytes = PacketInspector.hexDump(made.ticket),
            )
        }
    }

    /** Looks for apps the app has stored by [name], to pick one from. */
    fun findByName(name: String) {
        val query = name.trim()

        job?.cancel()
        job = viewModelScope.launch {
            discard()
            _uiState.value = AuthTicketUiState(isLoading = true)

            val matches = appDao.findAppsByName(query)

            _uiState.value = AuthTicketUiState(
                nameMatches = matches.toImmutableList(),
                message = if (matches.isEmpty()) {
                    "No stored app has \"$query\" in its name. Try its app ID."
                } else {
                    null
                },
            )
        }
    }

    /** Tells Steam the ticket on show is no longer in use; it stays on screen to read. */
    fun cancel() {
        discard()
        _uiState.update { it.copy(cancelled = true) }
    }

    override fun onCleared() = discard()

    private fun discard() {
        val made = ticket ?: return
        ticket = null
        connection.steamService?.apis?.authTickets?.cancel(made)
    }
}

@Composable
fun AuthTicketScreen(viewModel: AuthTicketViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AuthTicketContent(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onFindByName = viewModel::findByName,
        onMake = viewModel::make,
        onCancel = viewModel::cancel,
    )
}

@Composable
private fun AuthTicketContent(
    state: AuthTicketUiState,
    onBack: () -> Unit,
    onSelect: (Int, String?) -> Unit,
    onFindByName: (String) -> Unit,
    onMake: () -> Unit,
    onCancel: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var text by rememberSaveable { mutableStateOf("") }

    UnderHoodScaffold(title = "Auth tickets", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            SearchField(
                value = text,
                onValueChange = { text = it },
                placeholder = "App ID or name of a game you own",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                // A number is an app ID; anything else is a name, looked up in the app.
                onSearch = {
                    keyboard?.hide()
                    searchByIdOrName(text, onId = { onSelect(it, null) }, onName = onFindByName)
                },
            )

            when {
                state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())

                state.nameMatches.isNotEmpty() -> NameMatchList(
                    matches = state.nameMatches,
                    scaffoldPadding = padding,
                    onPick = { match ->
                        text = match.id.toString()
                        onSelect(match.id, match.title)
                    },
                )

                state.appId == 0 -> EmptyMessage(
                    message = state.message
                        ?: "Enter a game you own to make an auth session ticket for it: the " +
                        "proof of who you are and that you own the game, which a game " +
                        "hands to a server when you join",
                )

                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(listContentPadding(padding)),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (state.made) {
                        Ticket(state = state, onMake = onMake, onCancel = onCancel)
                    } else {
                        Picked(state = state, onMake = onMake)
                    }
                }
            }
        }
    }
}

/** The app that was picked, before a ticket is made for it. */
@Composable
private fun Picked(state: AuthTicketUiState, onMake: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
        Hint(
            "Each ticket uses up one of the few game connect tokens Steam hands out at " +
                "sign-in. When they run out, no more can be made until you sign in again.",
        )
        state.message?.let { message ->
            // Like a Hint, but in the error colour.
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Button(onClick = onMake) { Text("Make ticket") }
    }
}

/** The ticket that was made: its state, its parts, and on request its bytes. */
@Composable
private fun Ticket(state: AuthTicketUiState, onMake: () -> Unit, onCancel: () -> Unit) {
    var showBytes by rememberSaveable(state.bytes) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
        DetailList(
            listOf(
                "Size" to "${state.size} bytes",
                "State" to
                    if (state.cancelled) "Cancelled" else "In use, as far as Steam knows",
            ),
        )
        Hint(
            "While it is in use, a server that is handed this ticket is told by Steam " +
                "that it is you. It is cancelled when you leave this screen, pick another " +
                "game or make a new one.",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.cancelled) {
                Button(onClick = onMake) { Text("Make a new one") }
            } else {
                OutlinedButton(onClick = onCancel) { Text("Cancel ticket") }
            }
            TextButton(onClick = { showBytes = !showBytes }) {
                Text(if (showBytes) "Hide bytes" else "Show bytes")
            }
        }
    }

    state.parts.forEach { part ->
        Section(title = part.title) {
            Hint(part.note)
            DetailList(part.rows)
        }
    }
    if (state.parts.isEmpty()) {
        Hint("The ticket's layout was not recognised; its bytes can still be shown.")
    }

    if (showBytes) {
        Section(title = "Bytes") {
            Text(
                text = state.bytes,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .horizontalScroll(rememberScrollState()),
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        AuthTicketContent(
            state = AuthTicketUiState(
                appId = 440,
                title = "Team Fortress 2 · App 440",
                made = true,
                size = 234,
                parts = persistentListOf(
                    TicketPart(
                        title = "App ownership ticket",
                        note = "Steam's signed statement that the account owns the app.",
                        rows = listOf(
                            "Version" to "4",
                            "Steam ID" to "76561197960287930",
                            "App ID" to "440",
                            "Packages" to "0, 469",
                            "Signature" to "128 bytes",
                        ),
                    ),
                ),
            ),
            onBack = {},
            onSelect = { _, _ -> },
            onFindByName = {},
            onMake = {},
            onCancel = {},
        )
    }
}
