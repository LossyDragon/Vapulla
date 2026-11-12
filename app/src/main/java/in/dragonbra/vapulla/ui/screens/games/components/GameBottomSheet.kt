package `in`.dragonbra.vapulla.ui.screens.games.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.SteamApp
import kotlinx.collections.immutable.ImmutableSet

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameBottomSheet(
    sheetState: SheetState,
    appTypes: ImmutableSet<SteamApp.AppType>,
    onDismissRequest: () -> Unit,
    onAppTypeClicked: (SteamApp.AppType) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Filter by type",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Pick one or more. With nothing picked, only games are shown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SteamApp.AppType.entries
                    .filter { it != SteamApp.AppType.invalid }
                    .forEach { category ->
                        val selected = appTypes.contains(category)
                        FilterChip(
                            selected = selected,
                            onClick = { onAppTypeClicked(category) },
                            label = { Text(category.name.capitalize()) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else {
                                null
                            },
                        )
                    }
            }
        }
    }
}
