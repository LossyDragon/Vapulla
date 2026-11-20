package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.service.NotConnectedException
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchField
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.copyToClipboard
import `in`.dragonbra.vapulla.util.helpers.toSteamIdString
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
 * One key of the info, as a row of the tree.
 *
 * @property path where it sits, unique in the tree; what [PicsExplorerViewModel.toggle] takes.
 * @property depth how far it is indented; 0 for the top sections.
 * @property value null for a key that holds other keys.
 * @property children how many keys it holds directly.
 */
@Immutable
data class PicsNode(
    val path: String,
    val depth: Int,
    val key: String,
    val value: String?,
    val children: Int,
    val expanded: Boolean = false,
)

/**
 * @property facts what Steam says about the info as a whole: change number, token.
 * @property nodes the rows on show: everything except what lies under a closed key, or
 *   with a [filter] only the keys that match it and the keys they sit under.
 * @property filter the text the tree is searched for; empty for the whole tree.
 * @property filterMatches how many keys match the [filter].
 * @property nameMatches the apps or packages a search by name found; empty otherwise.
 */
@Immutable
data class PicsExplorerUiState(
    val isApp: Boolean = true,
    val isLoading: Boolean = false,
    val title: String? = null,
    val facts: ImmutableList<Pair<String, String>> = persistentListOf(),
    val nodes: ImmutableList<PicsNode> = persistentListOf(),
    val filter: String = "",
    val filterMatches: Int = 0,
    val nameMatches: ImmutableList<PicsNameMatch> = persistentListOf(),
    val message: String? = null,
)

/**
 * Shows the PICS info of any app or package as the tree of keys Steam sends: the source of
 * everything the library, the depot browser and the downloads know about a game.
 *
 * Steam only takes IDs. A name is looked up in what the app has stored: the apps it knows,
 * and for a package the games the account's packages grant, since packages have no name.
 *
 * @param initialId an ID to show straight away, or 0 to start with the search box.
 */
class PicsExplorerViewModel(
    initialId: Int,
    initialIsApp: Boolean,
    private val connection: SteamServiceConnection,
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PicsExplorerUiState(isApp = initialIsApp))
    val uiState: StateFlow<PicsExplorerUiState> = _uiState.asStateFlow()

    /** The whole tree in reading order; the rows on show are picked out of it. */
    private var all: List<PicsNode> = emptyList()
    private var open: Set<String> = emptySet()
    private var filter: String = ""
    private var loadJob: Job? = null

    init {
        if (initialId > 0) load(initialId)
    }

    fun setIsApp(isApp: Boolean) = _uiState.update { it.copy(isApp = isApp) }

    fun load(id: Int) {
        val isApp = _uiState.value.isApp
        val kind = if (isApp) "app" else "package"

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PicsExplorerUiState(isApp = isApp, isLoading = true)

            val result = connection.request("PICS info for $kind $id") {
                it.library.productInfo(id, isApp)
            }
            val product = result.getOrNull()

            if (product == null) {
                val reason = if (result.exceptionOrNull() is NotConnectedException) {
                    "Not connected to Steam"
                } else {
                    "Steam has no $kind $id"
                }
                _uiState.value = PicsExplorerUiState(isApp = isApp, message = reason)
                return@launch
            }

            all = buildList { flatten(product.keyValues, parent = "", depth = 0) }
            open = emptySet()
            filter = ""

            val name = product.keyValues["common"]["name"].value
            _uiState.value = PicsExplorerUiState(
                isApp = isApp,
                title = listOfNotNull(name, "${kind.replaceFirstChar(Char::uppercase)} $id")
                    .joinToString(" · "),
                facts = persistentListOf(
                    "Change number" to product.changeNumber.toString(),
                    "Access token" to
                        if (product.token == 0L) "None" else product.token.toSteamIdString(),
                    "Held back" to if (product.missingToken) "Yes, a token is needed" else "",
                    "Keys" to all.size.toString(),
                ),
                nodes = visible().nodes,
            )
        }
    }

    /**
     * Looks for apps, or packages, by the name of a game. An app is one the app has stored;
     * a package is one of the account's that grants a game of that name.
     */
    fun findByName(name: String) {
        val isApp = _uiState.value.isApp
        val query = name.trim()

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PicsExplorerUiState(isApp = isApp, isLoading = true)

            val apps = appDao.findAppsByName(query)
            val matches = if (isApp) {
                apps
            } else {
                val names = apps.associate { it.id to it.title }
                licenseDao.getAllLicenses()
                    .mapNotNull { license ->
                        val games = license.appIds.mapNotNull(names::get)
                        if (games.isEmpty()) return@mapNotNull null

                        PicsNameMatch(
                            id = license.packageID,
                            title = "Package ${license.packageID}",
                            detail = games.joinToString(),
                        )
                    }
                    .sortedBy { it.detail.lowercase() }
            }

            _uiState.value = PicsExplorerUiState(
                isApp = isApp,
                nameMatches = matches.toImmutableList(),
                message = when {
                    matches.isNotEmpty() -> null
                    isApp -> "No stored app has \"$query\" in its name. Try its app ID."
                    else -> "None of your packages grants a game with \"$query\" in its name."
                },
            )
        }
    }

    /** Searches the tree for [text]; an empty one shows the whole tree again. */
    fun setFilter(text: String) {
        filter = text
        show()
    }

    /**
     * Opens or closes the key at [path]. Not while the tree is searched: every match is
     * opened up then, so there is nothing to open or close.
     */
    fun toggle(path: String) {
        if (filter.isNotBlank()) return

        open = if (path in open) open - path else open + path
        show()
    }

    /**
     * Opens every key, or closes them all if any on show is open, which is what the button
     * for it goes by. Not while the tree is searched, as for [toggle].
     */
    fun toggleAll() {
        if (filter.isNotBlank()) return

        open = if (_uiState.value.nodes.any { it.expanded }) {
            emptySet()
        } else {
            all.filter { it.children > 0 }.mapTo(HashSet()) { it.path }
        }
        show()
    }

    /** Puts the rows that [open] and [filter] now call for on show. */
    private fun show() {
        val visible = visible()
        _uiState.update {
            it.copy(filter = filter, nodes = visible.nodes, filterMatches = visible.matches)
        }
    }

    /** Adds the keys under [keyValue] as rows, each followed by its own keys. */
    private fun MutableList<PicsNode>.flatten(keyValue: KeyValue, parent: String, depth: Int) {
        keyValue.children.forEachIndexed { index, child ->
            // The index keeps paths apart where a key appears twice.
            val path = "$parent/$index"
            add(
                PicsNode(
                    path = path,
                    depth = depth,
                    key = child.name.orEmpty(),
                    value = child.value.takeIf { child.children.isEmpty() },
                    children = child.children.size,
                ),
            )
            flatten(child, path, depth + 1)
        }
    }

    /**
     * The rows to show: everything except what lies under a closed key. With a [filter] it
     * is the keys whose name or value contains it, each with the keys it sits under, all
     * opened up so that every match is on show.
     */
    private fun visible(): Visible {
        val query = filter.trim()
        if (query.isNotEmpty()) {
            val matches = all.filter { node ->
                node.key.contains(query, ignoreCase = true) ||
                    node.value?.contains(query, ignoreCase = true) == true
            }

            // A path is its parent's path plus one more step, so the parents are its prefixes.
            val keep = HashSet<String>()
            matches.forEach { node ->
                var path = node.path
                while (path.isNotEmpty() && keep.add(path)) {
                    path = path.substringBeforeLast('/')
                }
            }

            val rows = all
                .filter { it.path in keep }
                .map { if (it.children > 0) it.copy(expanded = true) else it }
            return Visible(rows.toImmutableList(), matches.size)
        }

        val rows = ArrayList<PicsNode>()
        var hideDeeperThan = Int.MAX_VALUE

        for (node in all) {
            if (node.depth > hideDeeperThan) continue
            hideDeeperThan = Int.MAX_VALUE

            val expanded = node.path in open
            rows += if (expanded) node.copy(expanded = true) else node
            if (node.children > 0 && !expanded) hideDeeperThan = node.depth
        }
        return Visible(rows.toImmutableList(), matches = 0)
    }

    /** The rows on show, and how many of them match the filter. */
    private class Visible(val nodes: ImmutableList<PicsNode>, val matches: Int)
}

@Composable
fun PicsExplorerScreen(viewModel: PicsExplorerViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PicsExplorerContent(
        state = state,
        onBack = onBack,
        onIsApp = viewModel::setIsApp,
        onLoad = viewModel::load,
        onFindByName = viewModel::findByName,
        onFilter = viewModel::setFilter,
        onToggle = viewModel::toggle,
        onToggleAll = viewModel::toggleAll,
    )
}

@Composable
private fun PicsExplorerContent(
    state: PicsExplorerUiState,
    onBack: () -> Unit,
    onIsApp: (Boolean) -> Unit,
    onLoad: (Int) -> Unit,
    onFindByName: (String) -> Unit,
    onFilter: (String) -> Unit,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var text by rememberSaveable { mutableStateOf("") }

    // What the tree is searched for. A newly loaded tree starts out unsearched.
    var filter by rememberSaveable { mutableStateOf("") }
    val load: (Int) -> Unit = { id ->
        filter = ""
        onLoad(id)
    }

    UnderHoodScaffold(
        title = "PICS explorer",
        onBack = onBack,
        actions = {
            // A searched tree has every match opened up, so there is nothing to open or close.
            if (state.nodes.isNotEmpty() && filter.isBlank()) {
                val anyOpen = state.nodes.any { it.expanded }
                IconButton(onClick = onToggleAll) {
                    Icon(
                        imageVector = if (anyOpen) {
                            Icons.Outlined.UnfoldLess
                        } else {
                            Icons.Outlined.UnfoldMore
                        },
                        contentDescription = if (anyOpen) "Close all" else "Open all",
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            SearchField(
                value = text,
                onValueChange = { text = it },
                placeholder = if (state.isApp) "App ID or name" else "Package ID, or a game in it",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                // A number is an ID and goes to Steam; anything else is a name, looked up in
                // the app.
                onSearch = {
                    keyboard?.hide()
                    searchByIdOrName(text, onId = load, onName = onFindByName)
                },
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.isApp,
                    onClick = { onIsApp(true) },
                    label = { Text("App") },
                )
                FilterChip(
                    selected = !state.isApp,
                    onClick = { onIsApp(false) },
                    label = { Text("Package") },
                )
            }

            when {
                state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())

                state.nameMatches.isNotEmpty() -> NameMatchList(
                    matches = state.nameMatches,
                    scaffoldPadding = padding,
                    onPick = { match ->
                        text = match.id.toString()
                        load(match.id)
                    },
                )

                state.title == null -> EmptyMessage(
                    message = state.message
                        ?: (
                            "Enter the ID of any app or package to see what Steam's catalogue " +
                                "holds on it, or a game's name to find its ID"
                            ),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                ) {
                    item(key = "header") {
                        Column(
                            modifier = Modifier.padding(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                            DetailList(state.facts)
                            TreeFilter(
                                filter = filter,
                                matches = state.filterMatches,
                                onFilter = {
                                    filter = it
                                    onFilter(it)
                                },
                            )
                        }
                    }
                    items(state.nodes, key = { it.path }) { node ->
                        NodeRow(node = node, onToggle = { onToggle(node.path) })
                    }
                }
            }
        }
    }
}

/** Searches the keys and values of the tree as it is typed; the tree is on the phone already. */
@Composable
private fun TreeFilter(filter: String, matches: Int, onFilter: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SearchField(
            value = filter,
            onValueChange = onFilter,
            placeholder = "Find a key or value in the tree",
        )
        Hint(
            if (filter.isBlank()) {
                "Tap a value to copy it."
            } else {
                "$matches matching key(s). Tap a value to copy it."
            },
        )
    }
}

/** One key of the tree. A key that holds others opens and closes; a value is copied. */
@Composable
private fun NodeRow(node: PicsNode, onToggle: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isBranch = node.children > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isBranch) {
                    onToggle()
                } else {
                    context.copyToClipboard(label = node.key, text = node.value.orEmpty())
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
            .padding(start = (node.depth * INDENT).dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isBranch) {
            Icon(
                imageVector = if (node.expanded) {
                    Icons.Outlined.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight
                },
                contentDescription = if (node.expanded) "Close" else "Open",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Spacer(modifier = Modifier.width(18.dp))
        }

        Text(
            text = node.key,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (isBranch) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Text(
            text = node.value ?: "${node.children} inside",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (isBranch) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/** Width of one level of the tree, in dp. */
private const val INDENT = 14

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        PicsExplorerContent(
            state = PicsExplorerUiState(
                title = "Team Fortress 2 · App 440",
                facts = persistentListOf(
                    "Change number" to "28123456",
                    "Access token" to "None",
                    "Keys" to "1204",
                ),
                nodes = persistentListOf(
                    PicsNode("/0", 0, "appid", "440", 0),
                    PicsNode("/1", 0, "common", null, 3, expanded = true),
                    PicsNode("/1/0", 1, "name", "Team Fortress 2", 0),
                    PicsNode("/1/1", 1, "type", "Game", 0),
                    PicsNode("/1/2", 1, "oslist", "windows,macos,linux", 0),
                    PicsNode("/2", 0, "depots", null, 14),
                ),
            ),
            onBack = {},
            onIsApp = {},
            onLoad = {},
            onFindByName = {},
            onFilter = {},
            onToggle = {},
            onToggleAll = {},
        )
    }
}
