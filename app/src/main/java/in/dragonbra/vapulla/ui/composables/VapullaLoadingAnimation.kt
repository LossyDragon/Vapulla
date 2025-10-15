package `in`.dragonbra.vapulla.ui.composables

import android.content.res.Configuration
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import kotlinx.coroutines.delay

/**
 * [VapullaLoadingAnimation] was AI assisted to port animation from xml -> compose.
 */
@Composable
fun VapullaLoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    durationMillis: Int = 1000,
    isAnimating: Boolean = true
) {
    var middleAnimationStarted by remember { mutableStateOf(false) }
    var bottomAnimationStarted by remember { mutableStateOf(false) }

    val middleProgress by animateFloatAsState(
        targetValue = if (middleAnimationStarted && isAnimating) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = BounceEasing
        ),
        finishedListener = {
            if (isAnimating) {
                middleAnimationStarted = !middleAnimationStarted
            }
        },
        label = "middleProgress"
    )

    val bottomProgress by animateFloatAsState(
        targetValue = if (bottomAnimationStarted && isAnimating) 1f else 0f,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = BounceEasing
        ),
        finishedListener = {
            if (isAnimating) {
                bottomAnimationStarted = !bottomAnimationStarted
            }
        },
        label = "bottomProgress"
    )

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            middleAnimationStarted = true
            delay(300)
            bottomAnimationStarted = true
        } else {
            // Reset to initial state when stopped
            middleAnimationStarted = false
            bottomAnimationStarted = false
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.toPx()
            val scale = canvasSize / 1000f

            // Bottom sheet (animated) - #546e7a (blue-gray)
            val bottomPath = createBottomPath(bottomProgress, scale)
            drawPath(
                path = bottomPath,
                color = Color(0xFF546E7A)
            )

            // Middle sheet (animated) - #707070 (darker gray)
            val middlePath = createMiddlePath(middleProgress, scale)
            drawPath(
                path = middlePath,
                color = Color(0xFF707070)
            )

            // Top sheet (static) - #9e9e9e (lighter gray)
            val topPath = createTopPath(scale)
            drawPath(
                path = topPath,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

// Custom bounce easing to match Android's bounce interpolator
private val BounceEasing = Easing { fraction ->
    if (fraction < 0.36363637f) {
        7.5625f * fraction * fraction
    } else if (fraction < 0.72727275f) {
        val f = fraction - 0.54545456f
        7.5625f * f * f + 0.75f
    } else if (fraction < 0.90909094f) {
        val f = fraction - 0.8181818f
        7.5625f * f * f + 0.9375f
    } else {
        val f = fraction - 0.95454544f
        7.5625f * f * f + 0.984375f
    }
}

private fun createTopPath(scale: Float): Path {
    return Path().apply {
        moveTo(335f * scale, 669f * scale)
        lineTo(500f * scale, 1000f * scale)
        lineTo(1000f * scale, 0f)
        lineTo(669f * scale, 0f)
        close()
    }
}

private fun createMiddlePath(progress: Float, scale: Float): Path {
    val x1 = lerp(331f, 406f, progress)
    val y1 = lerp(0f, 150f, progress)
    val x2 = lerp(166f, 241f, progress)
    val y2 = lerp(331f, 481f, progress)

    return Path().apply {
        moveTo(666f * scale, 669f * scale)
        lineTo(x1 * scale, y1 * scale)
        lineTo(x2 * scale, y2 * scale)
        lineTo(500f * scale, 1000f * scale)
        close()
    }
}

private fun createBottomPath(progress: Float, scale: Float): Path {
    val x1 = lerp(331f, 481f, progress)
    val y1 = lerp(0f, 300f, progress)
    val hX = lerp(0f, 150f, progress)
    val lX = lerp(500f, 350f, progress)
    val lY = lerp(1000f, 700f, progress)

    return Path().apply {
        moveTo(666f * scale, 669f * scale)
        lineTo(x1 * scale, y1 * scale)
        lineTo(hX * scale, y1 * scale)
        relativeLineTo(lX * scale, lY * scale)
        close()
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        var isLoading by remember { mutableStateOf(true) }
        Surface {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                VapullaLoadingAnimation(isAnimating = isLoading)

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = { isLoading = !isLoading }) {
                    Text(if (isLoading) "Stop Animation" else "Start Animation")
                }
            }
        }
    }
}