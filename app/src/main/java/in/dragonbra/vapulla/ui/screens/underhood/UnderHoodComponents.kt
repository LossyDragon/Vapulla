package `in`.dragonbra.vapulla.ui.screens.underhood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.GroupedListItem
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import kotlinx.collections.immutable.ImmutableList

/*
 * What several of the Under the Hood screens share.
 */

/**
 * Shared frame of the Under the Hood detail screens: a title, a back button, and a body that
 * is handed the scaffold padding. A screen may add actions and a floating button.
 */
@Composable
internal fun UnderHoodScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton(onClick = onBack) },
                actions = { actions() },
            )
        },
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

/**
 * An app or a package found by name, to pick from.
 *
 * @property detail what makes it the match: an app's ID, or the games in a package.
 */
@Immutable
data class PicsNameMatch(val id: Int, val title: String, val detail: String)

/** How many apps a search by name looks at. */
private const val NAME_MATCHES = 100

/** The apps the app has stored whose name contains [name], each as a match to pick. */
internal suspend fun SteamAppDao.findAppsByName(name: String): List<PicsNameMatch> =
    searchNames(name, NAME_MATCHES).map { PicsNameMatch(it.id, it.name, "App ${it.id}") }

/**
 * What was typed into a field that takes an ID or a name: a number goes to [onId], anything
 * else to [onName], and nothing happens while it is empty.
 */
internal fun searchByIdOrName(text: String, onId: (Int) -> Unit, onName: (String) -> Unit) {
    val entered = text.trim()
    val id = entered.toIntOrNull()

    when {
        entered.isEmpty() -> Unit
        id != null -> onId(id)
        else -> onName(entered)
    }
}

/** The apps or packages a search by name found; tapping one picks it. */
@Composable
internal fun NameMatchList(
    matches: ImmutableList<PicsNameMatch>,
    scaffoldPadding: PaddingValues,
    onPick: (PicsNameMatch) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = listContentPadding(scaffoldPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(items = matches, key = { _, match -> match.id }) { index, match ->
            GroupedListItem(index = index, count = matches.size, onClick = { onPick(match) }) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = match.title.decodeHtml(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = match.detail.decodeHtml(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
