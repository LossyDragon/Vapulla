package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.rememberBooleanSettingState
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.VapullaEditDialog
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.compose.components.VapullaSelectionDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.manager.AccountManager

private val recentsMap = mapOf(
    "Disable" to -1L,
    "1 day" to 86400000L,
    "3 days" to 259200000L,
    "1 week" to 604800000L,
    "2 weeks" to 1209600000L,
    "1 month" to 2592000000L,
    "Forever" to 0L
)

@Composable
fun SettingsScreen(
    accountManager: AccountManager,
    onChangeName: (String) -> Unit,
    onChangeUser: () -> Unit,
    onClearDatabase: () -> Unit
) {
    val activity = LocalActivity.current

    SettingsContent(
        accountManager = accountManager,
        onBackPressed = { activity.finish() },
        onChangeName = onChangeName,
        onChangeUser = onChangeUser,
        onClearDatabase = onClearDatabase
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    accountManager: AccountManager,
    onBackPressed: () -> Unit,
    onChangeName: (String) -> Unit,
    onChangeUser: () -> Unit,
    onClearDatabase: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var changeUserDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        icon = Icons.Default.Logout,
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

    var changeRecentDialog by remember { mutableStateOf(false) }
    VapullaSelectionDialog(
        title = stringResource(id = R.string.dialogTitleRecentFriendChats),
        currentSelection = accountManager.prefFriendsListRecents,
        items = recentsMap,
        openDialog = changeRecentDialog,
        onPositive = {
            accountManager.prefFriendsListRecents = it
            changeRecentDialog = false
        },
        positiveText = stringResource(id = R.string.apply),
        onNegative = { changeRecentDialog = false },
        negativeText = stringResource(id = R.string.cancel)
    )

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

    val scrollState = rememberScrollState()
    Scaffold(
        topBar = {
            VapullaAppbar(
                toolbarText = stringResource(id = R.string.title_activity_settings),
                onBackPressed = onBackPressed
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            /* Account */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.textSettingsAccount)
                    )
                }
            ) {
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
                Divider(
                    Modifier
                        .padding(vertical = 2.dp)
                        .fillMaxWidth()
                )
            }

            /* Friends */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.textSettingsFriends)
                    )
                }
            ) {
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.textSettingsRecentChats)) },
                    subtitle = {
                        val text = recentsMap.entries.find {
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

            /* Other */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.textSettingsOther)
                    )
                }
            ) {
                SettingsSwitch(
                    title = {
                        Text(text = stringResource(R.string.textSettingsClearNotifications))
                    },
                    state = rememberBooleanSettingState(accountManager.prefClearNotifications),
                    subtitle = {
                        Text(text = stringResource(R.string.textSettingsClearNotificationsDesc))
                    },
                    onCheckedChange = { value ->
                        accountManager.prefClearNotifications = value
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
                Divider(
                    Modifier
                        .padding(vertical = 2.dp)
                        .fillMaxWidth()
                )
            }

            /* About */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.textSettingsAbout)
                    )
                }
            ) {
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.textSettingsVersion)) },
                    subtitle = { Text(text = BuildConfig.VERSION_NAME) },
                    onClick = { /* Unit */ }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.textSettingsRateApp)) },
                    enabled = false,
                    onClick = {
                        val pkgName = context.packageName
                        val url = "https://play.google.com/store/apps/details?id=$pkgName"
                        uriHandler.openUri(url)
                    }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.textSettingsSourceCode)) },
                    onClick = {
                        val url = "https://github.com/Longi94/Vapulla"
                        uriHandler.openUri(url)
                    }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.textSettingsLicenses)) },
                    onClick = {
                        val githubUrl = "https://raw.githubusercontent.com"
                        val url = "$githubUrl/Longi94/Vapulla/master/third_party.txt"
                        uriHandler.openUri(url)
                    }
                )

                Divider(
                    Modifier
                        .padding(top = 2.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_SettingsScreen() {
    VapullaTheme {
        Surface {
            SettingsContent(
                accountManager = AccountManager(LocalContext.current),
                onBackPressed = {},
                onChangeUser = {},
                onChangeName = {},
                onClearDatabase = {}
            )
        }
    }
}
