package `in`.dragonbra.vapulla.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.ui.composables.navigation.NavigationDrawer
import `in`.dragonbra.vapulla.ui.screens.chat.ChatScreen
import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesScreen
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeScreen
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginScreen
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileScreen
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

sealed class Routes {
    data object Downloads : Routes()
    data object Games : Routes()
    data object Home : Routes()
    data object Login : Routes()
    data object Settings : Routes()
    data class FriendChat(val id: Long) : Routes()
    data class FriendProfile(val id: Long) : Routes()
}

@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val connection: ServiceConnection = getKoin().get()
    val scope = rememberCoroutineScope()

    val backStack = remember {
        // TODO this may be flawed.
        val route = if (SteamService.isLoggedIn.value) Routes.Home else Routes.Login
        mutableStateListOf(route)
    }

    val onNavDrawerAction: () -> Unit = {
        scope.launch {
            drawerState.apply {
                if (isClosed) open() else close()
            }
        }
    }

    LaunchedEffect(backStack) {
        // Ensure the Nav Drawer is closed on Login.
        if (backStack.last() == Routes.Login) {
            scope.launch {
                drawerState.close()
            }
        }
    }

    ModalNavigationDrawer(
        gesturesEnabled = backStack.last() != Routes.Login,
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawer(
                currentSelection = backStack.last(),
                onLogOut = {
                    connection.steamService!!.logOut()
                },
                onNavigationClick = { route ->
                    backStack.removeAll { it != Routes.Home }
                    backStack.addLast(route)
                    scope.launch {
                        drawerState.close()
                    }
                },
            )
        },
        content = {
            NavDisplay(
                modifier = modifier,
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                sceneStrategy = SinglePaneSceneStrategy(),
                entryProvider = entryProvider {
                    /* Login */
                    entry<Routes.Login> {
                        val viewModel = koinViewModel<LoginViewModel>()
                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateToHome = {
                                backStack.add(Routes.Home)
                                backStack.remove(Routes.Login)
                            },
                        )
                    }
                    /* Main Screen */
                    entry<Routes.Home> {
                        val viewModel = koinViewModel<HomeViewModel>()
                        HomeScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                            onFriendClick = { friendId ->
                                backStack.add(Routes.FriendChat(friendId))
                            },
                            onFriendLongClick = { friendId ->
                                backStack.add(Routes.FriendProfile(friendId))
                            },
                        )
                    }
                    /* Chat */
                    entry<Routes.FriendChat> {
                        val viewModel = koinViewModel<ChatViewModel> {
                            parametersOf(it.id)
                        }
                        ChatScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLast() },
                        )
                    }
                    /* Profile */
                    entry<Routes.FriendProfile> {
                        val viewModel = koinViewModel<ProfileViewModel> {
                            parametersOf(it.id)
                        }
                        ProfileScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                            onBack = { backStack.removeLast() },
                        )
                    }
                    /* Games */
                    entry<Routes.Games> {
                        val viewModel = koinViewModel<GamesViewModel>()
                        GamesScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                        )
                    }
                    /* Downloads */
                    entry<Routes.Downloads> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Downloads")
                        }
                    }
                    /* Settings */
                    entry<Routes.Settings> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Settings")
                        }
                    }
                },
            )
        },
    )
}
