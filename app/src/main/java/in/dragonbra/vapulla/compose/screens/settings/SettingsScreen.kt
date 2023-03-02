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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onBackPressed: () -> Unit,
    onChangeName: (String) -> Unit,
    onChangeUser: () -> Unit,
    onBrowseUrl: (String) -> Unit
) {
    val context = LocalContext.current

    var changeUserDialog by remember { mutableStateOf(false) }
    VapullaMessageDialog(
        icon = Icons.Default.Logout,
        title = stringResource(id = R.string.dialogTitleChangeUser),
        message = stringResource(id = R.string.dialogMessageChangeUser),
        positiveText = stringResource(id = R.string.dialogYes),
        negativeText = stringResource(id = R.string.dialogNo),
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
        editTextLabel = stringResource(id = R.string.profileName),
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
        title = stringResource(id = R.string.prefTitleFriendsRecents),
        currentSelection = accountManager.prefFriendsListRecents,
        items = recentsMap,
        openDialog = changeRecentDialog,
        onPositive = {
            accountManager.prefFriendsListRecents = it
            changeRecentDialog = false
        },
        positiveText = stringResource(id = R.string.dialogConfirm),
        onNegative = { changeRecentDialog = false },
        negativeText = stringResource(id = R.string.dialogCancel)
    )

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
                .verticalScroll(rememberScrollState())
        ) {
            /* Account */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.prefCategoryAccount),
                        color = Color.White
                    )
                }
            ) {
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleChangeUser)) },
                    subtitle = {
                        val username = stringResource(
                            id = R.string.prefSummaryChangeUser,
                            accountManager.username ?: "*unknown*"
                        )
                        Text(text = username)
                    },
                    onClick = { changeUserDialog = true }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleChangeProfileName)) },
                    subtitle = { Text(text = accountManager.nickname ?: "*unknown*") },
                    onClick = { changeNameDialog = true }
                )
                Divider(Modifier.padding(vertical = 2.dp).fillMaxWidth())
            }

            /* Friends */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.prefCategoryFriends),
                        color = Color.White
                    )
                }
            ) {
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleFriendsRecents)) },
                    subtitle = {
                        val text = recentsMap.entries.find {
                            it.value == accountManager.prefFriendsListRecents
                        }?.key ?: "??"
                        Text(text = text)
                    },
                    onClick = { changeRecentDialog = true }
                )
                SettingsSwitch(
                    title = { Text(text = stringResource(R.string.prefTitleSortFriends)) },
                    subtitle = {
                        val text = if (accountManager.prefFriendsListSort) "Status" else "Name"
                        Text(text = text)
                    },
                    state = rememberBooleanSettingState(accountManager.prefFriendsListSort),
                    onCheckedChange = { value ->
                        accountManager.prefFriendsListSort = value
                    }
                )
                Divider(Modifier.padding(vertical = 2.dp).fillMaxWidth())
            }

            /* Other */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.prefCategoryOther),
                        color = Color.White
                    )
                }
            ) {
                SettingsSwitch(
                    title = { Text(text = stringResource(R.string.prefTitleClearNotifications)) },
                    state = rememberBooleanSettingState(accountManager.prefClearNotifications),
                    subtitle = {
                        Text(text = stringResource(id = R.string.prefSummaryClearNotifications))
                    },
                    onCheckedChange = { value ->
                        accountManager.prefClearNotifications = value
                    }
                )
                Divider(Modifier.padding(vertical = 2.dp).fillMaxWidth())
            }

            /* About */
            SettingsGroup(
                title = {
                    Text(
                        text = stringResource(id = R.string.prefCategoryAbout),
                        color = Color.White
                    )
                }
            ) {
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleVersion)) },
                    subtitle = { Text(text = BuildConfig.VERSION_NAME) },
                    onClick = { /* Unit */ }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleRateApp)) },
                    enabled = false,
                    onClick = {
                        val pkgName = context.packageName
                        onBrowseUrl("https://play.google.com/store/apps/details?id=$pkgName")
                    }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleSourceCode)) },
                    onClick = {
                        onBrowseUrl("https://github.com/Longi94/Vapulla")
                    }
                )
                SettingsMenuLink(
                    title = { Text(text = stringResource(R.string.prefTitleLicenses)) },
                    onClick = {
                        val githubUrl = "https://raw.githubusercontent.com"
                        val url = "$githubUrl/Longi94/Vapulla/master/third_party.txt"
                        onBrowseUrl(url)
                    }
                )

                Divider(Modifier.padding(top = 2.dp).fillMaxWidth())
            }
        }
    }
}

@Preview
@Composable
private fun Preview_SettingsScreen() {
    VapullaTheme {
        SettingsScreen(
            accountManager = AccountManager(LocalContext.current),
            onBackPressed = {},
            onBrowseUrl = {},
            onChangeUser = {},
            onChangeName = {}
        )
    }
}
