package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.model.FriendListItem

@Composable
fun StickyHeaderItem(isCollapsed: Boolean, header: String, count: Int, onHeaderAction: () -> Unit) {
    val headerText = remember(header, count) { "$header ($count)" }
    ListItem(
        headlineContent = { Text(text = headerText) },
        trailingContent = {
            val button = when (isCollapsed) {
                true -> Icons.Outlined.KeyboardArrowDown
                else -> Icons.Outlined.KeyboardArrowUp
            }
            IconButton(onClick = onHeaderAction) {
                Icon(imageVector = button, contentDescription = null)
            }
        }
    )
}

@Preview
@Composable
private fun Preview_StickyHeaderItem() {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }.diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(1.0)
                .build()
        }.build()

    val friend = FriendListItem(
        id = 0,
        state = EPersonaState.Online.code(),
        avatar = null,
        gameAppId = 440,
        gameName = "Team Fortess 2",
        lastLogOff = 0L,
        lastLogOn = 0L,
        lastMessage = null,
        lastMessageTime = null,
        name = "Name The Game",
        newMessageCount = null,
        nickname = null,
        relation = 0,
        stateFlags = 0,
        typingTs = 0L
    )
    VapullaTheme {
        Column {
            StickyHeaderItem(true, "Online", 60, {})
            FriendItem(
                imageLoader = imageLoader,
                friend = friend,
                onClickChat = {},
                onClickProfile = {}
            )
        }
    }
}
