package `in`.dragonbra.vapulla.ui.screens.family

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.Family
import `in`.dragonbra.vapulla.service.api.FamilyMember
import `in`.dragonbra.vapulla.service.api.SharedGame
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.MenuButton
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.SkeletonList
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.toDateString
import `in`.dragonbra.vapulla.util.helpers.toSteamIdString
import kotlinx.collections.immutable.persistentListOf

/**
 * The account's Steam Family: its members, and the games it shares with the account. It
 * only shows; nothing here changes the family.
 */
@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    onNavDrawerAction: () -> Unit,
    onOpenGame: (appId: Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FamilyContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onOpenGame = onOpenGame,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FamilyContent(
    uiState: FamilyUiState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onOpenGame: (appId: Int) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val family = uiState.family

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(family?.name?.ifEmpty { null } ?: "Family") },
                navigationIcon = { MenuButton(onClick = onNavDrawerAction) },
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
                family == null && uiState.isLoading ->
                    SkeletonList(contentPadding = listContentPadding(padding))

                family == null -> RefreshableMessage(message = uiState.message.orEmpty())

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "members-header") {
                        Header(
                            title = "Members",
                            detail = "${family.members.size} of " +
                                "${family.members.size + family.freeSpots}",
                        )
                    }
                    itemsIndexed(
                        items = family.members,
                        key = { _, member -> "m${member.steamId}" },
                    ) { index, member ->
                        MemberRow(
                            member = member,
                            index = index,
                            count = family.members.size,
                            onClick = { uriHandler.openUri(Utils.getProfileUrl(member.steamId)) },
                        )
                    }

                    item(key = "games-header") {
                        Header(title = "Shared with you", detail = "${uiState.games.size} games")
                    }
                    itemsIndexed(
                        items = uiState.games,
                        key = { _, game -> "g${game.appId}" },
                    ) { index, game ->
                        GameRow(
                            game = game,
                            owners = ownerNames(game, family),
                            index = index,
                            count = uiState.games.size,
                            onClick = { onOpenGame(game.appId) },
                        )
                    }
                }
            }
        }
    }
}

/** The names of the members who own [game], for the line under its name. */
private fun ownerNames(game: SharedGame, family: Family): String {
    val names = family.members.associate { it.steamId to it.name }
    return game.owners.mapNotNull { names[it]?.ifEmpty { null } }.joinToString()
}

@Composable
private fun Header(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One member; tapping opens their Steam profile. */
@Composable
private fun MemberRow(member: FamilyMember, index: Int, count: Int, onClick: () -> Unit) {
    GroupedListItem(index = index, count = count, onClick = onClick) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    // A member Steam would not name is shown by Steam ID.
                    text = member.name.ifEmpty { member.steamId.toSteamIdString() },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (member.joinedAt > 0) {
                    Text(
                        text = "Joined ${member.joinedAt.toDateString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (member.isSelf) {
                InfoPill(
                    text = "You",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            InfoPill(text = member.role)
        }
    }
}

/** One shared game and who owns it; tapping opens its store page. */
@Composable
private fun GameRow(
    game: SharedGame,
    owners: String,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val name = remember(game.name) { game.name.decodeHtml() }

    GroupedListItem(index = index, count = count, onClick = onClick) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name.ifEmpty { "App ${game.appId}" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (owners.isNotEmpty()) {
                Text(
                    text = "From $owners",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        FamilyContent(
            uiState = FamilyUiState(
                family = Family(
                    id = 1,
                    name = "The Dragons",
                    freeSpots = 3,
                    members = persistentListOf(
                        FamilyMember(1, "Lossy", "Adult", 1_710_000_000, isSelf = true),
                        FamilyMember(2, "Ann", "Adult", 1_711_000_000, isSelf = false),
                        FamilyMember(3, "", "Child", 1_712_000_000, isSelf = false),
                    ),
                ),
                games = persistentListOf(
                    SharedGame(620, "Portal 2", persistentListOf(2L)),
                    SharedGame(440, "Team Fortress 2", persistentListOf(2L, 3L)),
                ),
                isLoading = false,
            ),
            onNavDrawerAction = {},
            onRefresh = {},
            onOpenGame = {},
        )
    }
}
