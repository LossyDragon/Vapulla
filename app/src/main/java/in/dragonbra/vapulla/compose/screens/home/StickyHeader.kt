package `in`.dragonbra.vapulla.compose.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.waterfallPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@Composable
fun StickyHeaderItem(headerGroupCount: Int) {
    Text(
        text = "($headerGroupCount)",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(5.dp)
            .waterfallPadding()
    )
}

@Preview
@Composable
private fun Preview_StickyHeaderItem() {
    VapullaTheme {
        Surface {
            StickyHeaderItem(60)
        }
    }
}