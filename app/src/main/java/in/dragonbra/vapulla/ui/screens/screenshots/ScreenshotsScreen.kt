package `in`.dragonbra.vapulla.ui.screens.screenshots

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.service.api.Screenshot
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.ShimmerBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toDateString
import kotlinx.collections.immutable.persistentListOf

/** Steam screenshots are mostly 16:9, so every tile is; other shapes are cropped to fit. */
private const val TILE_ASPECT = 16f / 9f

/** Tiles shown while the first page loads. */
private const val SKELETON_TILES = 8

@Composable
fun ScreenshotsScreen(viewModel: ScreenshotsViewModel, onNavDrawerAction: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenshotsContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore,
    )
}

@Composable
private fun ScreenshotsContent(
    uiState: ScreenshotsUiState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    var viewing by remember { mutableStateOf<Screenshot?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Screenshots") },
                    navigationIcon = {
                        IconButton(onClick = onNavDrawerAction) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                        }
                    },
                )
            },
        ) { padding ->
            PullToRefreshContent(
                isRefreshing = uiState.isLoading,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding.exceptBottom()),
            ) {
                when {
                    uiState.error != null -> RefreshableMessage(message = uiState.error)

                    uiState.items.isEmpty() && !uiState.isLoading -> RefreshableMessage(
                        message = "You have not uploaded any screenshots to Steam",
                    )

                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listContentPadding(padding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Placeholder tiles until the first page arrives.
                        if (uiState.items.isEmpty()) {
                            items(SKELETON_TILES) { ScreenshotTileSkeleton() }
                        }

                        items(uiState.items, key = { it.id }) { screenshot ->
                            ScreenshotTile(
                                screenshot = screenshot,
                                onClick = { viewing = screenshot },
                            )
                        }

                        if (uiState.nextPage != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    shapes = ButtonDefaults.shapes(),
                                ) {
                                    val shown = uiState.items.size
                                    Text(text = "Load more ($shown of ${uiState.total})")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drawn over the whole screen, system bars included, so nothing of the grid shows.
        viewing?.let { screenshot ->
            ScreenshotViewer(screenshot = screenshot, onDismiss = { viewing = null })
        }
    }
}

/** A thumbnail with the game it was taken in underneath. */
@Composable
private fun ScreenshotTile(screenshot: Screenshot, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CoilImage(
            imageModel = { screenshot.thumbnailUrl },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TILE_ASPECT)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClickLabel = "View screenshot", onClick = onClick),
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop,
                contentDescription = screenshot.caption.ifEmpty { "Screenshot" },
            ),
            previewPlaceholder = painterResource(R.drawable.vapulla_background),
            loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
        )
        Text(
            text = screenshot.appName.ifEmpty { "App ${screenshot.appId}" },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun ScreenshotTileSkeleton() {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(TILE_ASPECT),
        shape = MaterialTheme.shapes.large,
    )
}

/** The full-size image over a black backdrop, with where and when it was taken. */
@Composable
private fun ScreenshotViewer(screenshot: Screenshot, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Keeps taps from reaching the grid underneath.
            .pointerInput(Unit) {}
            .systemBarsPadding(),
    ) {
        CoilImage(
            imageModel = { screenshot.imageUrl },
            modifier = Modifier.fillMaxSize(),
            imageOptions = ImageOptions(
                contentScale = ContentScale.Fit,
                contentDescription = screenshot.caption.ifEmpty { "Screenshot" },
            ),
            previewPlaceholder = painterResource(R.drawable.vapulla_background),
            loading = { LoadingBox(modifier = Modifier.fillMaxSize()) },
        )

        FilledTonalIconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (screenshot.caption.isNotBlank()) {
                Text(
                    text = screenshot.caption,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
            }
            val size = if (screenshot.width > 0) {
                "${screenshot.width} × ${screenshot.height}"
            } else {
                ""
            }
            Text(
                text = listOf(screenshot.appName, screenshot.timeCreated.toDateString(), size)
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

private val previewScreenshots = persistentListOf(
    Screenshot(
        id = 1,
        appId = 440,
        appName = "Team Fortress 2",
        caption = "Payload pushed",
        imageUrl = "",
        thumbnailUrl = "",
        width = 1920,
        height = 1080,
        timeCreated = 1_760_722_982,
    ),
    Screenshot(
        id = 2,
        appId = 620,
        appName = "Portal 2",
        caption = "",
        imageUrl = "",
        thumbnailUrl = "",
        width = 2560,
        height = 1440,
        timeCreated = 1_750_000_000,
    ),
    Screenshot(
        id = 3,
        appId = 570,
        appName = "",
        caption = "",
        imageUrl = "",
        thumbnailUrl = "",
        width = 1920,
        height = 1080,
        timeCreated = 1_740_000_000,
    ),
)

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        ScreenshotsContent(
            uiState = ScreenshotsUiState(
                items = previewScreenshots,
                total = 42,
                nextPage = 2,
                isLoading = false,
            ),
            onNavDrawerAction = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_Loading() {
    VapullaTheme {
        ScreenshotsContent(
            uiState = ScreenshotsUiState(),
            onNavDrawerAction = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_Viewer() {
    VapullaTheme {
        ScreenshotViewer(screenshot = previewScreenshots.first(), onDismiss = {})
    }
}
