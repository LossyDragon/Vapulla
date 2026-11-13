package `in`.dragonbra.vapulla.ui.screens.home.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.mock.mockFriendsList
import `in`.dragonbra.vapulla.ui.mock.mockStickyHeaders
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
) {
    val listState = rememberLazyListState()

    // Right after login every friend starts offline and the online sections are inserted
    // above as presence arrives. The lazy list keeps its anchor item, which would leave the
    // user staring at the Offline section, so stay pinned to the top while already there.
    var wasAtTop by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { wasAtTop = it }
    }
    LaunchedEffect(friendsList.keys.firstOrNull()) {
        if (wasAtTop) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((header, friends) in friendsList.entries) {
            stickyHeader(key = "header-$header") {
                FriendListHeader(
                    isCollapsed = header in stickyHeaders,
                    header = header,
                    count = friends.size,
                    onHeaderAction = { onStickyHeaderAction(header) },
                )
            }

            if (header !in stickyHeaders) {
                itemsIndexed(friends, key = { _, item -> item.id }) { idx, friend ->
                    val shape = groupedListShape(idx, friends.size)
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
                            .fillMaxWidth()
                            .clip(shape),
                        endActions = listOf(swipeToProfile),
                        content = {
                            FriendListItem(
                                friend = friend,
                                shape = shape,
                                onClick = {
                                    if (friend.isBlocked) {
                                        onFriendLongClick(friend.id)
                                    } else {
                                        onFriendClick(friend.id)
                                    }
                                },
                                onLongClick = { onFriendLongClick(friend.id) },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            FriendList(
                modifier = Modifier.fillMaxSize(),
                friendsList = mockFriendsList,
                stickyHeaders = mockStickyHeaders,
                onStickyHeaderAction = {},
                onFriendClick = {},
                onFriendLongClick = {},
            )
        }
    }
}
