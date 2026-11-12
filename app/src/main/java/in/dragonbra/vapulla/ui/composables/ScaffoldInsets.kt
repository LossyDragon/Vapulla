package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space to leave above the last row so a floating action button never covers it. */
val FabClearance = 88.dp

/**
 * The scaffold padding minus its bottom inset. Apply this to a list's container and put the
 * inset into [listContentPadding] instead, so rows scroll under the gesture bar edge to edge
 * while the last one can still be scrolled clear of it.
 */
@Composable
fun PaddingValues.exceptBottom(): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(direction),
        top = calculateTopPadding(),
        end = calculateEndPadding(direction),
    )
}

/** Content padding for a list whose container used [exceptBottom]; [extraBottom] is for FABs. */
fun listContentPadding(
    scaffoldPadding: PaddingValues,
    horizontal: Dp = 16.dp,
    vertical: Dp = 8.dp,
    extraBottom: Dp = 0.dp,
): PaddingValues = PaddingValues(
    start = horizontal,
    top = vertical,
    end = horizontal,
    bottom = vertical + extraBottom + scaffoldPadding.calculateBottomPadding(),
)
