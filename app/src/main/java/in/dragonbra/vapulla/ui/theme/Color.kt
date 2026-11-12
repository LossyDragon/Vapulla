package `in`.dragonbra.vapulla.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Friend presence colours, following the Steam client's conventions but tuned a little
 * lighter and cooler so they sit on the grey [VapullaDarkColors] containers.
 */
val friendAwayOrSnooze = Color(0x807FCBEF)
val friendInGame = Color(0xFF9FD36A)
val friendInGameAwayOrSnooze = Color(0x809FD36A)
val friendOffline = Color(0xFF8A919A)
val friendOnline = Color(0xFF7FCBEF)
val friendBlocked = Color(0xFFE08383)

/**
 * The app's dark scheme: a cool dark-grey neutral ramp instead of Material's near-black, so
 * the grouped list containers and the floating toolbar still read as layers. The accent is
 * Steam's light blue; tertiary is a muted gold used for score and type pills.
 */
val VapullaDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF8FCBF6),
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF1F4C6B),
    onPrimaryContainer = Color(0xFFCCE8FF),
    inversePrimary = Color(0xFF2B6A95),

    secondary = Color(0xFFB9C5D3),
    onSecondary = Color(0xFF232E3A),
    secondaryContainer = Color(0xFF3A4654),
    onSecondaryContainer = Color(0xFFD9E3F0),

    tertiary = Color(0xFFDCBF8E),
    onTertiary = Color(0xFF3D2E0F),
    tertiaryContainer = Color(0xFF574525),
    onTertiaryContainer = Color(0xFFF9E1B8),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1D2025),
    onBackground = Color(0xFFE4E7EC),
    surface = Color(0xFF1D2025),
    onSurface = Color(0xFFE4E7EC),
    surfaceVariant = Color(0xFF3B4149),
    onSurfaceVariant = Color(0xFFC0C7D0),
    surfaceTint = Color(0xFF8FCBF6),

    surfaceDim = Color(0xFF15181C),
    surfaceBright = Color(0xFF3E444B),
    surfaceContainerLowest = Color(0xFF15181C),
    surfaceContainerLow = Color(0xFF23262C),
    surfaceContainer = Color(0xFF292D33),
    surfaceContainerHigh = Color(0xFF30353C),
    surfaceContainerHighest = Color(0xFF383E45),

    inverseSurface = Color(0xFFE4E7EC),
    inverseOnSurface = Color(0xFF2B2F35),
    outline = Color(0xFF8A919A),
    outlineVariant = Color(0xFF424850),
    scrim = Color(0xFF000000),
)
