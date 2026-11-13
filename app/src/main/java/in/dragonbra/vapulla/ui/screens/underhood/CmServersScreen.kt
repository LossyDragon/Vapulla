package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.CmServer
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Immutable
data class CmServersUiState(
    val servers: ImmutableList<CmServer> = persistentListOf(),
    val isLoading: Boolean = true,
)

/** The client's CM server list (`SmartCMServerList`), with the server in use first. */
class CmServersViewModel(private val connection: ServiceConnection) : ViewModel() {

    private val _uiState = MutableStateFlow(CmServersUiState())
    val uiState: StateFlow<CmServersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // May go to the network: an empty list is fetched from Steam's directory first.
            val servers = withContext(Dispatchers.IO) {
                runCatching { connection.steamService?.cmServers().orEmpty() }
                    .onFailure { Timber.w(it, "Reading the CM server list failed") }
                    .getOrDefault(emptyList())
            }

            _uiState.value = CmServersUiState(
                servers = servers.sortedByDescending { it.current }.toImmutableList(),
                isLoading = false,
            )
        }
    }
}

@Composable
fun CmServersScreen(viewModel: CmServersViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CmServersContent(state = state, onBack = onBack, onRefresh = viewModel::refresh)
}

@Composable
private fun CmServersContent(state: CmServersUiState, onBack: () -> Unit, onRefresh: () -> Unit) {
    UnderHoodScaffold(
        title = "CM Servers",
        onBack = onBack,
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            state.servers.isEmpty() -> SearchResultMessage(
                message = "No servers: the client is not running",
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding.exceptBottom()),
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    InfoPill(text = "${state.servers.size} servers")
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = state.servers,
                        key = { _, server -> "${server.host}:${server.port}:${server.protocols}" },
                    ) { index, server ->
                        Surface(
                            shape = groupedListShape(index, state.servers.size),
                            color = if (server.current) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${server.host}:${server.port}",
                                        style = MaterialTheme.typography.bodyMedium
                                            .copy(fontFamily = FontFamily.Monospace),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = server.protocols,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (server.current) {
                                    InfoPill(
                                        text = "Connected",
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        CmServersContent(
            state = CmServersUiState(
                servers = persistentListOf(
                    CmServer("cmp1-ord1.steamserver.net", 443, "WEB_SOCKET", current = true),
                    CmServer("ext1-ord1.steamserver.net", 27017, "TCP, UDP", current = false),
                    CmServer("ext2-ord1.steamserver.net", 27018, "TCP, UDP", current = false),
                ),
                isLoading = false,
            ),
            onBack = {},
            onRefresh = {},
        )
    }
}
