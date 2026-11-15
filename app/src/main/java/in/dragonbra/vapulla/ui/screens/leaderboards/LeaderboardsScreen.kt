package `in`.dragonbra.vapulla.ui.screens.leaderboards

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.Leaderboard
import `in`.dragonbra.vapulla.service.api.LeaderboardScore
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.composables.search.SearchResultMessage
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import kotlinx.collections.immutable.persistentListOf

/**
 * A game's leaderboards. Picking one shows the scores on it: the best of everyone, or those
 * of the account's friends. Back returns from the scores to the list before it leaves.
 */
@Composable
fun LeaderboardsScreen(viewModel: LeaderboardsViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LeaderboardsContent(
        uiState = uiState,
        onBack = onBack,
        onSelect = viewModel::select,
        onFriendsOnly = viewModel::setFriendsOnly,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaderboardsContent(
    uiState: LeaderboardsUiState,
    onBack: () -> Unit,
    onSelect: (Leaderboard?) -> Unit,
    onFriendsOnly: (Boolean) -> Unit,
) {
    val selected = uiState.selected

    BackHandler(enabled = selected != null) { onSelect(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selected?.name ?: "Leaderboards",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uiState.gameName.isNotEmpty()) {
                            Text(
                                text = uiState.gameName.decodeHtml(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    BackButton(onClick = { if (selected != null) onSelect(null) else onBack() })
                },
            )
        },
    ) { padding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding.exceptBottom())

        when {
            uiState.isLoading -> LoadingBox(modifier = modifier)

            uiState.message != null ->
                SearchResultMessage(message = uiState.message, modifier = modifier)

            selected == null -> LazyColumn(
                modifier = modifier,
                contentPadding = listContentPadding(padding),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(uiState.boards, key = { _, board -> board.id }) { index, board ->
                    BoardRow(
                        board = board,
                        shape = groupedListShape(index, uiState.boards.size),
                        onClick = { onSelect(board) },
                    )
                }
            }

            else -> Column(modifier = modifier) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !uiState.friendsOnly,
                        onClick = { onFriendsOnly(false) },
                        label = { Text("Everyone") },
                    )
                    FilterChip(
                        selected = uiState.friendsOnly,
                        onClick = { onFriendsOnly(true) },
                        label = { Text("Friends") },
                    )
                }

                Scores(uiState = uiState, bottomPadding = padding)
            }
        }
    }
}

@Composable
private fun Scores(
    uiState: LeaderboardsUiState,
    bottomPadding: androidx.compose.foundation.layout.PaddingValues,
) {
    val uriHandler = LocalUriHandler.current

    when {
        uiState.isLoadingScores -> LoadingBox(modifier = Modifier.fillMaxSize())

        uiState.scores.isEmpty() -> SearchResultMessage(
            message = if (uiState.friendsOnly) {
                "None of your friends has a score here"
            } else {
                "Steam returned no scores for this leaderboard"
            },
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = listContentPadding(bottomPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(uiState.scores, key = { _, score -> score.steamId }) { index, score ->
                ScoreRow(
                    score = score,
                    shape = groupedListShape(index, uiState.scores.size),
                    onClick = { uriHandler.openUri(Utils.getProfileUrl(score.steamId)) },
                )
            }
        }
    }
}

@Composable
private fun BoardRow(board: Leaderboard, shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = board.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%,d players".format(board.entries),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One score; tapping it opens the player's Steam profile. */
@Composable
private fun ScoreRow(score: LeaderboardScore, shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${score.rank}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 40.dp),
            )
            Text(
                // A player Steam would not name is shown by Steam ID.
                text = score.name.ifEmpty { score.steamId.toULong().toString() },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = score.score,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    val board = Leaderboard(
        id = 47840,
        name = "05 Catapults: Time",
        entries = 178_215,
        displayType = 3,
    )

    VapullaTheme {
        LeaderboardsContent(
            uiState = LeaderboardsUiState(
                gameName = "Portal 2",
                boards = persistentListOf(board),
                isLoading = false,
                selected = board,
                scores = persistentListOf(
                    LeaderboardScore(1, 76561197960287930, name = "Gabe", score = "0:41.250"),
                    LeaderboardScore(2, 76561197960287931, name = "", score = "0:42.010"),
                ),
            ),
            onBack = {},
            onSelect = {},
            onFriendsOnly = {},
        )
    }
}
