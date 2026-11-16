package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.SteamServiceConnection
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.util.NetworkMonitor
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/** App-wide singletons that are not part of the database. */
val AppModules = module {
    single { AccountManager(androidApplication()) }
    single { SettingsManager(androidApplication()) }
    single { NetworkMonitor(androidApplication()) }
    single { SteamServiceConnection(androidApplication()) }
    single { DownloadManager(androidApplication(), get(), get(), get()) }
}
