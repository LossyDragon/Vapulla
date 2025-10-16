package `in`.dragonbra.vapulla.ui

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
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun NavigationDrawer(
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean,
    drawerState: DrawerState,
    currentSelection: Routes,
    onLogOut: () -> Unit,
    onNavigationClick: (Routes) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        modifier = modifier,
        gesturesEnabled = gesturesEnabled,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )

                    HorizontalDivider()

                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text(text = "Friends") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null
                            )
                        },
                        selected = currentSelection == Routes.Home,
                        onClick = { onNavigationClick(Routes.Home) }
                    )

                    NavigationDrawerItem(
                        label = { Text(text = "Library") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Games,
                                contentDescription = null
                            )
                        },
                        selected = currentSelection == Routes.Games,
                        onClick = { onNavigationClick(Routes.Games) }
                    )

                    NavigationDrawerItem(
                        label = { Text(text = "Downloads") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null
                            )
                        },
                        selected = currentSelection == Routes.Downloads,
                        onClick = { onNavigationClick(Routes.Downloads) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text(text = "Settings") },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null
                            )
                        },
                        selected = currentSelection == Routes.Settings,
                        onClick = { onNavigationClick(Routes.Settings) }
                    )

                    NavigationDrawerItem(
                        label = { Text(text = "Log Out") },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null
                            )
                        },
                        selected = false,
                        onClick = onLogOut
                    )
                }
            }
        },
        content = content
    )
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        NavigationDrawer(
            gesturesEnabled = false,
            drawerState = DrawerState(initialValue = DrawerValue.Open),
            currentSelection = Routes.Games,
            onLogOut = { },
            onNavigationClick = { },
            content = { },
        )
    }
}