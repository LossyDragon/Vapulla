package `in`.dragonbra.vapulla.ui.screens.games.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.minutesToPlaytime
import java.time.Instant
import java.time.ZoneId

/**
 * One row of the games list, styled as a Material 3 Expressive grouped list item: a tonal
 * container whose [shape] the caller picks by position, store header art, and info pills
 * instead of labelled text lines.
 */
@Composable
fun GameListItem(
    app: SteamAppListItem,
    onGameClicked: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    localAccountId: Long? = null,
) {
    val isShared = !app.ownerAccountId.contains(localAccountId?.toInt())
    val byline = listOf(app.developer, app.publisher)
        .map { it.decodeHtml().trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString(" · ")

    Surface(
        onClick = onGameClicked,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderArt(app = app)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = app.name.decodeHtml(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (byline.isNotEmpty()) {
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (app.type != SteamApp.AppType.game) {
                        InfoPill(
                            text = app.type.name.uppercase(),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }

                    if (app.releaseDate != 0L) {
                        InfoPill(
                            text = releaseYear(app.releaseDate),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }

                    if (app.playtimeForever > 0) {
                        InfoPill(
                            text = app.playtimeForever.minutesToPlaytime(),
                            icon = Icons.Outlined.Schedule,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (app.metacriticScore > 0) {
                        val (container, content) = metacriticColors(app.metacriticScore.toInt())
                        InfoPill(
                            text = "${app.metacriticScore} Metacritic",
                            containerColor = container,
                            contentColor = content,
                        )
                    }

                    if (isShared) {
                        InfoPill(
                            text = "Shared",
                            icon = Icons.Outlined.Groups,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeaderArt(app: SteamAppListItem) {
    CoilImage(
        imageModel = { app.headerImageUrl },
        modifier = Modifier
            .width(116.dp)
            .aspectRatio(460f / 215f)
            .clip(MaterialTheme.shapes.medium),
        imageOptions = ImageOptions(contentScale = ContentScale.Crop),
        previewPlaceholder = painterResource(R.drawable.vapulla_background),
        loading = {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
                content = { LoadingIndicator(modifier = Modifier.size(28.dp)) },
            )
        },
        failure = {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Failed to load image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun metacriticColors(score: Int): Pair<Color, Color> = when {
    score >= 75 ->
        MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer

    score >= 50 ->
        MaterialTheme.colorScheme.tertiaryContainer to
            MaterialTheme.colorScheme.onTertiaryContainer

    else -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

private fun releaseYear(epochSeconds: Long): String =
    Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()).year.toString()

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            GameListItem(
                app = SteamAppListItem(
                    id = 440,
                    name = "Team Fortress 2",
                    type = SteamApp.AppType.game,
                    releaseDate = 1191999600,
                    metacriticScore = 92,
                    developer = "Valve",
                    publisher = "Valve",
                ),
                onGameClicked = {},
            )
            GameListItem(
                app = SteamAppListItem(
                    id = 570,
                    name = "Dota 2 Workshop Tools",
                    type = SteamApp.AppType.tool,
                    developer = "Valve",
                    publisher = "Valve",
                    ownerAccountId = listOf(1),
                ),
                onGameClicked = {},
                localAccountId = 2,
            )
        }
    }
}
