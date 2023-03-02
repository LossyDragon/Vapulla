package `in`.dragonbra.vapulla.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val darkColorScheme = darkColorScheme(
    primary = colorPrimary,
    secondary = colorSecondary,
    error = colorError
)

@Composable
fun VapullaTheme(
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isSystemInDarkTheme()
    SideEffect {
        systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = useDarkIcons)
    }

    MaterialTheme(
        colorScheme = darkColorScheme,
        shapes = Shapes,
        content = { Surface { content() } }
    )
}
