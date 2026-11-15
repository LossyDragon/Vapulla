package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/** Rows from the top within which a list still follows new rows. */
private const val FOLLOW_THRESHOLD = 3

/** True once the list is scrolled further down than [FollowNewestEffect] follows new rows. */
val LazyListState.isScrolledAway: Boolean
    get() = firstVisibleItemIndex > FOLLOW_THRESHOLD

/**
 * Keeps a newest-first list at its top while rows are added. Keyed rows stay put when new
 * ones are inserted above them, so the list would otherwise quietly drift down. A list the
 * user has scrolled away from the top is left where it is.
 *
 * @param keys what changes when the rows do, such as the ID of the newest one.
 */
@Composable
fun FollowNewestEffect(listState: LazyListState, vararg keys: Any?) {
    LaunchedEffect(*keys) {
        if (!listState.isScrolledAway && !listState.isScrollInProgress) {
            listState.scrollToItem(0)
        }
    }
}
