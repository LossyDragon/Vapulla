package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import kotlin.collections.component1
import kotlin.collections.component2

@Composable
fun FriendList(
    modifier: Modifier,
    friends: Map<String, List<SteamFriend>>,
    stickyHeaders: Set<String>,
    onStickyHeaderAction: (String) -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        content = {
            friends.forEach { (k, v) ->
                stickyHeader {
                    FriendListHeader(
                        isCollapsed = k in stickyHeaders,
                        header = k,
                        count = v.size,
                        onHeaderAction = { onStickyHeaderAction(k) },
                    )
                }

                if (k !in stickyHeaders) {
                    itemsIndexed(v, key = { _, item -> item.id }) { idx, friend ->
                        FriendListItem(
                            modifier = Modifier
                                .animateItem()
                                .fillParentMaxWidth()
                                .combinedClickable(
                                    onClick = { onFriendClick(friend.id) },
                                    onLongClick = { onFriendLongClick(friend.id) }
                                ),
                            friend = friend,
                        )

                        if (idx < v.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    )
}