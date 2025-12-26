package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.ServiceConnection
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val AppModules = module {
    single { AccountManager(androidApplication()) }
    single { ServiceConnection(androidApplication()) }
}
