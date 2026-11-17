package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.ui.screens.account.AccountViewModel
import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsViewModel
import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsViewModel
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesViewModel
import `in`.dragonbra.vapulla.ui.screens.downloads.DownloadsViewModel
import `in`.dragonbra.vapulla.ui.screens.family.FamilyViewModel
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.leaderboards.LeaderboardsViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import `in`.dragonbra.vapulla.ui.screens.remote.RemoteClientsViewModel
import `in`.dragonbra.vapulla.ui.screens.screenshots.ScreenshotsViewModel
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserViewModel
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsViewModel
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsViewModel
import `in`.dragonbra.vapulla.ui.screens.store.StorePageViewModel
import `in`.dragonbra.vapulla.ui.screens.store.StoreViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.AuthTicketViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.CmServersViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.DepotsViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.DownloadInternalsViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.LicensesViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.ManifestViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.PicsExplorerViewModel
import `in`.dragonbra.vapulla.ui.screens.underhood.WebApiViewModel
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** One view model per screen. Parameters are the friend's Steam ID or the app ID. */
val ViewModelModules = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { GamesViewModel(get(), get(), get(), get()) }
    viewModel { DownloadsViewModel(get(), get(), get()) }
    viewModel { ChatRoomsViewModel(get()) }
    viewModel { ServerBrowserViewModel(get(), get(), get()) }
    viewModel { ScreenshotsViewModel(get()) }
    viewModel { SessionsViewModel(get()) }
    viewModel { RemoteClientsViewModel(get()) }
    viewModel { AccountViewModel(get(), get(), get()) }
    viewModel { StoreViewModel(get()) }
    viewModel { FamilyViewModel(get()) }
    viewModel { (appId: Int) -> StorePageViewModel(appId, get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CmServersViewModel(get()) }
    viewModel { LicensesViewModel(get(), get()) }
    viewModel { WebApiViewModel() }
    viewModel { AuthTicketViewModel(get(), get()) }
    viewModel { DownloadInternalsViewModel(get(), get(), get()) }
    viewModel { (id: Int, isApp: Boolean) ->
        PicsExplorerViewModel(id, isApp, get(), get(), get())
    }
    viewModel { (appId: Int, depotId: Int, manifestId: Long, branch: String) ->
        ManifestViewModel(appId, depotId, manifestId, branch, get())
    }
    viewModel { (appId: Int) -> DepotsViewModel(appId, get(), get(), get()) }
    viewModel { (friendId: Long) ->
        ChatViewModel(friendId, get(), get(), get(), get(), androidApplication())
    }
    viewModel { (groupId: Long, chatId: Long, groupName: String, roomName: String) ->
        GroupChatViewModel(groupId, chatId, groupName, roomName, get(), get(), androidApplication())
    }
    viewModel { (friendId: Long) -> ProfileViewModel(get(), get(), get(), friendId) }
    viewModel { (appId: Int) ->
        GameDetailViewModel(appId, get(), get(), get(), get(), get(), get())
    }
    viewModel { (appId: Int) -> LeaderboardsViewModel(appId, get(), get()) }
    viewModel { (appId: Int) -> AchievementsViewModel(appId, get(), get()) }
    viewModel { (appId: Int) -> CloudFilesViewModel(appId, get(), androidApplication()) }
    viewModel { (appId: Int) -> WorkshopViewModel(appId, get(), get(), get(), get(), get()) }
}
