package `in`.dragonbra.vapulla.ui.screens.chatrooms

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.service.api.ChatGroup
import `in`.dragonbra.vapulla.service.api.ChatRoomInfo
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

@Composable
fun ChatRoomsScreen(
    viewModel: ChatRoomsViewModel,
    onNavDrawerAction: () -> Unit,
    onRoomClick: (ChatGroup, ChatRoomInfo) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatRoomsContent(
        uiState = uiState,
        onNavDrawerAction = onNavDrawerAction,
        onRefresh = viewModel::refresh,
        onToggle = viewModel::toggleExpanded,
        onRoomClick = onRoomClick,
    )
}

@Composable
private fun ChatRoomsContent(
    uiState: ChatRoomsUiState,
    onNavDrawerAction: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (Long) -> Unit,
    onRoomClick: (ChatGroup, ChatRoomInfo) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Chats") },
                navigationIcon = {
                    IconButton(onClick = onNavDrawerAction) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation")
                    }
                },
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
                uiState.isLoading && uiState.groups.isEmpty() -> Unit

                // the indicator is the loading state
                uiState.error != null -> RefreshableMessage(message = uiState.error)

                uiState.groups.isEmpty() -> RefreshableMessage(
                    message = "You are not in any group chats",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(uiState.groups, key = { _, group ->
                        group.groupId
                    }) { index, group ->
                        GroupRow(
                            group = group,
                            expanded = group.groupId in uiState.expanded,
                            shape = groupedListShape(index, uiState.groups.size),
                            onClick = { onToggle(group.groupId) },
                            onRoomClick = { room -> onRoomClick(group, room) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: ChatGroup,
    expanded: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    onRoomClick: (ChatRoomInfo) -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoilImage(
                    imageModel = { group.avatarUrl },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.large),
                    imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                    failure = { Icon(Icons.Outlined.Groups, contentDescription = null) },
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (group.tagline.isNotBlank()) {
                        Text(
                            text = group.tagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                InfoPill(text = "${group.activeMembers} Members", icon = Icons.Outlined.Groups)
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    group.rooms.forEach { room ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onRoomClick(room) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                // A group's first channel has no name of its own; Steam calls it Home.
                                Text(
                                    text = room.name.ifEmpty { "Home" },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (room.lastMessage.isNotBlank()) {
                                    Text(
                                        text = room.lastMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (room.timeLastMessage > 0) {
                                Text(
                                    text = (room.timeLastMessage * 1000).toTimeAgo(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun group(id: Long, name: String, tagline: String, members: Int) = ChatGroup(
        groupId = id,
        name = name,
        tagline = tagline,
        avatarUrl = "",
        activeMembers = members,
        clanId = 0,
        appId = 0,
        rooms = persistentListOf(
            ChatRoomInfo(
                chatId = id * 10,
                name = "",
                lastMessage = "Anyone up for a match?",
                timeLastMessage = 1_760_722_982,
            ),
            ChatRoomInfo(
                chatId = id * 10 + 1,
                name = "off-topic",
                lastMessage = "",
                timeLastMessage = 0,
            ),
        ),
    )

    VapullaTheme {
        ChatRoomsContent(
            uiState = ChatRoomsUiState(
                groups = persistentListOf(
                    group(1, "JavaSteam", "Steam client library for the JVM", 24),
                    group(2, "Weekend Gamers", "", 7),
                ),
                expanded = persistentSetOf(1L),
                isLoading = false,
            ),
            onNavDrawerAction = {},
            onRefresh = {},
            onToggle = {},
            onRoomClick = { _, _ -> },
        )
    }
}
