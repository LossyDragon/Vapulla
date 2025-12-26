package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.ui.screens.chat.ChatViewModel
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val ViewModelModules = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { GamesViewModel(get(), get()) }
    viewModel { (friendId: Long) ->
        ChatViewModel(
            friendId = friendId,
            friendDao = get(),
            messageDao = get(),
        )
    }
    viewModel { (friendId: Long) ->
        ProfileViewModel(
            friendDao = get(),
            serviceConnection = get(),
            friendId = friendId,
        )
    }
}
