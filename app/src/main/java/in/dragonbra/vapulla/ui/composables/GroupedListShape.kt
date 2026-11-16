package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val OuterCorner = 24.dp
private val InnerCorner = 6.dp

/**
 * Material 3 Expressive grouped-list shape for the item at [index] in a group of [count]:
 * large corners on the outer edges of the group, small corners between neighbours.
 */
fun groupedListShape(index: Int, count: Int): Shape = when {
    count == 1 -> RoundedCornerShape(OuterCorner)

    index == 0 -> RoundedCornerShape(
        topStart = OuterCorner,
        topEnd = OuterCorner,
        bottomStart = InnerCorner,
        bottomEnd = InnerCorner,
    )

    index == count - 1 -> RoundedCornerShape(
        topStart = InnerCorner,
        topEnd = InnerCorner,
        bottomStart = OuterCorner,
        bottomEnd = OuterCorner,
    )

    else -> RoundedCornerShape(InnerCorner)
}

/**
 * One row of a grouped list: a surface in the [groupedListShape] of the item at [index] in a
 * group of [count]. Put the rows in a column spaced 2 dp apart. With [onClick] the row can
 * be tapped, and the ripple stays inside its rounded corners.
 */
@Composable
fun GroupedListItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = groupedListShape(index, count)
    if (onClick != null) {
        // Material pads a clickable surface out to a 48 dp touch target. A short row would
        // get that as empty space above and below it, and the group would fall apart.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
            Surface(
                onClick = onClick,
                shape = shape,
                color = color,
                modifier = modifier.fillMaxWidth(),
                content = content,
            )
        }
    } else {
        Surface(shape = shape, color = color, modifier = modifier.fillMaxWidth(), content = content)
    }
}
