package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamAppName
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchField
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.helpers.toSteamIdString
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A branch of an app: `public`, or a beta the developer has set up. */
@Immutable
data class BranchRow(
    val name: String,
    val buildId: Long,
    val updated: Long,
    val passwordRequired: Boolean,
)

/** One build of a depot: the manifest a branch points at. [encrypted] hides its real ID. */
@Immutable
data class ManifestRow(
    val branch: String,
    val gid: Long,
    val size: Long,
    val download: Long,
    val encrypted: Boolean,
)

/**
 * One depot of an app.
 *
 * @property traits what the depot is for: platform, architecture, language and so on.
 * @property owned a license on this account covers the depot, so it could be downloaded.
 */
@Immutable
data class DepotRow(
    val depotId: Int,
    val traits: ImmutableList<String>,
    val owned: Boolean,
    val manifests: ImmutableList<ManifestRow>,
)

/** The depots and branches of one app, ready to show. */
@Immutable
data class AppDepots(
    val appId: Int,
    val name: String,
    val fresh: Boolean,
    val branches: ImmutableList<BranchRow>,
    val depots: ImmutableList<DepotRow>,
)

@Immutable
data class DepotsUiState(
    val suggestions: ImmutableList<SteamAppName> = persistentListOf(),
    val isLoading: Boolean = false,
    val app: AppDepots? = null,
    val message: String? = null,
)

/**
 * Looks up an app's depot layout in PICS: the `depots` section of its app info, which is
 * what the Steam client and the depot downloader read to decide what to install.
 *
 * @param initialAppId an app to show straight away, or 0 to start with the search box.
 */
class DepotsViewModel(
    initialAppId: Int,
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val connection: SteamServiceConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DepotsUiState())
    val uiState: StateFlow<DepotsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        if (initialAppId > 0) {
            load(initialAppId)
        }
    }

    /** Offers known apps whose name contains [text], to save looking up an app ID. */
    fun search(text: String) {
        val query = text.trim()

        // The newest text wins: an older search still running must not answer after it.
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val matches = if (query.length < MIN_QUERY || query.toIntOrNull() != null) {
                emptyList()
            } else {
                appDao.searchNames(query, MAX_SUGGESTIONS)
            }
            _uiState.update { it.copy(suggestions = matches.toImmutableList()) }
        }
    }

    fun load(appId: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = DepotsUiState(isLoading = true)

            // Current data from Steam where possible; the stored row is what the library last saw.
            val fresh = connection.request("App info for $appId") { it.library.appInfo(appId) }
                .getOrNull()
            val app = fresh ?: withContext(Dispatchers.IO) { appDao.findApp(appId) }

            _uiState.value = when {
                app == null -> DepotsUiState(message = "Steam has no app info for $appId")

                app.depots.isEmpty() && app.branches.isEmpty() ->
                    DepotsUiState(message = "${app.label(appId)} has no depots Steam will show")

                else -> DepotsUiState(app = app.toRows(appId, fresh != null, ownedDepots()))
            }
        }
    }

    private suspend fun ownedDepots(): Set<Int> = withContext(Dispatchers.IO) {
        licenseDao.ownedDepotIds()
    }

    private fun SteamApp.label(appId: Int) = name.ifEmpty { "App $appId" }

    private fun SteamApp.toRows(appId: Int, fresh: Boolean, owned: Set<Int>) = AppDepots(
        appId = appId,
        name = label(appId),
        fresh = fresh,
        branches = branches.values
            // The branch everyone gets first, then the newest builds.
            .sortedWith(compareBy({ it.name != "public" }, { -it.timeUpdated.time }))
            .map { BranchRow(it.name, it.buildId, it.timeUpdated.time, it.pwdRequired) }
            .toImmutableList(),
        depots = depots.values.sortedBy { it.depotId }.map { depot ->
            DepotRow(
                depotId = depot.depotId,
                traits = depot.traits().toImmutableList(),
                owned = depot.depotId in owned,
                manifests = (
                    depot.manifests.values.map { it.toRow(encrypted = false) } +
                        depot.encryptedManifests.values.map { it.toRow(encrypted = true) }
                    )
                    .sortedBy { it.branch != "public" }
                    .toImmutableList(),
            )
        }.toImmutableList(),
    )

    private fun SteamApp.ManifestInfo.toRow(encrypted: Boolean) =
        ManifestRow(branch = name, gid = gid, size = size, download = download, encrypted)

    private fun SteamApp.DepotInfo.traits(): List<String> = buildList {
        val systems = osList.filter { it != SteamApp.OS.none }
        add(if (systems.isEmpty()) "Any OS" else systems.joinToString { it.name })

        if (osArch != SteamApp.OSArch.Unknown) add("${osArch.keyValName}-bit")
        if (language.isNotEmpty()) add(language)
        if (lowViolence) add("Low violence")
        if (dlcAppId != Int.MAX_VALUE) add("DLC $dlcAppId")
        if (depotFromApp != Int.MAX_VALUE) add("Shared from app $depotFromApp")
        if (sharedInstall) add("Shared install")
    }

    private companion object {
        const val MIN_QUERY = 2
        const val MAX_SUGGESTIONS = 8
    }
}

@Composable
fun DepotsScreen(
    viewModel: DepotsViewModel,
    onBack: () -> Unit,
    onOpenManifest: (appId: Int, depotId: Int, manifestId: Long, branch: String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DepotsContent(
        state = state,
        onBack = onBack,
        onSearch = viewModel::search,
        onLoad = viewModel::load,
        onOpenManifest = onOpenManifest,
    )
}

@Composable
private fun DepotsContent(
    state: DepotsUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onLoad: (Int) -> Unit,
    onOpenManifest: (appId: Int, depotId: Int, manifestId: Long, branch: String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var text by rememberSaveable { mutableStateOf("") }

    val open: (Int) -> Unit = { appId ->
        keyboard?.hide()
        onSearch("")
        onLoad(appId)
    }

    UnderHoodScaffold(title = "Depots", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            SearchField(
                value = text,
                onValueChange = {
                    text = it
                    onSearch(it)
                },
                placeholder = "App ID, or the name of a game you know",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                // Only an ID can be searched for; a name is picked from the suggestions.
                onSearch = { text.trim().toIntOrNull()?.let(open) },
            )

            when {
                state.suggestions.isNotEmpty() -> Suggestions(
                    apps = state.suggestions,
                    onPick = { app ->
                        text = app.name.decodeHtml()
                        open(app.id)
                    },
                )

                state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())

                state.app != null -> DepotList(
                    app = state.app,
                    bottomPadding = padding,
                    onOpenManifest = { depotId, manifest ->
                        onOpenManifest(state.app.appId, depotId, manifest.gid, manifest.branch)
                    },
                )

                else -> EmptyMessage(
                    message = state.message
                        ?: "Enter an app ID and search, or start typing a game's name",
                )
            }
        }
    }
}

@Composable
private fun Suggestions(apps: ImmutableList<SteamAppName>, onPick: (SteamAppName) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(apps, key = { _, app -> app.id }) { index, app ->
            GroupedListItem(index = index, count = apps.size, onClick = { onPick(app) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = app.name.decodeHtml(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "App ${app.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DepotList(
    app: AppDepots,
    bottomPadding: PaddingValues,
    onOpenManifest: (depotId: Int, ManifestRow) -> Unit,
) {
    val dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(bottomPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = app.name, style = MaterialTheme.typography.titleLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    InfoPill(text = "App ${app.appId}")
                    InfoPill(text = "${app.depots.size} depots")
                    InfoPill(text = "${app.branches.size} branches")
                    InfoPill(text = if (app.fresh) "From Steam just now" else "Stored copy")
                }
            }
        }

        if (app.branches.isNotEmpty()) {
            item(key = "branches-title") { Heading("Branches") }

            itemsIndexed(app.branches, key = { _, branch -> "b:${branch.name}" }) { index, branch ->
                Block(index = index, count = app.branches.size) {
                    Text(
                        text = branch.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Mono("Build ${branch.buildId}")
                    Faint(
                        if (branch.updated > 0) {
                            "Updated ${dates.format(branch.updated)}"
                        } else {
                            "No update time"
                        } + if (branch.passwordRequired) " · needs a password" else "",
                    )
                }
            }
        }

        if (app.depots.isNotEmpty()) {
            item(key = "depots-title") { Heading("Depots") }

            itemsIndexed(app.depots, key = { _, depot -> "d:${depot.depotId}" }) { index, depot ->
                Block(index = index, count = app.depots.size) {
                    Text(
                        text = "Depot ${depot.depotId}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        depot.traits.forEach { InfoPill(text = it) }
                        if (depot.owned) {
                            InfoPill(
                                text = "Licensed",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    if (depot.manifests.isEmpty()) {
                        Faint("No manifest: nothing of its own to download")
                    }
                    depot.manifests.forEach { manifest ->
                        // An encrypted manifest belongs to a branch behind a password, and
                        // cannot be fetched without it.
                        val canOpen = !manifest.encrypted && manifest.gid != 0L
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canOpen) {
                                    onOpenManifest(depot.depotId, manifest)
                                },
                        ) {
                            Mono(
                                "${manifest.branch}: ${manifest.gid.toSteamIdString()}" +
                                    if (manifest.encrypted) " (encrypted)" else "",
                            )
                            if (manifest.size > 0) {
                                Faint(
                                    "${manifest.size.toFileSize()} on disk · " +
                                        "${manifest.download.toFileSize()} to download",
                                )
                            }
                            if (canOpen) {
                                Text(
                                    text = "List files",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
    )
}

/** One branch or depot: a row of its grouped list, with its lines spaced evenly. */
@Composable
private fun Block(index: Int, count: Int, content: @Composable () -> Unit) {
    GroupedListItem(index = index, count = count) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

/** An ID worth copying: monospaced and selectable. */
@Composable
private fun Mono(text: String) {
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** A line of small grey text inside a [Block]; a `Hint` would indent it past the lines above. */
@Composable
private fun Faint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        DepotsContent(
            state = DepotsUiState(
                app = AppDepots(
                    appId = 1052990,
                    name = "A Monster's Expedition",
                    fresh = true,
                    branches = persistentListOf(
                        BranchRow("public", 15_123_456, 1_760_000_000_000, false),
                        BranchRow("beta", 15_200_001, 1_761_000_000_000, true),
                    ),
                    depots = persistentListOf(
                        DepotRow(
                            depotId = 1052991,
                            traits = persistentListOf("windows", "64-bit"),
                            owned = true,
                            manifests = persistentListOf(
                                ManifestRow("public", 7_123_456_789, 560_900, 358_600, false),
                                ManifestRow("beta", 8_123_456_789_012_345, 0, 0, true),
                            ),
                        ),
                        DepotRow(
                            depotId = 228_989,
                            traits = persistentListOf("Any OS", "Shared from app 228980"),
                            owned = false,
                            manifests = persistentListOf(),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onSearch = {},
            onLoad = {},
            onOpenManifest = { _, _, _, _ -> },
        )
    }
}
