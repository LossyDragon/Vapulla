package `in`.dragonbra.vapulla.compose.util

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Color.darkenBy(percent: Float): Color {
    val hsl = FloatArray(3)
    AndroidColor.RGBToHSV(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        hsl
    )

    hsl[2] *= (1f - percent)

    return Color(AndroidColor.HSVToColor(hsl))
}
