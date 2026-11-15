package `in`.dragonbra.vapulla.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import `in`.dragonbra.vapulla.manager.LocalAppSettings
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.service.SteamService
import `in`.dragonbra.vapulla.ui.composables.navigation.NavigationDrawer
import `in`.dragonbra.vapulla.ui.screens.account.AccountScreen
import `in`.dragonbra.vapulla.ui.screens.account.AccountViewModel
import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsScreen
import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsViewModel
import `in`.dragonbra.vapulla.ui.screens.chat.ChatScreen
import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsScreen
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsViewModel
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesScreen
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesViewModel
import `in`.dragonbra.vapulla.ui.screens.downloads.DownloadsScreen
import `in`.dragonbra.vapulla.ui.screens.downloads.DownloadsViewModel
import `in`.dragonbra.vapulla.ui.screens.family.FamilyScreen
import `in`.dragonbra.vapulla.ui.screens.family.FamilyViewModel
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailScreen
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesScreen
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatScreen
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeScreen
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.leaderboards.LeaderboardsScreen
import `in`.dragonbra.vapulla.ui.screens.leaderboards.LeaderboardsViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginScreen
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.networklog.NetworkLogScreen
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileScreen
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import `in`.dragonbra.vapulla.ui.screens.screenshots.ScreenshotsScreen
import `in`.dragonbra.vapulla.ui.screens.screenshots.ScreenshotsViewModel
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserScreen
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserViewModel
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsScreen
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsViewModel
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsScreen
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsViewModel
import `in`.dragonbra.vapulla.ui.screens.store.StorePageScreen
import `in`.dragonbra.vapulla.ui.screens.store.StorePageViewModel
import `in`.dragonbra.vapulla.ui.screens.store.StoreScreen
import `in`.dragonbra.vapulla.ui.screens.store.StoreViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.AuthTicketScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.AuthTicketViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.CmServersScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.CmServersViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.ConnectionScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.DepotsScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.DepotsViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.DiagnosticsScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.DownloadInternalsScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.DownloadInternalsViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.IdDecoderScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.LicensesScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.LicensesViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.ManifestScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.ManifestViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.PicsExplorerScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.PicsExplorerViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.PicsFeedScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.UnderTheHoodScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.WebApiScreen
import `in`.dragonbra.vapulla.ui.screens.underhood.WebApiViewModel
import `in`.dragonbra.vapulla.ui.screens.web.LocalExternalUriHandler
import `in`.dragonbra.vapulla.ui.screens.web.SteamPages
import `in`.dragonbra.vapulla.ui.screens.web.WebPageScreen
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopScreen
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopViewModel
import java.io.Serializable
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf

/** Navigation 3 destinations. The data classes carry the friend's 64-bit Steam ID. */
sealed class Routes : Serializable {
    data object Downloads : Routes()
    data object Games : Routes()
    data object Home : Routes()
    data object Login : Routes()
    data object Settings : Routes()
    data object ChatRooms : Routes()
    data object ServerBrowser : Routes()
    data object Sessions : Routes()
    data object Account : Routes()
    data object Family : Routes()
    data object Store : Routes()
    data object Screenshots : Routes()
    data object NetworkLog : Routes()
    data object UnderTheHood : Routes()
    data object ConnectionInfo : Routes()
    data object CmServers : Routes()
    data object Licenses : Routes()
    data object IdDecoder : Routes()
    data object AuthTicket : Routes()
    data object Diagnostics : Routes()
    data object PicsFeed : Routes()
    data object WebApi : Routes()
    data object DownloadInternals : Routes()
    data class FriendChat(val id: Long) : Routes()
    data class FriendProfile(val id: Long) : Routes()
    data class GameDetail(val appId: Int) : Routes()
    data class CloudFiles(val appId: Int) : Routes()
    data class Workshop(val appId: Int) : Routes()
    data class Achievements(val appId: Int) : Routes()
    data class Leaderboards(val appId: Int) : Routes()

    /** One of Steam's web pages inside the app, signed in; see the setting for it. */
    data class WebPage(val url: String) : Routes()

    /** The store page of any app, owned or not. */
    data class StorePage(val appId: Int) : Routes()

    /** The depot browser; [appId] 0 opens it on its search box. */
    data class Depots(val appId: Int = 0) : Routes()

    /** The PICS explorer; [id] 0 opens it on its search box. */
    data class PicsExplorer(val id: Int = 0, val isApp: Boolean = true) : Routes()

    /** The files of one manifest of a depot. */
    data class Manifest(
        val appId: Int,
        val depotId: Int,
        val manifestId: Long,
        val branch: String,
    ) : Routes()
    data class GroupChat(
        val groupId: Long,
        val chatId: Long,
        val groupName: String,
        val roomName: String,
    ) : Routes()
}

/**
 * Keeps the back stack when the activity is recreated, such as when the phone is rotated.
 * The routes are stored as they are, which is why [Routes] is [Serializable].
 */
private val BackStackSaver = listSaver<SnapshotStateList<Routes>, Routes>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

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

    val backStack = rememberSaveable(saver = BackStackSaver) {
        // Skip the login screen when the service is already signed in (e.g. after the
        // activity was recreated while the foreground service kept running).
        val route = if (SteamService.isLoggedIn.value) Routes.Home else Routes.Login
        mutableStateListOf(route)
    }

    // A chat links to the profile and the profile back to the chat. Going to the screen we
    // just came from pops back to it, so the two do not pile up on the back stack.
    fun openOrReturnTo(route: Routes) {
        if (backStack.getOrNull(backStack.lastIndex - 1) == route) {
            backStack.removeLastOrNull()
        } else {
            backStack.add(route)
        }
    }

    // With the setting on, Steam's community and store pages open inside the app, signed
    // in. Every other address, and every address with the setting off, goes to the browser.
    val browser = LocalUriHandler.current
    val signedInWebPages = LocalAppSettings.current.signedInWebPages
    val uriHandler = remember(browser, signedInWebPages) {
        object : UriHandler {
            override fun openUri(uri: String) {
                if (signedInWebPages && SteamPages.isSignedInPage(uri)) {
                    backStack.add(Routes.WebPage(uri))
                } else {
                    browser.openUri(uri)
                }
            }
        }
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

    // An anonymous session has only Under the Hood to show, and no drawer to get lost in.
    // When it ends, by going back or by Steam, so does the visit.
    val anonymous by SteamService.isAnonymous.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        SteamService.isAnonymous.collect { isAnonymous ->
            if (isAnonymous) {
                if (backStack.lastOrNull() == Routes.Login) backStack.add(Routes.UnderTheHood)
            } else if (!SteamService.isLoggedIn.value && backStack.lastOrNull() != Routes.Login) {
                backStack.add(Routes.Login)
                backStack.removeAll { it != Routes.Login }
            }
        }
    }

    LaunchedEffect(backStack.lastOrNull()) {
        // Ensure the Nav Drawer is closed on Login.
        if (backStack.lastOrNull() == Routes.Login) {
            drawerState.close()
        }
    }

    CompositionLocalProvider(
        LocalUriHandler provides uriHandler,
        LocalExternalUriHandler provides browser,
    ) {
        ModalNavigationDrawer(
            // Not on a web page either: a swipe there belongs to the page, to scroll it sideways.
            gesturesEnabled = !anonymous &&
                backStack.last().let { it != Routes.Login && it !is Routes.WebPage },
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
                                onProfile = { openOrReturnTo(Routes.FriendProfile(it.id)) },
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
                                onChat = { friendId ->
                                    openOrReturnTo(Routes.FriendChat(friendId))
                                },
                            )
                        }
                        /* Games */
                        entry<Routes.Games> {
                            val viewModel = koinViewModel<GamesViewModel>()
                            GamesScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                                onGameClick = { appId -> backStack.add(Routes.GameDetail(appId)) },
                                onOpenDownloads = { backStack.add(Routes.Downloads) },
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
                                // The two link to each other, so going back and forth pops
                                // instead of piling up.
                                onOpenDownloads = { openOrReturnTo(Routes.Downloads) },
                                onOpenDepots = { appId -> backStack.add(Routes.Depots(appId)) },
                                onOpenStore = { appId -> openOrReturnTo(Routes.StorePage(appId)) },
                                onOpenLeaderboards = { appId ->
                                    backStack.add(Routes.Leaderboards(appId))
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
                            val viewModel =
                                koinViewModel<WorkshopViewModel> { parametersOf(it.appId) }
                            WorkshopScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.Leaderboards> {
                            val viewModel =
                                koinViewModel<LeaderboardsViewModel> { parametersOf(it.appId) }
                            LeaderboardsScreen(
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
                        entry<Routes.Account> {
                            val viewModel = koinViewModel<AccountViewModel>()
                            AccountScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                            )
                        }
                        entry<Routes.Family> {
                            val viewModel = koinViewModel<FamilyViewModel>()
                            FamilyScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                                onOpenGame = { appId -> backStack.add(Routes.StorePage(appId)) },
                            )
                        }
                        entry<Routes.Sessions> {
                            val viewModel = koinViewModel<SessionsViewModel>()
                            SessionsScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                            )
                        }
                        entry<Routes.WebPage> {
                            WebPageScreen(url = it.url, onBack = { backStack.removeLastOrNull() })
                        }
                        entry<Routes.Store> {
                            val viewModel = koinViewModel<StoreViewModel>()
                            StoreScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                                onOpen = { appId -> backStack.add(Routes.StorePage(appId)) },
                            )
                        }
                        entry<Routes.StorePage> {
                            val viewModel =
                                koinViewModel<StorePageViewModel> { parametersOf(it.appId) }
                            StorePageScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                                // The library page links back here, so the two do not pile up.
                                onOpenLibrary = { appId ->
                                    openOrReturnTo(Routes.GameDetail(appId))
                                },
                            )
                        }
                        entry<Routes.Screenshots> {
                            val viewModel = koinViewModel<ScreenshotsViewModel>()
                            ScreenshotsScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                            )
                        }
                        /* Under the Hood: how Steam and JavaSteam work underneath */
                        entry<Routes.NetworkLog> {
                            NetworkLogScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        entry<Routes.UnderTheHood> {
                            UnderTheHoodScreen(
                                onNavDrawerAction = onNavDrawerAction,
                                onOpen = { route -> backStack.add(route) },
                                anonymous = anonymous,
                                // Keeps a saved sign-in: only the anonymous session ends.
                                onLeave = {
                                    connection.steamService?.logOut(clearCredentials = false)
                                },
                            )
                        }
                        entry<Routes.ConnectionInfo> {
                            ConnectionScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        entry<Routes.CmServers> {
                            val viewModel = koinViewModel<CmServersViewModel>()
                            CmServersScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.Licenses> {
                            val viewModel = koinViewModel<LicensesViewModel>()
                            LicensesScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.IdDecoder> {
                            IdDecoderScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        entry<Routes.AuthTicket> {
                            val viewModel = koinViewModel<AuthTicketViewModel>()
                            AuthTicketScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.Diagnostics> {
                            DiagnosticsScreen(onBack = { backStack.removeLastOrNull() })
                        }
                        entry<Routes.PicsFeed> {
                            PicsFeedScreen(
                                onBack = { backStack.removeLastOrNull() },
                                onOpen = { id, isApp ->
                                    backStack.add(Routes.PicsExplorer(id, isApp))
                                },
                            )
                        }
                        entry<Routes.Depots> {
                            val viewModel =
                                koinViewModel<DepotsViewModel> { parametersOf(it.appId) }
                            DepotsScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                                onOpenManifest = { appId, depotId, manifestId, branch ->
                                    backStack.add(
                                        Routes.Manifest(appId, depotId, manifestId, branch),
                                    )
                                },
                            )
                        }
                        entry<Routes.Manifest> {
                            val viewModel = koinViewModel<ManifestViewModel> {
                                parametersOf(it.appId, it.depotId, it.manifestId, it.branch)
                            }
                            ManifestScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.PicsExplorer> {
                            val viewModel = koinViewModel<PicsExplorerViewModel> {
                                parametersOf(it.id, it.isApp)
                            }
                            PicsExplorerScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.WebApi> {
                            val viewModel = koinViewModel<WebApiViewModel>()
                            WebApiScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Routes.DownloadInternals> {
                            val viewModel = koinViewModel<DownloadInternalsViewModel>()
                            DownloadInternalsScreen(
                                viewModel = viewModel,
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        /* Downloads */
                        entry<Routes.Downloads> {
                            val viewModel = koinViewModel<DownloadsViewModel>()
                            DownloadsScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                                onOpenGame = { appId -> openOrReturnTo(Routes.GameDetail(appId)) },
                                onOpenLibrary = {
                                    backStack.removeAll { it != Routes.Home }
                                    backStack.add(Routes.Games)
                                },
                            )
                        }
                        entry<Routes.Settings> {
                            val viewModel = koinViewModel<SettingsViewModel>()
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavDrawerAction = onNavDrawerAction,
                            )
                        }
                    },
                )
            },
        )
    }
}
