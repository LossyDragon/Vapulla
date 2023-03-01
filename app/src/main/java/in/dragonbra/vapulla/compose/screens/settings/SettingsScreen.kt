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
import com.alorma.compose.settings.ui.SettingsGroup
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.components.VapullaEditDialog
import `in`.dragonbra.vapulla.compose.components.VapullaMessageDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.manager.AccountManager

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
        onPositive = onChangeUser,
        onNegative = {
            changeUserDialog = false
        }
    )

    // TODO not quite right
    var changeNameDialog by remember { mutableStateOf(false) }
    VapullaEditDialog(
        icon = Icons.Default.Edit,
        name = accountManager.nickname!!,
        currentName = accountManager.nickname!!,
        openDialog = changeNameDialog,
        onConfirm = onChangeName,
        onDismiss = {
            changeNameDialog = false
        }
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
                Divider(Modifier.fillMaxWidth())
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
                    onClick = {
                        // TODO set recent chats time out to the following.
                        //  Disable -> -1
                        //  1 day -> 86400000
                        //  3 days -> 259200000
                        //  1 week -> 604800000
                        //  2 weeks -> 1209600000
                        //  1 month -> 2592000000
                        //  Forever -> 0
                    }
                )
                SettingsSwitch(
                    title = { Text(text = stringResource(R.string.prefTitleSortFriends)) },
                    onCheckedChange = {
                        // TODO show list to sort by "Name" -> 0 or "Status" -> 1, defaults to "Status"
                    }
                )
                Divider(Modifier.fillMaxWidth())
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
                    subtitle = {
                        Text(
                            text = stringResource(id = R.string.prefSummaryClearNotifications)
                        )
                    },
                    onCheckedChange = {
                        // TODO change pref_clear_notifications
                    }
                )
                Divider(Modifier.fillMaxWidth())
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
