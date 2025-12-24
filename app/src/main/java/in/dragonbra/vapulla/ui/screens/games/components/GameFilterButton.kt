package `in`.dragonbra.vapulla.ui.screens.games.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun GameFilterButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = { Text(text = "Filter") },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}
