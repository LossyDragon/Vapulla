package `in`.dragonbra.vapulla.compose.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.compose.ui.icons.VR

@Composable
fun getStatusIcon(friend: FriendListItem?): ImageVector? {
    val personaState = EPersonaState.from(friend?.state ?: 0)
    val flags = EPersonaStateFlag.from(friend?.stateFlags ?: 0)
    return if (personaState == EPersonaState.Away) {
        Icons.Default.Bedtime
    } else {
        when {
            flags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
            flags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
            flags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
            flags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
            else -> null
        }
    }
}