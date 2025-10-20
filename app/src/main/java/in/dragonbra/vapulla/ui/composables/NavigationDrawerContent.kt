package `in`.dragonbra.vapulla.ui.composables

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.VapullaApplication
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.di.appModule
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.Routes
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.getKoin
import org.koin.dsl.module
import timber.log.Timber

@Composable
fun NavigationDrawerContent(
    currentSelection: Routes,
    onLogOut: () -> Unit,
    onNavigationClick: (Routes) -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            DrawerHeader()

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DrawerHeader() {
    val koin = getKoin()
    val accountManager = remember(koin) { koin.get<AccountManager>() }
    val serviceConnection = remember(koin) { koin.get<ServiceConnection>() }
    val avatar by accountManager.accountAvatar.collectAsStateWithLifecycle(null)
    val name by accountManager.accountName.collectAsStateWithLifecycle(null)
    val state by accountManager.accountPersonaState.collectAsStateWithLifecycle(null)

    val localUser = remember(avatar, name, state) {
        val personaState = EPersonaState.from(state ?: 0)
        Timber.d("Drawer Header: $name is  ${personaState.name}")
        SteamFriend(
            id = 0,
            name = name ?: "",
            avatar = avatar ?: Utils.Constants.MISSING_AVATAR_URL,
            state = personaState
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            FriendAvatar(
                size = 128.dp,
                friend = localUser
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = localUser.nameOrNickname,
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 3
                    ),
                    onClick = { serviceConnection.setPersonaState(EPersonaState.Online) },
                    selected = localUser.state == EPersonaState.Online,
                    label = { Text(EPersonaState.Online.name) }
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 3
                    ),
                    onClick = { serviceConnection.setPersonaState(EPersonaState.Away) },
                    selected = localUser.state == EPersonaState.Away,
                    label = { Text(EPersonaState.Away.name) }
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 2,
                        count = 3
                    ),
                    onClick = { serviceConnection.setPersonaState(EPersonaState.Invisible) },
                    selected = localUser.state == EPersonaState.Invisible,
                    label = { Text(EPersonaState.Invisible.name) }
                )
            }
        }
    )
}

@Composable
private fun DrawerItem(
    text: String,
    imageVector: ImageVector,
    contentDescription: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(text = text) },
        icon = { Icon(imageVector = imageVector, contentDescription = contentDescription) },
        selected = selected,
        onClick = onClick
    )
}

@Preview
@Composable
private fun Preview() {
    val context = LocalContext.current
    val previewModule = module {
        single<AccountManager> { AccountManager(context) }
        single<ServiceConnection> { ServiceConnection(context) }
    }

    KoinApplicationPreview(
        application = {
            androidContext(context)
            modules(previewModule)
        },
        {
            VapullaTheme {
                ModalNavigationDrawer(
                    drawerState = rememberDrawerState(DrawerValue.Open),
                    drawerContent = {
                        NavigationDrawerContent(
                            currentSelection = Routes.Downloads,
                            onLogOut = { },
                            onNavigationClick = { },
                        )
                    },
                    content = { }
                )
            }
        }
    )
}