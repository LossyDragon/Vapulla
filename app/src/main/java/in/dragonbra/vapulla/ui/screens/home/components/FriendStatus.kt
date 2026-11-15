package `in`.dragonbra.vapulla.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.icons.VR
import `in`.dragonbra.vapulla.ui.theme.friendAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendBlocked
import `in`.dragonbra.vapulla.ui.theme.friendInGame
import `in`.dragonbra.vapulla.ui.theme.friendInGameAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendOffline
import `in`.dragonbra.vapulla.ui.theme.friendOnline
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo

// How a friend's presence is worded, coloured and marked wherever a friend is shown. Kept
// here rather than on the entity so that the database layer knows nothing of the UI.

/** One line describing what the friend is doing, or when they were last seen. */
val SteamFriend.statusText: String
    get() = when {
        isBlocked -> "Blocked"

        isRequestRecipient -> "Wants to be your friend"

        isPlayingGame -> {
            val game = gameName.ifEmpty { "Playing game id: $gameAppID" }
            if (richPresenceStatus.isNotEmpty()) "$game · $richPresenceStatus" else game
        }

        isOffline -> if (lastLogOff.time > 0) {
            "Last online ${lastLogOff.time.toTimeAgo()}"
        } else {
            "Offline"
        }

        else -> state.name
    }

val SteamFriend.statusColor: Color
    get() = when {
        isBlocked -> friendBlocked
        isOffline -> friendOffline
        isInGameAwayOrSnooze -> friendInGameAwayOrSnooze
        isAwayOrSnooze -> friendAwayOrSnooze
        isPlayingGame -> friendInGame
        isOnline -> friendOnline
        else -> friendOffline
    }

/** The small mark after a friend's name: a pending request, away, or the kind of client. */
val SteamFriend.statusIcon: ImageVector?
    get() = when {
        isRequestRecipient -> Icons.Default.PersonAddAlt1
        isAwayOrSnooze -> Icons.Default.Bedtime
        stateFlags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
        stateFlags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
        stateFlags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
        stateFlags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
        else -> null
    }
