package `in`.dragonbra.vapulla.ui.composables.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Games
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.Routes
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import org.koin.compose.getKoin

@Composable
fun NavigationDrawer(
    currentSelection: Routes,
    onLogOut: () -> Unit,
    onNavigationClick: (Routes) -> Unit,
) {
    val koin = getKoin()
    val accountManager = remember(koin) { koin.get<AccountManager>() }
    val serviceConnection = remember(koin) { koin.get<ServiceConnection>() }
    val avatar by accountManager.accountAvatar.collectAsStateWithLifecycle(null)
    val name by accountManager.accountName.collectAsStateWithLifecycle(null)
    val state by accountManager.accountPersonaState.collectAsStateWithLifecycle(null)

    NavigationDrawerContent(
        currentSelection = currentSelection,
        avatar = avatar,
        name = name,
        state = state,
        onLogOut = onLogOut,
        onPersonaState = serviceConnection::setPersonaState,
        onNavigationClick = onNavigationClick,
    )
}

@Composable
private fun NavigationDrawerContent(
    currentSelection: Routes,
    avatar: String?,
    name: String?,
    state: Int?,
    onLogOut: () -> Unit,
    onPersonaState: (EPersonaState) -> Unit,
    onNavigationClick: (Routes) -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(12.dp))

            DrawerHeader(
                avatar = avatar ?: "",
                name = name ?: "",
                state = EPersonaState.from(state ?: 0),
                onPersonaState = onPersonaState,
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(Modifier.height(12.dp))

            DrawerItem(
                text = "Friends",
                imageVector = Icons.Outlined.Groups,
                selected = currentSelection == Routes.Home,
                onClick = { onNavigationClick(Routes.Home) },
            )

            DrawerItem(
                text = "Library",
                imageVector = Icons.Outlined.Games,
                selected = currentSelection == Routes.Games,
                onClick = { onNavigationClick(Routes.Games) },
            )

            DrawerItem(
                text = "Downloads",
                imageVector = Icons.Outlined.Download,
                selected = currentSelection == Routes.Downloads,
                onClick = { onNavigationClick(Routes.Downloads) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DrawerItem(
                text = "Settings",
                imageVector = Icons.Outlined.Settings,
                selected = currentSelection == Routes.Settings,
                onClick = { onNavigationClick(Routes.Settings) },
            )

            DrawerItem(
                text = "Log Out",
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                selected = false,
                onClick = onLogOut,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        ModalNavigationDrawer(
            drawerState = rememberDrawerState(DrawerValue.Open),
            drawerContent = {
                NavigationDrawerContent(
                    currentSelection = Routes.Downloads,
                    avatar = null,
                    name = stringResource(R.string.app_name),
                    state = 2,
                    onLogOut = { },
                    onPersonaState = { },
                    onNavigationClick = { },
                )
            },
            content = { },
        )
    }
}
