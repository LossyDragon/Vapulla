package `in`.dragonbra.vapulla.ui.composables.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.ui.graphics.vector.ImageVector

enum class DialogType(val icon: ImageVector? = null) {
    NONE,

    // Friend
    FRIEND_BLOCK(Icons.Default.Block),
    FRIEND_REMOVE(Icons.Default.PersonRemove),
    FRIEND_FAVORITE(Icons.Default.Favorite),
    FRIEND_UN_FAVORITE(Icons.Default.FavoriteBorder),
}