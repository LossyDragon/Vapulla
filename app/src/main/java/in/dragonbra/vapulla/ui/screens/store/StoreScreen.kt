package `in`.dragonbra.vapulla.ui.screens.store

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.StoreSearchResult
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.collections.immutable.persistentListOf

/** Room under the list so its last row can be scrolled clear of the search button. */
private val SEARCH_BUTTON_SPACE = 72.dp

/** A search result's picture is the store's small capsule, 231 by 87 pixels. */
private const val CAPSULE_RATIO = 231f / 87f

/**
 * Finds any app in the Steam store by name, owned or not, and opens its store page. Nothing
 * is looked up while typing, only when the search button or the keyboard's search key is
 * pressed.
 */
@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    onNavDrawerAction: () -> Unit,
    onOpen: (appId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StoreContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onSearch = viewModel::search,
        onOpen = onOpen,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreContent(
    uiState: StoreUiState,
    onNavDrawerAction: () -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (appId: Int) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(uiState.searched) }
    val keyboard = LocalSoftwareKeyboardController.current

    val search: () -> Unit = {
        onSearch(input)
        keyboard?.hide()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = search,
                // The scaffold already keeps the button clear of the navigation bar, so only
                // the rest of the keyboard's height is added.
                modifier = Modifier.windowInsetsPadding(
                    WindowInsets.ime.exclude(WindowInsets.navigationBars),
                ),
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search the store")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(padding.exceptBottom()),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("A game's name, e.g. Portal") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                input = ""
                                onSearch("")
                            },
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
            )

            when {
                uiState.isSearching -> LoadingBox(modifier = Modifier.fillMaxSize())

                uiState.message != null -> SearchResultMessage(message = uiState.message)

                uiState.searched.isEmpty() -> SearchResultMessage(
                    message = "Search the Steam store for any game, whether you own it or not",
                )

                uiState.results.isEmpty() ->
                    SearchResultMessage(message = "The store has nothing by that name")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(
                        scaffoldPadding = padding,
                        extraBottom = SEARCH_BUTTON_SPACE,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = uiState.results,
                        key = { _, result -> result.appId },
                    ) { index, result ->
                        ResultRow(
                            result = result,
                            shape = groupedListShape(index, uiState.results.size),
                            onClick = { onOpen(result.appId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: StoreSearchResult, shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                imageModel = { result.imageUrl },
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                modifier = Modifier
                    .width(108.dp)
                    .aspectRatio(CAPSULE_RATIO)
                    .clip(MaterialTheme.shapes.small),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = result.price,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (result.discountPercent > 0) {
                        Text(
                            text = result.originalPrice,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                        Text(
                            text = "-${result.discountPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
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
        StoreContent(
            uiState = StoreUiState(
                searched = "portal",
                results = persistentListOf(
                    StoreSearchResult(620, "Portal 2", imageUrl = "", price = "$9.99"),
                    StoreSearchResult(
                        appId = 400,
                        name = "Portal",
                        imageUrl = "",
                        price = "$1.99",
                        originalPrice = "$9.99",
                        discountPercent = 80,
                    ),
                    StoreSearchResult(440, "Team Fortress 2", imageUrl = "", price = "Free"),
                ),
            ),
            onNavDrawerAction = {},
            onSearch = {},
            onOpen = {},
        )
    }
}
