package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaSelectionDialog
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.manager.AccountManager

@Composable
fun SettingsGroupFriends(
    accountManager: AccountManager
) {
    var changeRecentDialog by remember { mutableStateOf(false) }
    VapullaSelectionDialog(
        title = stringResource(id = R.string.dialogTitleRecentFriendChats),
        currentSelection = accountManager.prefFriendsListRecents,
        items = Constants.recentsMap,
        openDialog = changeRecentDialog,
        onPositive = {
            accountManager.prefFriendsListRecents = it
            changeRecentDialog = false
        },
        positiveText = stringResource(id = R.string.apply),
        onNegative = { changeRecentDialog = false },
        negativeText = stringResource(id = R.string.cancel)
    )

    SettingsGroup(title = { Text(text = stringResource(id = R.string.textSettingsFriends)) }) {
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsRecentChats)) },
            subtitle = {
                val text = Constants.recentsMap.entries.find {
                    it.value == accountManager.prefFriendsListRecents
                }?.key ?: "??"
                Text(text = text)
            },
            onClick = { changeRecentDialog = true }
        )
        SettingsSwitch(
            title = { Text(text = stringResource(R.string.textSettingsSortFriendBy)) },
            subtitle = {
                Text(text = stringResource(R.string.textSettingsSortFriendByDesc))
            },
            state = rememberBooleanSettingState(accountManager.prefFriendsListSort),
            onCheckedChange = { value ->
                accountManager.prefFriendsListSort = value
            }
        )
        Divider(
            Modifier
                .padding(vertical = 2.dp)
                .fillMaxWidth()
        )
    }
}
