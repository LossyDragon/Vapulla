package `in`.dragonbra.vapulla.compose.ui.theme

import androidx.compose.ui.graphics.Color
import `in`.dragonbra.vapulla.adapter.FriendListItem

/* Friend Status Colors */
val friendAwayOrSnooze = Color(0x806DCFF6)
val friendInGame = Color(0xFF90BA3C)
val friendInGameAwayOrSnooze = Color(0x8090BA3C)
val friendOffline = Color(0xFF7A7A7A)
val friendOnline = Color(0xFF6DCFF6)

/* Jetpack Compose Colors */
val colorPrimary = Color(0xFF212121)
val colorSecondary = Color(0xFF546E7A)
val colorError = Color(0xFFEF5350)

fun getStatusColor(friend: FriendListItem?): Color {
    if (friend == null) {
        return friendOffline
    }

    return when {
        friend.isOffline() -> friendOffline
        friend.isInGameAwayOrSnooze() -> friendInGameAwayOrSnooze
        friend.isAwayOrSnooze() -> friendAwayOrSnooze
        friend.isInGame() -> friendInGame
        friend.isOnline() -> friendOnline
        else -> friendOffline
    }
}
