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
    viewModel { (appId: Int) ->
        StorePageViewModel(
            appId = appId,
            connection = get(),
            appDao = get(),
        )
    }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { CmServersViewModel(get()) }
    viewModel { LicensesViewModel(get(), get()) }
    viewModel { WebApiViewModel() }
    viewModel { AuthTicketViewModel(get(), get()) }
    viewModel { DownloadInternalsViewModel(get(), get(), get()) }
    viewModel { (id: Int, isApp: Boolean) ->
        PicsExplorerViewModel(
            initialId = id,
            initialIsApp = isApp,
            connection = get(),
            appDao = get(),
            licenseDao = get(),
        )
    }
    viewModel { (appId: Int, depotId: Int, manifestId: Long, branch: String) ->
        ManifestViewModel(
            appId = appId,
            depotId = depotId,
            manifestId = manifestId,
            branch = branch,
            connection = get(),
        )
    }
    viewModel { (appId: Int) ->
        DepotsViewModel(
            initialAppId = appId,
            appDao = get(),
            licenseDao = get(),
            connection = get(),
        )
    }
    viewModel { (friendId: Long) ->
        ChatViewModel(
            friendId = friendId,
            friendDao = get(),
            messageDao = get(),
            emoticonDao = get(),
            connection = get(),
            application = androidApplication(),
        )
    }
    viewModel { (groupId: Long, chatId: Long, groupName: String, roomName: String) ->
        GroupChatViewModel(
            groupId = groupId,
            chatId = chatId,
            groupName = groupName,
            roomName = roomName,
            emoticonDao = get(),
            connection = get(),
            application = androidApplication(),
        )
    }
    viewModel { (friendId: Long) ->
        ProfileViewModel(
            friendDao = get(),
            appDao = get(),
            connection = get(),
            friendId = friendId,
        )
    }
    viewModel { (appId: Int) ->
        GameDetailViewModel(
            appId = appId,
            appDao = get(),
            downloadDao = get(),
            licenseDao = get(),
            connection = get(),
            downloads = get(),
            friendDao = get(),
        )
    }
    viewModel { (appId: Int) ->
        LeaderboardsViewModel(
            appId = appId,
            connection = get(),
            appDao = get(),
        )
    }
    viewModel { (appId: Int) ->
        AchievementsViewModel(
            appId = appId,
            appDao = get(),
            connection = get(),
        )
    }
    viewModel { (appId: Int) ->
        CloudFilesViewModel(
            appId = appId,
            connection = get(),
            application = androidApplication(),
        )
    }
    viewModel { (appId: Int) ->
        WorkshopViewModel(
            appId = appId,
            appDao = get(),
            connection = get(),
            settingsManager = get(),
            downloadDao = get(),
            downloads = get(),
        )
    }
}
