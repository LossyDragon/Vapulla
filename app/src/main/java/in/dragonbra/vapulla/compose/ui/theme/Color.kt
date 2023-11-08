@file:Suppress("unused")

package `in`.dragonbra.vapulla.compose.ui.theme

import androidx.compose.ui.graphics.Color
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListItem

/* Friend Status Colors */
val friendAwayOrSnooze = Color(0x806DCFF6)
val friendInGame = Color(0xFF90BA3C)
val friendInGameAwayOrSnooze = Color(0x8090BA3C)
val friendOffline = Color(0xFF7A7A7A)
val friendOnline = Color(0xFF6DCFF6)

/* Jetpack Compose Colors */
val md_theme_dark_primary = Color(0xFF61D4FF)
val md_theme_dark_onPrimary = Color(0xFF003545)
val md_theme_dark_primaryContainer = Color(0xFF004D63)
val md_theme_dark_onPrimaryContainer = Color(0xFFBBE9FF)
val md_theme_dark_secondary = Color(0xFFB4CAD5)
val md_theme_dark_onSecondary = Color(0xFF1E333C)
val md_theme_dark_secondaryContainer = Color(0xFF354A53)
val md_theme_dark_onSecondaryContainer = Color(0xFFCFE6F2)
val md_theme_dark_tertiary = Color(0xFFC5C3EA)
val md_theme_dark_onTertiary = Color(0xFF2E2D4D)
val md_theme_dark_tertiaryContainer = Color(0xFF444364)
val md_theme_dark_onTertiaryContainer = Color(0xFFE2DFFF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF191C1E)
val md_theme_dark_onBackground = Color(0xFFE1E3E4)
val md_theme_dark_surface = Color(0xFF191C1E)
val md_theme_dark_onSurface = Color(0xFFE1E3E4)
val md_theme_dark_surfaceVariant = Color(0xFF40484C)
val md_theme_dark_onSurfaceVariant = Color(0xFFC0C8CC)
val md_theme_dark_outline = Color(0xFF8A9296)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1E)
val md_theme_dark_inverseSurface = Color(0xFFE1E3E4)
val md_theme_dark_inversePrimary = Color(0xFF006782)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFF61D4FF)
val md_theme_dark_outlineVariant = Color(0xFF40484C)
val md_theme_dark_scrim = Color(0xFF000000)

val seed = Color(0xFF546E7A)

fun getAccountStatusColor(state: EPersonaState): Color {
    return when (state) {
        EPersonaState.Busy,
        EPersonaState.Away,
        EPersonaState.Snooze -> friendAwayOrSnooze
        EPersonaState.Online -> friendOnline
        else -> friendOffline
    }
}

fun getStatusColor(friend: FriendListItem?): Color {
    if (friend == null) return friendOffline

    return when {
        friend.isOffline -> friendOffline
        friend.isInGameAwayOrSnooze -> friendInGameAwayOrSnooze
        friend.isAwayOrSnooze() -> friendAwayOrSnooze
        friend.isInGame -> friendInGame
        friend.isOnline -> friendOnline
        else -> friendOffline
    }
}
