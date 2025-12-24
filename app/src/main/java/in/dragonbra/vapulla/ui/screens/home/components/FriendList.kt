package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import kotlin.collections.component1
import kotlin.collections.component2
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import timber.log.Timber

@Composable
fun FriendList(
    modifier: Modifier,
    friendsList: ImmutableMap<Int, ImmutableList<SteamFriend>>,
    stickyHeaders: ImmutableSet<Int>,
    onStickyHeaderAction: (Int) -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        content = {
            for ((header, friends) in friendsList.entries) {
                stickyHeader {
                    FriendListHeader(
                        isCollapsed = header in stickyHeaders,
                        header = header,
                        count = friends.size,
                        onHeaderAction = { onStickyHeaderAction(header) },
                    )
                }

                if (header !in stickyHeaders) {
                    itemsIndexed(friends, key = { _, item -> item.id }) { idx, friend ->
                        val swipeToProfile = SwipeAction(
                            icon = rememberVectorPainter(Icons.Default.ManageAccounts),
                            background = MaterialTheme.colorScheme.secondary,
                            onSwipe = {
                                Timber.d("Swipe to profile ${friend.id}")
                                onFriendLongClick(friend.id)
                            },
                        )

                        SwipeableActionsBox(
                            modifier = Modifier
                                .animateItem()
                                .fillParentMaxWidth()
                                .combinedClickable(
                                    onClick = { onFriendClick(friend.id) },
                                    onLongClick = { onFriendLongClick(friend.id) },
                                ),
                            endActions = listOf(swipeToProfile),
                            content = { FriendListItem(friend = friend) },
                        )

                        if (idx < friends.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
    )
}
