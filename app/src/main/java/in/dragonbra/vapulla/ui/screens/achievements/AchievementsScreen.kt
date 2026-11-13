package `in`.dragonbra.vapulla.ui.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.service.api.Achievement
import `in`.dragonbra.vapulla.service.api.GameStat
import `in`.dragonbra.vapulla.service.api.GameStats
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.SkeletonList
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toDateString
import kotlinx.collections.immutable.persistentListOf

@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AchievementsContent(uiState = uiState, onBack = onBack, onRefresh = viewModel::refresh)
}

@Composable
private fun AchievementsContent(
    uiState: AchievementsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Achievements & Stats")
                        if (uiState.gameName.isNotEmpty()) {
                            Text(
                                text = uiState.gameName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = { BackButton(onClick = onBack) },
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
            val stats = uiState.stats

            when {
                uiState.isLoading && stats == null ->
                    SkeletonList(contentPadding = listContentPadding(padding))

                uiState.error != null -> RefreshableMessage(message = uiState.error)

                stats == null || (stats.achievements.isEmpty() && stats.stats.isEmpty()) ->
                    RefreshableMessage(message = "This game has no achievements or stats")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (stats.achievements.isNotEmpty()) {
                        item(key = "summary") { Summary(stats) }

                        itemsIndexed(
                            items = stats.achievements,
                            key = { _, achievement -> "achievement:${achievement.name}" },
                        ) { index, achievement ->
                            AchievementRow(
                                achievement = achievement,
                                shape = groupedListShape(index, stats.achievements.size),
                            )
                        }
                    }

                    if (stats.stats.isNotEmpty()) {
                        item(key = "stats") { SectionTitle("Stats") }

                        itemsIndexed(
                            items = stats.stats,
                            key = { index, stat -> "stat:$index:${stat.name}" },
                        ) { index, stat ->
                            StatRow(stat = stat, shape = groupedListShape(index, stats.stats.size))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(stats: GameStats) {
    val unlocked = stats.achievements.count { it.unlocked }
    val total = stats.achievements.size

    Column(
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "$unlocked of $total unlocked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearWavyProgressIndicator(
            progress = { unlocked / total.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Suppress("SameParameterValue")
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun AchievementRow(achievement: Achievement, shape: Shape) {
    // A hidden achievement keeps its secret until it is earned, as on Steam.
    val concealed = achievement.hidden && !achievement.unlocked

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoilImage(
                imageModel = { achievement.iconUrl },
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .alpha(if (achievement.unlocked) 1f else 0.6f),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                failure = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null) },
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (concealed) "Hidden achievement" else achievement.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val description = if (concealed) {
                    "Details are revealed once unlocked"
                } else {
                    achievement.description
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val progress = achievement.progress
                val progressMax = achievement.progressMax
                if (!achievement.unlocked && progress != null && progressMax != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { progress / progressMax },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "%,.0f / %,.0f".format(progress, progressMax),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (achievement.unlocked) {
                    Text(
                        text = achievement.unlockTime.toDateString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                achievement.globalPercent?.let { percent ->
                    Text(
                        text = "%.1f%% of players".format(percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(stat: GameStat, shape: Shape) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stat.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        AchievementsContent(
            uiState = AchievementsUiState(
                gameName = "Team Fortress 2",
                isLoading = false,
                stats = GameStats(
                    achievements = persistentListOf(
                        Achievement(
                            name = "TF_PLAY_GAME_EVERYCLASS",
                            title = "Head of the Class",
                            description = "Play a complete round with every class.",
                            iconUrl = "",
                            unlocked = true,
                            unlockTime = 1_300_000_000,
                            hidden = false,
                            globalPercent = 41.3f,
                            progress = null,
                            progressMax = null,
                        ),
                        Achievement(
                            name = "TF_GET_HEALPOINTS",
                            title = "Team Doctor",
                            description = "Accumulate 25000 heal points as a Medic.",
                            iconUrl = "",
                            unlocked = false,
                            unlockTime = 0,
                            hidden = false,
                            globalPercent = 12.9f,
                            progress = 8_200f,
                            progressMax = 25_000f,
                        ),
                    ),
                    stats = persistentListOf(GameStat("Scout: kills", "1,204")),
                ),
            ),
            onBack = {},
            onRefresh = {},
        )
    }
}
