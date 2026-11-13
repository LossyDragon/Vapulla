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
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.ui.composables.navigation.NavigationDrawer
import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsScreen
import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsViewModel
import `in`.dragonbra.vapulla.ui.screens.chat.ChatScreen
import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsScreen
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsViewModel
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesScreen
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesViewModel
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailScreen
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesScreen
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatScreen
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeScreen
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginScreen
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.networklog.NetworkLogScreen
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileScreen
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserScreen
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserViewModel
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsScreen
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsViewModel
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsScreen
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsViewModel
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopScreen
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/** Navigation 3 destinations. The data classes carry the friend's 64-bit Steam ID. */
sealed class Routes {
    data object Downloads : Routes()
    data object Games : Routes()
    data object Home : Routes()
    data object Login : Routes()
    data object Settings : Routes()
    data object ChatRooms : Routes()
    data object ServerBrowser : Routes()
    data object Sessions : Routes()
    data object NetworkLog : Routes()
    data class FriendChat(val id: Long) : Routes()
    data class FriendProfile(val id: Long) : Routes()
    data class GameDetail(val appId: Int) : Routes()
    data class CloudFiles(val appId: Int) : Routes()
    data class Workshop(val appId: Int) : Routes()
    data class Achievements(val appId: Int) : Routes()
    data class GroupChat(
        val groupId: Long,
        val chatId: Long,
        val groupName: String,
        val roomName: String,
    ) : Routes()
}

/** Hosts the navigation drawer and the single-pane back stack of [Routes]. */
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    openChat: Long? = null,
    onChatOpened: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val connection: ServiceConnection = getKoin().get()
    val scope = rememberCoroutineScope()

    val backStack = remember {
        // Skip the login screen when the service is already signed in (e.g. after the
        // activity was recreated while the foreground service kept running).
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

    // Return to sign-in whenever the service stops being signed in: after logging out, or
    // once a dropped connection could not be re-established.
    LaunchedEffect(Unit) {
        SteamService.isLoggedIn.collect { loggedIn ->
            if (!loggedIn && backStack.lastOrNull() != Routes.Login) {
                backStack.add(Routes.Login)
                backStack.removeAll { it != Routes.Login }
            }
        }
    }

    // A tapped message notification asks for a chat. Home being on the stack means we are
    // signed in; until then (the app was started by the tap and is still signing in) the
    // request waits, and is carried out as soon as Home appears.
    val signedIn = Routes.Home in backStack
    LaunchedEffect(openChat, signedIn) {
        if (openChat != null && signedIn) {
            val chat = Routes.FriendChat(openChat)
            if (backStack.lastOrNull() != chat) {
                // Back from the chat leads to the friends list, whatever was open before.
                backStack.removeAll { it != Routes.Home }
                backStack.add(chat)
            }
            drawerState.close()
            onChatOpened()
        }
    }

    LaunchedEffect(backStack.lastOrNull()) {
        // Ensure the Nav Drawer is closed on Login.
        if (backStack.lastOrNull() == Routes.Login) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        gesturesEnabled = backStack.last() != Routes.Login,
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawer(
                currentSelection = backStack.last(),
                onLogOut = {
                    connection.steamService?.logOut()
                },
                onNavigationClick = { route ->
                    // Home is the root of the stack, so going there only pops back to it.
                    backStack.removeAll { it != Routes.Home }
                    if (route != Routes.Home || backStack.isEmpty()) {
                        backStack.add(route)
                    }
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
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    /* Profile */
                    entry<Routes.FriendProfile> {
                        val viewModel = koinViewModel<ProfileViewModel> {
                            parametersOf(it.id)
                        }
                        ProfileScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onChat = { friendId -> backStack.add(Routes.FriendChat(friendId)) },
                        )
                    }
                    /* Games */
                    entry<Routes.Games> {
                        val viewModel = koinViewModel<GamesViewModel>()
                        GamesScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                            onGameClick = { appId -> backStack.add(Routes.GameDetail(appId)) },
                        )
                    }
                    entry<Routes.GameDetail> {
                        val viewModel =
                            koinViewModel<GameDetailViewModel> { parametersOf(it.appId) }
                        GameDetailScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenCloud = { appId -> backStack.add(Routes.CloudFiles(appId)) },
                            onOpenWorkshop = { appId -> backStack.add(Routes.Workshop(appId)) },
                            onOpenAchievements = { appId ->
                                backStack.add(Routes.Achievements(appId))
                            },
                        )
                    }
                    entry<Routes.CloudFiles> {
                        val viewModel =
                            koinViewModel<CloudFilesViewModel> { parametersOf(it.appId) }
                        CloudFilesScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Routes.Workshop> {
                        val viewModel = koinViewModel<WorkshopViewModel> { parametersOf(it.appId) }
                        WorkshopScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Routes.Achievements> {
                        val viewModel =
                            koinViewModel<AchievementsViewModel> { parametersOf(it.appId) }
                        AchievementsScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    /* Showcase screens reachable from the drawer */
                    entry<Routes.ChatRooms> {
                        val viewModel = koinViewModel<ChatRoomsViewModel>()
                        ChatRoomsScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                            onRoomClick = { group, room ->
                                backStack.add(
                                    Routes.GroupChat(
                                        groupId = group.groupId,
                                        chatId = room.chatId,
                                        groupName = group.name,
                                        roomName = room.name,
                                    ),
                                )
                            },
                        )
                    }
                    entry<Routes.GroupChat> {
                        val viewModel = koinViewModel<GroupChatViewModel> {
                            parametersOf(it.groupId, it.chatId, it.groupName, it.roomName)
                        }
                        GroupChatScreen(
                            viewModel = viewModel,
                            onBack = { backStack.removeLastOrNull() },
                            // Replaces this entry rather than adding one, so Back always
                            // returns to Group Chats however many channels were visited.
                            onSwitchChannel = { room ->
                                backStack[backStack.lastIndex] = it.copy(
                                    chatId = room.chatId,
                                    roomName = room.name,
                                )
                            },
                        )
                    }
                    entry<Routes.ServerBrowser> {
                        val viewModel = koinViewModel<ServerBrowserViewModel>()
                        ServerBrowserScreen(
                            viewModel = viewModel,
                            onNavDrawerAction = onNavDrawerAction,
                        )
                    }
                    entry<Routes.Sessions> {
                        val viewModel = koinViewModel<SessionsViewModel>()
                        SessionsScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
                    }
                    entry<Routes.NetworkLog> {
                        NetworkLogScreen(onNavDrawerAction = onNavDrawerAction)
                    }
                    /* Downloads */
                    entry<Routes.Downloads> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Downloads")
                        }
                    }
                    entry<Routes.Settings> {
                        val viewModel = koinViewModel<SettingsViewModel>()
                        SettingsScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
                    }
                },
            )
        },
    )
}
