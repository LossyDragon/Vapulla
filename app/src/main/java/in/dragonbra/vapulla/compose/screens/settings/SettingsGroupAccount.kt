package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaEditDialog
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.manager.AccountManager

@Composable
fun SettingsGroupAccount(
    accountManager: AccountManager,
    onChangeUser: () -> Unit,
    onChangeName: (String) -> Unit
) {
    var changeUserDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        icon = Icons.AutoMirrored.Filled.Logout,
        title = stringResource(id = R.string.dialogTitleChangeUser),
        message = stringResource(id = R.string.dialogMessageChangeUser),
        positiveText = stringResource(id = R.string.buttonLogout),
        negativeText = stringResource(id = R.string.cancel),
        openDialog = changeUserDialog,
        onPositive = {
            onChangeUser()
            changeUserDialog = false
        },
        onNegative = {
            changeUserDialog = false
        }
    )

    var changeNameDialog by remember { mutableStateOf(false) }
    VapullaEditDialog(
        icon = Icons.Default.Edit,
        title = stringResource(id = R.string.dialogTitleChangeName),
        editTextLabel = stringResource(id = R.string.textLabelProfileName),
        currentName = accountManager.nickname,
        openDialog = changeNameDialog,
        onConfirm = {
            onChangeName(it)
            changeNameDialog = false
        },
        onDismiss = { changeNameDialog = false }
    )

    SettingsGroup(title = { Text(text = stringResource(id = R.string.textSettingsAccount)) }) {
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsChangeAccount)) },
            subtitle = {
                val username = stringResource(
                    id = R.string.textSettingsChangeAccountDesc,
                    accountManager.username ?: "*unknown*"
                )
                Text(text = username)
            },
            onClick = { changeUserDialog = true }
        )
        SettingsMenuLink(
            title = { Text(text = stringResource(R.string.textSettingsChangeProfileName)) },
            subtitle = { Text(text = accountManager.nickname ?: "*unknown*") },
            onClick = { changeNameDialog = true }
        )
        HorizontalDivider(
            Modifier
                .padding(vertical = 2.dp)
                .fillMaxWidth()
        )
    }
}
