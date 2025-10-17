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
    friendsList: Map<Int, List<SteamFriend>>,
    stickyHeaders: Set<Int>,
    onStickyHeaderAction: (Int) -> Unit,
    onFriendClick: (Long) -> Unit,
    onFriendLongClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        content = {
            friendsList.forEach { (header, friends) ->
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

                        if (idx < friends.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    )
}