package `in`.dragonbra.vapulla.ui.composables

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.ui.Routes
import `in`.dragonbra.vapulla.ui.screens.home.components.FriendAvatar
import `in`.dragonbra.vapulla.util.Utils

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

            DrawerHeader(

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DrawerHeader() {
    val context = LocalContext.current
    var localAccount by remember { mutableStateOf(SteamFriend(0)) }

    DisposableEffect(Unit) {
        val accountManager = AccountManager(context)
        localAccount = localAccount.copy(
            avatar = accountManager.avatarHash ?: Utils.Constants.MISSING_AVATAR_URL,
            name = accountManager.nickname ?: "",
            state = accountManager.state
        )
        val listener = object : AccountManager.AccountManagerListener {
            override fun unAccountUpdate(account: AccountManager) {
                localAccount = localAccount.copy(
                    avatar = account.avatarHash ?: Utils.Constants.MISSING_AVATAR_URL,
                    name = accountManager.nickname ?: "",
                    state = account.state
                )
            }
        }
        accountManager.addListener(listener)
        onDispose {
            accountManager.removeListener(listener)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = {
            FriendAvatar(
                size = 128.dp,
                friend = localAccount
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = localAccount.nameOrNickname,
                style = MaterialTheme.typography.titleLargeEmphasized
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 0,
                        count = 2
                    ),
                    onClick = { TODO() },
                    selected = localAccount.state == EPersonaState.Online,
                    label = { Text(EPersonaState.Online.name) }
                )
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = 1,
                        count = 2
                    ),
                    onClick = { TODO() },
                    selected = localAccount.state != EPersonaState.Online,
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
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