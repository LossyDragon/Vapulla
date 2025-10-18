package `in`.dragonbra.vapulla.di

import androidx.room.Room
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import `in`.dragonbra.vapulla.ui.screens.home.HomeViewModel
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    /* Database */
    single<VapullaDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME
        ).apply {
            fallbackToDestructiveMigration(true)
        }.build()
    }

    single { AccountManager(androidApplication()) }
    single { ServiceConnection(androidApplication()) }

    /* ViewModels */
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
}