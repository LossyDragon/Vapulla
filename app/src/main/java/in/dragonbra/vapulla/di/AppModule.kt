package `in`.dragonbra.vapulla.di

import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceManager
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single<VapullaDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME
        ).build()
    }

    // Account Manager
    single<AccountManager> { AccountManager(androidApplication()) }

    // Service Manager. VM <~> Service Communication
    single { ServiceManager(androidApplication()) }

    viewModel {
        LoginViewModel(get())
    }
}