package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.manager.AccountManager

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsGroupOther(
    accountManager: AccountManager,
    onClearDatabase: () -> Unit
) {
    val context = LocalContext.current

    var clearDatabaseDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        title = stringResource(id = R.string.dialogTitleClearDatabase),
        message = stringResource(id = R.string.dialogMessageClearDatabase),
        openDialog = clearDatabaseDialog,
        onPositive = {
            onClearDatabase()
            clearDatabaseDialog = false
        },
        positiveText = stringResource(id = R.string.apply),
        onNegative = { clearDatabaseDialog = false },
        negativeText = stringResource(id = R.string.cancel)
    )

    var prefClearNotifications by remember {

        mutableStateOf(accountManager.prefClearNotifications)
    }
    SettingsGroup(title = { Text(text = stringResource(id = R.string.textSettingsOther)) }) {
        SettingsSwitch(
            title = {
                Text(text = stringResource(R.string.textSettingsClearNotifications))
            },
            state = prefClearNotifications,
            subtitle = {
                Text(text = stringResource(R.string.textSettingsClearNotificationsDesc))
            },
            onCheckedChange = { value ->
                accountManager.prefClearNotifications = value
                prefClearNotifications = value
            }
        )
        SettingsMenuLink(
            title = {
                Text(text = stringResource(id = R.string.textSettingsClearDatabase))
            },
            subtitle = {
                Text(
                    text = stringResource(id = R.string.textSettingsClearDatabaseDesc)
                )
            },
            onClick = { clearDatabaseDialog = true }
        )
        SettingsMenuLink(
            title = {
                Text(text = "Clear Image Cache")
            },
            subtitle = {
                Text(
                    text = "Clear the image loader cache to refresh any images that may " +
                        "have trouble displaying or is not displaying correctly"
                )
            },
            onClick = {
                context.imageLoader.diskCache?.clear()
                context.imageLoader.memoryCache?.clear()
            }
        )
        HorizontalDivider(
            Modifier
                .padding(vertical = 2.dp)
                .fillMaxWidth()
        )
    }
}
