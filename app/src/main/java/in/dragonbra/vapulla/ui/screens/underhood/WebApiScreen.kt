package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.service.api.WebApiDirectory
import `in`.dragonbra.vapulla.service.api.WebApiInterface
import `in`.dragonbra.vapulla.service.api.WebApiMethod
import `in`.dragonbra.vapulla.service.api.WebApiParameter
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * @property interfaces the ones matching [query], each with only its matching methods.
 * @property total how many interfaces Steam listed in all.
 */
@Immutable
data class WebApiUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val interfaces: ImmutableList<WebApiInterface> = persistentListOf(),
    val total: Int = 0,
    val message: String? = null,
)

/** Lists what Steam's Web API offers, as Steam itself describes it. */
class WebApiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WebApiUiState())
    val uiState: StateFlow<WebApiUiState> = _uiState.asStateFlow()

    private var all: List<WebApiInterface> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { WebApiDirectory.load() }
                .onSuccess {
                    all = it
                    _uiState.value = WebApiUiState(
                        isLoading = false,
                        interfaces = it.toImmutableList(),
                        total = it.size,
                    )
                }
                .onFailure {
                    Timber.w(it, "GetSupportedAPIList failed")
                    _uiState.value = WebApiUiState(
                        isLoading = false,
                        message = "Could not reach the Steam Web API",
                    )
                }
        }
    }

    /** Narrows the list to interfaces and methods whose name contains [text]. */
    fun search(text: String) {
        val query = text.trim()
        val matches = if (query.isEmpty()) {
            all
        } else {
            all.mapNotNull { api ->
                val methods = api.methods.filter { it.name.contains(query, ignoreCase = true) }
                when {
                    api.name.contains(query, ignoreCase = true) -> api
                    methods.isNotEmpty() -> api.copy(methods = methods)
                    else -> null
                }
            }
        }
        _uiState.update { it.copy(query = text, interfaces = matches.toImmutableList()) }
    }
}

@Composable
fun WebApiScreen(viewModel: WebApiViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WebApiContent(state = state, onBack = onBack, onSearch = viewModel::search)
}

@Composable
private fun WebApiContent(state: WebApiUiState, onBack: () -> Unit, onSearch: (String) -> Unit) {
    // Interfaces opened by hand. A search opens everything it finds.
    var opened by remember { mutableStateOf(setOf<String>()) }

    UnderHoodScaffold(title = "Web API", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Interface or method, e.g. News") },
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

            when {
                state.isLoading -> LoadingBox(modifier = Modifier.fillMaxSize())

                state.message != null -> SearchResultMessage(message = state.message)

                state.interfaces.isEmpty() -> SearchResultMessage(message = "Nothing by that name")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "note") {
                        Text(
                            text = "${state.total} interfaces at api.steampowered.com that " +
                                "anyone may call. A publisher key would list more.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "reference") {
                        ReferenceLink(modifier = Modifier.padding(bottom = 8.dp))
                    }
                    itemsIndexed(state.interfaces, key = { _, api -> api.name }) { index, api ->
                        InterfaceBlock(
                            api = api,
                            expanded = state.query.isNotBlank() || api.name in opened,
                            shape = groupedListShape(index, state.interfaces.size),
                            onToggle = {
                                opened = if (api.name in opened) {
                                    opened - api.name
                                } else {
                                    opened + api.name
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Points to xPaw's Web API reference, which does more than this list: it also has the methods
 * Steam leaves out of its own listing, and calls a method with a key or token you give it.
 */
@Composable
private fun ReferenceLink(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(REFERENCE_URL) },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "steamapi.xpaw.me",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "The better place to explore the Web API. It also lists the " +
                        "methods Steam leaves out here, and can call a method with your " +
                        "own key and show the answer.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in browser")
        }
    }
}

private const val REFERENCE_URL = "https://steamapi.xpaw.me/"

@Composable
private fun InterfaceBlock(
    api: WebApiInterface,
    expanded: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onToggle: () -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column {
                Text(
                    text = api.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${api.methods.size} method(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                api.methods.forEach { MethodBlock(it) }
            }
        }
    }
}

@Composable
private fun MethodBlock(method: WebApiMethod) {
    Column {
        Text(
            text = "${method.httpMethod} ${method.name}/v${method.version}",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
        method.parameters.forEach { parameter ->
            val optional = if (parameter.optional) ", optional" else ""
            Text(
                text = "  ${parameter.name} (${parameter.type}$optional) ${parameter.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        WebApiContent(
            state = WebApiUiState(
                isLoading = false,
                query = "news",
                interfaces = persistentListOf(
                    WebApiInterface(
                        name = "ISteamNews",
                        methods = listOf(
                            WebApiMethod(
                                name = "GetNewsForApp",
                                version = 2,
                                parameters = listOf(
                                    WebApiParameter("appid", "uint32", false, "AppID for the news"),
                                    WebApiParameter("count", "uint32", true, "# of posts to get"),
                                ),
                            ),
                        ),
                    ),
                ),
                total = 42,
            ),
            onBack = {},
            onSearch = {},
        )
    }
}
