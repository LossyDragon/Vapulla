package `in`.dragonbra.vapulla.ui.composables.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A centred one-line message for empty or error states. Pass a `fillMaxSize()` modifier with
 * the scaffold padding to centre it on a whole screen; by default it sits at the top.
 */
@Composable
fun SearchResultMessage(message: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
