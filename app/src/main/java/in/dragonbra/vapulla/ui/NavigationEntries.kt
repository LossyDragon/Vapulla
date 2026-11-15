package `in`.dragonbra.vapulla.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import `in`.dragonbra.vapulla.service.SteamService
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
import `in`.dragonbra.vapulla.ui.screens.web.WebPageScreen
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopScreen
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/*
 * The screen behind each of the [Routes], one function per part of the app. Each creates the
 * screen's view model and turns what the screen asks for into changes of the back stack.
 */

/**
 * Two screens that link to each other, such as a chat and its profile, would pile up on the
 * back stack. Going to the screen we just came from pops back to it instead.
 */
private fun SnapshotStateList<Routes>.openOrReturnTo(route: Routes) {
    if (getOrNull(lastIndex - 1) == route) {
        removeLastOrNull()
    } else {
        add(route)
    }
}

/** Sign-in, the friends list, chats and profiles. */
internal fun EntryProviderScope<Routes>.friendsEntries(
    backStack: SnapshotStateList<Routes>,
    onBack: () -> Unit,
    onNavDrawerAction: () -> Unit,
) {
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
    entry<Routes.Home> {
        val viewModel = koinViewModel<HomeViewModel>()
        HomeScreen(
            viewModel = viewModel,
            onNavDrawerAction = onNavDrawerAction,
            onOpenChat = { friendId -> backStack.add(Routes.FriendChat(friendId)) },
            onOpenProfile = { friendId -> backStack.add(Routes.FriendProfile(friendId)) },
        )
    }
    entry<Routes.FriendChat> {
        val viewModel = koinViewModel<ChatViewModel> { parametersOf(it.id) }
        ChatScreen(
            viewModel = viewModel,
            onBack = onBack,
            onProfile = { backStack.openOrReturnTo(Routes.FriendProfile(it.id)) },
        )
    }
    entry<Routes.FriendProfile> {
        val viewModel = koinViewModel<ProfileViewModel> { parametersOf(it.id) }
        ProfileScreen(
            viewModel = viewModel,
            onBack = onBack,
            onChat = { friendId -> backStack.openOrReturnTo(Routes.FriendChat(friendId)) },
        )
    }
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
            onBack = onBack,
            // Replaces this entry rather than adding one, so Back always returns to Group
            // Chats however many channels were visited.
            onSwitchChannel = { room ->
                backStack[backStack.lastIndex] = it.copy(
                    chatId = room.chatId,
                    roomName = room.name,
                )
            },
        )
    }
}

/** The library and what hangs off a game, the store, screenshots and downloads. */
internal fun EntryProviderScope<Routes>.libraryEntries(
    backStack: SnapshotStateList<Routes>,
    onBack: () -> Unit,
    onNavDrawerAction: () -> Unit,
) {
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
        val viewModel = koinViewModel<GameDetailViewModel> { parametersOf(it.appId) }
        GameDetailScreen(
            viewModel = viewModel,
            onBack = onBack,
            onOpenCloud = { appId -> backStack.add(Routes.CloudFiles(appId)) },
            onOpenWorkshop = { appId -> backStack.add(Routes.Workshop(appId)) },
            onOpenAchievements = { appId -> backStack.add(Routes.Achievements(appId)) },
            // Downloads and the store page link back here, so going back and forth pops
            // instead of piling up.
            onOpenDownloads = { backStack.openOrReturnTo(Routes.Downloads) },
            onOpenDepots = { appId -> backStack.add(Routes.Depots(appId)) },
            onOpenStore = { appId -> backStack.openOrReturnTo(Routes.StorePage(appId)) },
            onOpenLeaderboards = { appId -> backStack.add(Routes.Leaderboards(appId)) },
        )
    }
    entry<Routes.CloudFiles> {
        val viewModel = koinViewModel<CloudFilesViewModel> { parametersOf(it.appId) }
        CloudFilesScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.Workshop> {
        val viewModel = koinViewModel<WorkshopViewModel> { parametersOf(it.appId) }
        WorkshopScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.Leaderboards> {
        val viewModel = koinViewModel<LeaderboardsViewModel> { parametersOf(it.appId) }
        LeaderboardsScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.Achievements> {
        val viewModel = koinViewModel<AchievementsViewModel> { parametersOf(it.appId) }
        AchievementsScreen(viewModel = viewModel, onBack = onBack)
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
        val viewModel = koinViewModel<StorePageViewModel> { parametersOf(it.appId) }
        StorePageScreen(
            viewModel = viewModel,
            onBack = onBack,
            // The library page links back here, so the two do not pile up.
            onOpenLibrary = { appId -> backStack.openOrReturnTo(Routes.GameDetail(appId)) },
        )
    }
    entry<Routes.Screenshots> {
        val viewModel = koinViewModel<ScreenshotsViewModel>()
        ScreenshotsScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
    }
    entry<Routes.Downloads> {
        val viewModel = koinViewModel<DownloadsViewModel>()
        DownloadsScreen(
            viewModel = viewModel,
            onNavDrawerAction = onNavDrawerAction,
            onOpenGame = { appId -> backStack.openOrReturnTo(Routes.GameDetail(appId)) },
            onOpenLibrary = {
                backStack.removeAll { it != Routes.Home }
                backStack.add(Routes.Games)
            },
        )
    }
}

/** The account's own screens, the server browser, web pages and the settings. */
internal fun EntryProviderScope<Routes>.accountEntries(
    backStack: SnapshotStateList<Routes>,
    onBack: () -> Unit,
    onNavDrawerAction: () -> Unit,
) {
    entry<Routes.ServerBrowser> {
        val viewModel = koinViewModel<ServerBrowserViewModel>()
        ServerBrowserScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
    }
    entry<Routes.Account> {
        val viewModel = koinViewModel<AccountViewModel>()
        AccountScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
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
        SessionsScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
    }
    entry<Routes.WebPage> {
        WebPageScreen(url = it.url, onBack = onBack)
    }
    entry<Routes.Settings> {
        val viewModel = koinViewModel<SettingsViewModel>()
        SettingsScreen(viewModel = viewModel, onNavDrawerAction = onNavDrawerAction)
    }
}

/**
 * Under the Hood: how Steam and JavaSteam work underneath.
 *
 * @param onLeaveAnonymous ends an anonymous session, of which these screens are the whole app.
 */
internal fun EntryProviderScope<Routes>.underHoodEntries(
    backStack: SnapshotStateList<Routes>,
    onBack: () -> Unit,
    onNavDrawerAction: () -> Unit,
    onLeaveAnonymous: () -> Unit,
) {
    entry<Routes.UnderTheHood> {
        val anonymous by SteamService.isAnonymous.collectAsStateWithLifecycle()
        UnderTheHoodScreen(
            onNavDrawerAction = onNavDrawerAction,
            onOpen = { route -> backStack.add(route) },
            anonymous = anonymous,
            onLeave = onLeaveAnonymous,
        )
    }
    entry<Routes.NetworkLog> {
        NetworkLogScreen(onBack = onBack)
    }
    entry<Routes.ConnectionInfo> {
        ConnectionScreen(onBack = onBack)
    }
    entry<Routes.CmServers> {
        val viewModel = koinViewModel<CmServersViewModel>()
        CmServersScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.Licenses> {
        val viewModel = koinViewModel<LicensesViewModel>()
        LicensesScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.IdDecoder> {
        IdDecoderScreen(onBack = onBack)
    }
    entry<Routes.AuthTicket> {
        val viewModel = koinViewModel<AuthTicketViewModel>()
        AuthTicketScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.Diagnostics> {
        DiagnosticsScreen(onBack = onBack)
    }
    entry<Routes.PicsFeed> {
        PicsFeedScreen(
            onBack = onBack,
            onOpen = { id, isApp -> backStack.add(Routes.PicsExplorer(id, isApp)) },
        )
    }
    entry<Routes.Depots> {
        val viewModel = koinViewModel<DepotsViewModel> { parametersOf(it.appId) }
        DepotsScreen(
            viewModel = viewModel,
            onBack = onBack,
            onOpenManifest = { appId, depotId, manifestId, branch ->
                backStack.add(Routes.Manifest(appId, depotId, manifestId, branch))
            },
        )
    }
    entry<Routes.Manifest> {
        val viewModel = koinViewModel<ManifestViewModel> {
            parametersOf(it.appId, it.depotId, it.manifestId, it.branch)
        }
        ManifestScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.PicsExplorer> {
        val viewModel = koinViewModel<PicsExplorerViewModel> { parametersOf(it.id, it.isApp) }
        PicsExplorerScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.WebApi> {
        val viewModel = koinViewModel<WebApiViewModel>()
        WebApiScreen(viewModel = viewModel, onBack = onBack)
    }
    entry<Routes.DownloadInternals> {
        val viewModel = koinViewModel<DownloadInternalsViewModel>()
        DownloadInternalsScreen(viewModel = viewModel, onBack = onBack)
    }
}
