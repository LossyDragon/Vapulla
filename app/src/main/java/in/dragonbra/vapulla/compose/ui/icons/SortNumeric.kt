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

val Icons.SortNumeric: ImageVector
    get() {
        if (_sortNumeric != null) {
            return _sortNumeric!!
        }
        _sortNumeric = materialIcon(name = "SortNumeric") {
            materialPath {
                moveTo(19.0F, 17.0F)
                horizontalLineTo(22.0F)
                lineTo(18.0F, 21.0F)
                lineTo(14.0F, 17.0F)
                horizontalLineTo(17.0F)
                verticalLineTo(3.0F)
                horizontalLineTo(19.0F)
                verticalLineTo(17.0F)
                moveTo(9.0F, 13.0F)
                horizontalLineTo(7.0F)
                curveTo(5.9F, 13.0F, 5.0F, 13.9F, 5.0F, 15.0F)
                verticalLineTo(16.0F)
                curveTo(5.0F, 17.11F, 5.9F, 18.0F, 7.0F, 18.0F)
                horizontalLineTo(9.0F)
                verticalLineTo(19.0F)
                horizontalLineTo(5.0F)
                verticalLineTo(21.0F)
                horizontalLineTo(9.0F)
                curveTo(10.11F, 21.0F, 11.0F, 20.11F, 11.0F, 19.0F)
                verticalLineTo(15.0F)
                curveTo(11.0F, 13.9F, 10.11F, 13.0F, 9.0F, 13.0F)
                moveTo(9.0F, 16.0F)
                horizontalLineTo(7.0F)
                verticalLineTo(15.0F)
                horizontalLineTo(9.0F)
                verticalLineTo(16.0F)
                moveTo(9.0F, 3.0F)
                horizontalLineTo(7.0F)
                curveTo(5.9F, 3.0F, 5.0F, 3.9F, 5.0F, 5.0F)
                verticalLineTo(9.0F)
                curveTo(5.0F, 10.11F, 5.9F, 11.0F, 7.0F, 11.0F)
                horizontalLineTo(9.0F)
                curveTo(10.11F, 11.0F, 11.0F, 10.11F, 11.0F, 9.0F)
                verticalLineTo(5.0F)
                curveTo(11.0F, 3.9F, 10.11F, 3.0F, 9.0F, 3.0F)
                moveTo(9.0F, 9.0F)
                horizontalLineTo(7.0F)
                verticalLineTo(5.0F)
                horizontalLineTo(9.0F)
                verticalLineTo(9.0F)
                close()
            }
        }
        return _sortNumeric!!
    }

private var _sortNumeric: ImageVector? = null

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun IconSortNumericPreview() {
    Image(
        modifier = Modifier.size(64.dp),
        imageVector = Icons.SortNumeric,
        contentDescription = null
    )
}
