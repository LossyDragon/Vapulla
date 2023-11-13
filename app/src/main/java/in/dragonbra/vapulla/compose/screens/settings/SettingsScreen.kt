package `in`.dragonbra.vapulla.compose.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.VapullaAppbar
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.LocalActivity
import `in`.dragonbra.vapulla.manager.AccountManager

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
        onBackPressed = activity::finish,
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
    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            VapullaAppbar(
                scrollBehavior = scrollBehavior,
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
            SettingsGroupAccount(
                accountManager = accountManager,
                onChangeUser = onChangeUser,
                onChangeName = onChangeName
            )
            SettingsGroupFriends(accountManager = accountManager)
            SettingsGroupOther(accountManager = accountManager, onClearDatabase = onClearDatabase)
            SettingsGroupAbout()
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
