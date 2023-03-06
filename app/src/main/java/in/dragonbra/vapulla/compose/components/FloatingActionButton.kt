package `in`.dragonbra.vapulla.compose.components

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

private enum class Visibility {
    Visible,
    Gone
}

@Composable
fun ScrollBackUp(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClicked: () -> Unit
) {
    val transition = updateTransition(
        if (enabled) Visibility.Visible else Visibility.Gone,
        label = "ScrollBackUp Transition"
    )

    val bottomOffset by transition.animateDp(label = "ScrollBackUp offset") {
        if (it == Visibility.Gone) (-24).dp else 24.dp
    }

    if (bottomOffset > 0.dp) {
        ExtendedFloatingActionButton(
            modifier = modifier
                .offset(x = 0.dp, y = -bottomOffset)
                .height(36.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    modifier = Modifier.height(18.dp),
                    contentDescription = null
                )
            },
            onClick = onClicked,
            text = { Text(text = "Scroll Up") }
        )
    }
}

@Preview
@Composable
private fun Preview_ScrollBackUp() {
    VapullaTheme {
        Box(Modifier.height(100.dp)) {
            ScrollBackUp(
                modifier = Modifier.align(Alignment.BottomCenter),
                enabled = true,
                onClicked = {}
            )
        }
    }
}
