package `in`.dragonbra.vapulla.ui.composables

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
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
