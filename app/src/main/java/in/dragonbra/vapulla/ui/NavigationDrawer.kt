package `in`.dragonbra.vapulla.ui

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.ui.composables.NavigationDrawerContent
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
            NavigationDrawerContent(
                currentSelection = currentSelection,
                onLogOut = onLogOut,
                onNavigationClick = onNavigationClick,
            )
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