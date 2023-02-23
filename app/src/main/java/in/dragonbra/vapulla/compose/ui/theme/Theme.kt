package `in`.dragonbra.vapulla.compose.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val darkColorScheme = darkColorScheme(
    primary = colorPrimary,
    secondary = colorSecondary,
    error = colorError
)

@Composable
fun VapullaTheme(
    /* darkTheme: Boolean = true, // isSystemInDarkTheme() */
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(color = colorPrimary)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = darkColorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme,
        shapes = Shapes,
        content = content
    )
}
