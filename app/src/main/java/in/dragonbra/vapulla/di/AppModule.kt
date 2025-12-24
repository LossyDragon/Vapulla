package `in`.dragonbra.vapulla.di

import androidx.room.Room
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.screens.games.GamesViewModel
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import `in`.dragonbra.vapulla.ui.screens.profile.ProfileViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    /* Database */
    single<VapullaDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME,
        ).apply {
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration(true)
            }
        }.build()
    }

    /* DAOs */
    single { get<VapullaDatabase>().steamFriendDao() }
    single { get<VapullaDatabase>().chatMessageDao() }
    single { get<VapullaDatabase>().emoticonDao() }
    single { get<VapullaDatabase>().steamAppDao() }
    single { get<VapullaDatabase>().steamLicenseDao() }

    /* ViewModels */
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { GamesViewModel(get(), get()) }
    viewModel { (friendId: Long) ->
        ProfileViewModel(
            friendDao = get(),
            serviceConnection = get(),
            friendId = friendId,
        )
    }

    /* Other */
    single { AccountManager(androidApplication()) }
    single { ServiceConnection(androidApplication()) }
}
