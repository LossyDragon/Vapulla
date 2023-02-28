package `in`.dragonbra.vapulla.compose.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* Jetpack Compose Shapes */
val Shapes = Shapes()

val ChatBubbleFriendShape = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomEnd = 2.dp,
    bottomStart = 10.dp
)

val ChatBubbleMeShape = RoundedCornerShape(
    topStart = 10.dp,
    topEnd = 10.dp,
    bottomEnd = 10.dp,
    bottomStart = 2.dp
)
