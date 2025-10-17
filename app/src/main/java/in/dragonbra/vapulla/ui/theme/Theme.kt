package `in`.dragonbra.vapulla.ui.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R

@SuppressLint("ObsoleteSdkInt")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VapullaTheme(
    seedColor: Color = colorPrimary,
    isDarkTheme: Boolean =  isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(primary = seedColor)
    val shapes = Shapes(largeIncreased = RoundedCornerShape(36.0.dp))
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        supportsDynamicColor && isDarkTheme -> {
            dynamicDarkColorScheme(LocalContext.current)
        }

        supportsDynamicColor && !isDarkTheme -> {
            dynamicLightColorScheme(LocalContext.current)
        }

        isDarkTheme -> darkColorScheme
        else -> expressiveLightColorScheme()
    }


    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}