package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.javasteam.enums.EDepotFileFlag
import `in`.dragonbra.javasteam.types.DepotManifest
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import java.text.DateFormat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * One file of a manifest.
 *
 * @property chunks how many pieces, of about a megabyte each, the file is downloaded in.
 * @property flags what kind of entry it is, e.g. a folder or an executable; often empty.
 */
@Immutable
data class ManifestFile(val name: String, val size: Long, val chunks: Int, val flags: String)

/**
 * @property facts figures of the manifest as a whole.
 * @property files the ones matching [query], in path order.
 */
@Immutable
data class ManifestUiState(
    val isLoading: Boolean = true,
    val facts: ImmutableList<Pair<String, String>> = persistentListOf(),
    val query: String = "",
    val files: ImmutableList<ManifestFile> = persistentListOf(),
    val message: String? = null,
)

/**
 * Fetches one manifest of a depot and lists the files of that build. Only the manifest is
 * downloaded, a few hundred kilobytes even for a large game, never the files themselves.
 */
class ManifestViewModel(
    private val appId: Int,
    private val depotId: Int,
    private val manifestId: Long,
    private val branch: String,
    private val connection: ServiceConnection,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManifestUiState())
    val uiState: StateFlow<ManifestUiState> = _uiState.asStateFlow()

    private var all: List<ManifestFile> = emptyList()

    init {
        viewModelScope.launch {
            val api = connection.steamService?.apis?.manifests
            if (api == null) {
                _uiState.value =
                    ManifestUiState(isLoading = false, message = "Not connected to Steam")
                return@launch
            }

            runCatching { api.manifest(appId, depotId, manifestId, branch) }
                .onSuccess { show(it) }
                .onFailure {
                    Timber.w(it, "Manifest ${manifestId.toULong()} of depot $depotId failed")
                    _uiState.value = ManifestUiState(
                        isLoading = false,
                        message = it.message ?: "Steam did not serve the manifest",
                    )
                }
        }
    }

    private suspend fun show(manifest: DepotManifest) {
        // A large game has tens of thousands of files to sort.
        all = withContext(Dispatchers.Default) {
            manifest.files
                .map { file ->
                    ManifestFile(
                        name = file.fileName.replace('\\', '/'),
                        size = file.totalSize,
                        chunks = file.chunks.size,
                        flags = file.flags.joinToString { it.name },
                    )
                }
                .sortedBy { it.name.lowercase() }
        }

        val folders = manifest.files.count { EDepotFileFlag.Directory in it.flags }
        val dates = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

        _uiState.value = ManifestUiState(
            isLoading = false,
            facts = persistentListOf(
                "Depot" to depotId.toString(),
                "Manifest" to manifestId.toULong().toString(),
                "Branch" to branch,
                "Built" to dates.format(manifest.creationTime),
                "Files" to "%,d".format(all.size - folders),
                "Folders" to "%,d".format(folders),
                "Chunks" to "%,d".format(all.sumOf { it.chunks }),
                "On disk" to manifest.totalUncompressedSize.toFileSize(),
                "To download" to manifest.totalCompressedSize.toFileSize(),
            ),
            files = all.toImmutableList(),
        )
    }

    /** Narrows the list to files whose path contains [text]. */
    fun search(text: String) {
        val query = text.trim()
        val matches = if (query.isEmpty()) all else all.filter { it.name.contains(query, true) }
        _uiState.update { it.copy(query = text, files = matches.toImmutableList()) }
    }
}

@Composable
fun ManifestScreen(viewModel: ManifestViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ManifestContent(state = state, onBack = onBack, onSearch = viewModel::search)
}

@Composable
private fun ManifestContent(
    state: ManifestUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
) {
    UnderHoodScaffold(title = "Manifest", onBack = onBack) { padding ->
        when {
            state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))

            state.message != null -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                SearchResultMessage(message = state.message)
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(padding.exceptBottom()),
                contentPadding = listContentPadding(padding),
            ) {
                item(key = "facts") { DetailList(state.facts) }

                item(key = "search") {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        placeholder = { Text("Filter by path") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { onSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                }

                items(state.files, key = { it.name }) { file -> FileRow(file) }
            }
        }
    }
}

@Composable
private fun FileRow(file: ManifestFile) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = listOfNotNull(
                file.size.toFileSize(),
                "${file.chunks} chunk(s)",
                file.flags.takeIf { it.isNotEmpty() },
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        ManifestContent(
            state = ManifestUiState(
                isLoading = false,
                facts = persistentListOf(
                    "Depot" to "1052991",
                    "Manifest" to "7123456789012345678",
                    "Files" to "214",
                    "On disk" to "534.9 MB",
                ),
                files = persistentListOf(
                    ManifestFile("A Monster's Expedition.exe", 641_536, 1, "Executable"),
                    ManifestFile("A Monster's Expedition_Data", 0, 0, "Directory"),
                    ManifestFile("A Monster's Expedition_Data/level0", 48_211_004, 46, ""),
                    ManifestFile("UnityPlayer.dll", 21_791_232, 21, ""),
                ),
            ),
            onBack = {},
            onSearch = {},
        )
    }
}
