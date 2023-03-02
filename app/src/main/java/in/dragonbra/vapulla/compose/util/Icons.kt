package `in`.dragonbra.vapulla.compose.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.icons.VR

@Composable
fun getStatusIcon(friend: FriendListItem?): ImageVector? {
    val flags = EPersonaStateFlag.from(friend?.stateFlags ?: 0)
    return when {
        friend?.isRequestRecipient() == true -> Icons.Default.PersonAdd
        friend?.isAwayOrSnooze() == true -> Icons.Default.Bedtime
        flags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
        flags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
        flags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
        flags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
        else -> null
    }
}
