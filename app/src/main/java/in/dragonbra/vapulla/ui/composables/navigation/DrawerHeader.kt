package `in`.dragonbra.vapulla.ui.composables.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
import timber.log.Timber

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DrawerHeader(
    avatar: String,
    name: String,
    state: EPersonaState,
    onPersonaState: (EPersonaState) -> Unit,
) {
    val localUser = remember(avatar, name, state) {
        Timber.d("Drawer Header: $name is  ${state.name}")
        SteamFriend(
            id = 0,
            name = name,
            avatar = avatar,
            state = state,
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            FriendAvatar(
                size = 128.dp,
                friend = localUser,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = localUser.nameOrNickname,
                style = MaterialTheme.typography.titleLargeEmphasized,
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 3,
                    ),
                    onClick = { onPersonaState(EPersonaState.Online) },
                    selected = localUser.state == EPersonaState.Online,
                    label = { Text(text = "Online") },
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 3,
                    ),
                    onClick = { onPersonaState(EPersonaState.Away) },
                    selected = localUser.state == EPersonaState.Away,
                    label = { Text(text = "Away") },
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 2,
                        count = 3,
                    ),
                    onClick = { onPersonaState(EPersonaState.Invisible) },
                    selected = localUser.state == EPersonaState.Invisible,
                    label = { Text(text = "Invisible") },
                )
            }
        },
    )
}
