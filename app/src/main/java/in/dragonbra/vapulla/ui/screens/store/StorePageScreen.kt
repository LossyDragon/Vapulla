package `in`.dragonbra.vapulla.ui.screens.store

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.StorePage
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.IconLabel
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.ShimmerBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.games.components.HEADER_ASPECT
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toDateString
import kotlinx.collections.immutable.persistentListOf

/** The store's screenshots are 16 by 9. */
private const val SCREENSHOT_RATIO = 16f / 9f
private val SCREENSHOT_HEIGHT = 150.dp

/**
 * What the Steam store shows for one app: price, reviews, description, tags and
 * screenshots. An owned app links on to its page in the library.
 */
@Composable
fun StorePageScreen(
    viewModel: StorePageViewModel,
    onBack: () -> Unit,
    onOpenLibrary: (appId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StorePageContent(uiState = uiState, onBack = onBack, onOpenLibrary = onOpenLibrary)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorePageContent(
    uiState: StorePageUiState,
    onBack: () -> Unit,
    onOpenLibrary: (appId: Int) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val page = uiState.page

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = page?.name ?: "Store",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
    ) { padding ->
        if (page == null) {
            if (uiState.isLoading) {
                LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
            } else {
                EmptyMessage(
                    message = uiState.message.orEmpty(),
                    modifier = Modifier.padding(padding),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
            contentPadding = listContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (page.headerUrl.isNotEmpty()) {
                item {
                    RemoteImage(
                        imageModel = { page.headerUrl },
                        imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(HEADER_ASPECT)
                            .clip(MaterialTheme.shapes.extraLarge),
                        loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
                    )
                }
            }

            item { Price(page) }

            if (page.description.isNotEmpty()) {
                item { Text(text = page.description, style = MaterialTheme.typography.bodyMedium) }
            }

            item {
                Actions(
                    inLibrary = uiState.inLibrary,
                    onStore = { uriHandler.openUri(page.storeUrl) },
                    onLibrary = { onOpenLibrary(page.appId) },
                )
            }

            item { Section(title = "Details") { DetailList(details(page)) } }

            if (page.tags.isNotEmpty()) {
                item { Section(title = "Tags") { Tags(page.tags) } }
            }

            if (page.screenshots.isNotEmpty()) {
                item {
                    Section(title = "Screenshots") {
                        Screenshots(urls = page.screenshots, onOpen = uriHandler::openUri)
                    }
                }
            }
        }
    }
}

/** What it costs: the price, and when it is on sale the discount and the price before it. */
@Composable
private fun Price(page: StorePage) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                page.isFree -> "Free"
                page.price.isNotEmpty() -> page.price
                page.isComingSoon -> "Coming soon"
                else -> "Not for sale"
            },
            style = MaterialTheme.typography.headlineSmall,
        )

        if (page.discountPercent > 0) {
            if (page.originalPrice.isNotEmpty()) {
                Text(
                    text = page.originalPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            }
            InfoPill(
                text = "-${page.discountPercent}%",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        if (page.isEarlyAccess) InfoPill(text = "Early Access")
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Actions(inLibrary: Boolean, onStore: () -> Unit, onLibrary: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onStore,
            modifier = Modifier.weight(1f),
            shapes = ButtonDefaults.shapes(),
        ) {
            IconLabel(Icons.Outlined.Storefront, "Store website")
        }
        if (inLibrary) {
            FilledTonalButton(
                onClick = onLibrary,
                modifier = Modifier.weight(1f),
                shapes = ButtonDefaults.shapes(),
            ) {
                IconLabel(Icons.Outlined.Games, "In your library")
            }
        }
    }
}

@Composable
private fun Tags(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { InfoPill(text = it) }
    }
}

/** The page's screenshots, side by side; tapping one opens it full size in the browser. */
@Composable
private fun Screenshots(urls: List<String>, onOpen: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(urls, key = { it }) { url ->
            RemoteImage(
                imageModel = { url },
                imageOptions = ImageOptions(
                    contentDescription = "Screenshot",
                    contentScale = ContentScale.Crop,
                ),
                modifier = Modifier
                    .height(SCREENSHOT_HEIGHT)
                    .aspectRatio(SCREENSHOT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onOpen(url) },
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
            )
        }
    }
}

/** The rows of the Details section; ones the store has nothing for come out empty. */
private fun details(page: StorePage): List<Pair<String, String>> {
    val reviews = if (page.reviewLabel.isEmpty()) {
        ""
    } else {
        // The count is formatted on its own: a "%" in the text around it would be read as
        // part of the format.
        "${page.reviewLabel}, ${page.reviewPercent}% of ${"%,d".format(page.reviewCount)}"
    }
    val platforms = listOfNotNull(
        "Windows".takeIf { page.windows },
        "macOS".takeIf { page.mac },
        "Linux".takeIf { page.linux },
    ).joinToString()

    return listOf(
        "Reviews" to reviews,
        "Released" to when {
            page.isComingSoon -> "Coming soon"
            page.releaseDate > 0 -> page.releaseDate.toDateString()
            else -> ""
        },
        "Developer" to page.developers,
        "Publisher" to page.publishers,
        "Platforms" to platforms,
        "App ID" to page.appId.toString(),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        StorePageContent(
            uiState = StorePageUiState(
                page = StorePage(
                    appId = 620,
                    name = "Portal 2",
                    description = "The \"Perpetual Testing Initiative\" has been expanded to " +
                        "allow you to design co-op puzzles for you and your friends!",
                    headerUrl = "",
                    developers = "Valve",
                    publishers = "Valve",
                    isFree = false,
                    price = "$1.99",
                    originalPrice = "$9.99",
                    discountPercent = 80,
                    reviewLabel = "Overwhelmingly Positive",
                    reviewPercent = 98,
                    reviewCount = 392_971,
                    releaseDate = 1_303_186_800,
                    isComingSoon = false,
                    isEarlyAccess = false,
                    windows = true,
                    mac = false,
                    linux = true,
                    tags = persistentListOf("Platformer", "Puzzle", "Co-op", "First-Person"),
                    screenshots = persistentListOf(),
                    storeUrl = "https://store.steampowered.com/app/620/Portal_2",
                ),
                isLoading = false,
                inLibrary = true,
            ),
            onBack = {},
            onOpenLibrary = {},
        )
    }
}
