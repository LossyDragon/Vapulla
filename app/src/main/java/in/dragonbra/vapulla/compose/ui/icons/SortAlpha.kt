@file:Suppress("UnusedReceiverParameter")

package `in`.dragonbra.vapulla.compose.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val Icons.SortAlpha: ImageVector
    get() {
        if (_sortAlpha != null) {
            return _sortAlpha!!
        }
        _sortAlpha = materialIcon(name = "SortAlpha") {
            materialPath {
                moveTo(19.0F, 17.0F)
                horizontalLineTo(22.0F)
                lineTo(18.0F, 21.0F)
                lineTo(14.0F, 17.0F)
                horizontalLineTo(17.0F)
                verticalLineTo(3.0F)
                horizontalLineTo(19.0F)
                moveTo(11.0F, 13.0F)
                verticalLineTo(15.0F)
                lineTo(7.67F, 19.0F)
                horizontalLineTo(11.0F)
                verticalLineTo(21.0F)
                horizontalLineTo(5.0F)
                verticalLineTo(19.0F)
                lineTo(8.33F, 15.0F)
                horizontalLineTo(5.0F)
                verticalLineTo(13.0F)
                moveTo(9.0F, 3.0F)
                horizontalLineTo(7.0F)
                curveTo(5.9F, 3.0F, 5.0F, 3.9F, 5.0F, 5.0F)
                verticalLineTo(11.0F)
                horizontalLineTo(7.0F)
                verticalLineTo(9.0F)
                horizontalLineTo(9.0F)
                verticalLineTo(11.0F)
                horizontalLineTo(11.0F)
                verticalLineTo(5.0F)
                curveTo(11.0F, 3.9F, 10.11F, 3.0F, 9.0F, 3.0F)
                moveTo(9.0F, 7.0F)
                horizontalLineTo(7.0F)
                verticalLineTo(5.0F)
                horizontalLineTo(9.0F)
                close()
            }
        }
        return _sortAlpha!!
    }

private var _sortAlpha: ImageVector? = null

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconSortAlphaPreview() {
    Image(modifier = Modifier.size(64.dp), imageVector = Icons.SortAlpha, contentDescription = null)
}
