package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.ui.screens.achievements.AchievementsViewModel
import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.chatrooms.ChatRoomsViewModel
import `in`.dragonbra.vapulla.ui.screens.cloud.CloudFilesViewModel
import `in`.dragonbra.vapulla.ui.screens.downloads.DownloadsViewModel
import `in`.dragonbra.vapulla.ui.screens.gamedetail.GameDetailViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.groupchat.GroupChatViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import `in`.dragonbra.vapulla.ui.screens.screenshots.ScreenshotsViewModel
import `in`.dragonbra.vapulla.ui.screens.servers.ServerBrowserViewModel
import `in`.dragonbra.vapulla.ui.screens.sessions.SessionsViewModel
import `in`.dragonbra.vapulla.ui.screens.settings.SettingsViewModel
import `in`.dragonbra.vapulla.ui.screens.workshop.WorkshopViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** One view model per screen. Parameters are the friend's Steam ID or the app ID. */
val ViewModelModules = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { GamesViewModel(get(), get(), get(), get()) }
    viewModel { DownloadsViewModel(get(), get()) }
    viewModel { ChatRoomsViewModel(get()) }
    viewModel { ServerBrowserViewModel(get(), get(), get()) }
    viewModel { ScreenshotsViewModel(get()) }
    viewModel { SessionsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
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
            serviceConnection = get(),
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
            connection = get(),
            settingsManager = get(),
        )
    }
}
