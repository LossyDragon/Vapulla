package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun BottomSheetLayout(
    modifier: Modifier = Modifier,
    title: @Composable (() -> String)? = null,
    subtitle: @Composable (() -> AnnotatedString)? = null,
    hasSubtitlePadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        BottomSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))

        if (title != null) {
            BottomSheetHeader(
                text = title(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (subtitle != null) {
            BottomSheetSubtitle(
                text = subtitle(),
                modifier = Modifier.padding(bottom = if (hasSubtitlePadding) 16.dp else 0.dp)
            )
        }

        content()
    }
}
