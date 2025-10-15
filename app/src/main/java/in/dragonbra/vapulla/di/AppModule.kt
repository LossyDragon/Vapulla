package `in`.dragonbra.vapulla.di

import `in`.dragonbra.vapulla.service.ServiceManager
import `in`.dragonbra.vapulla.ui.screens.login.LoginViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ServiceManager(androidApplication()) } // VM <~> Service Communication
    viewModel {
        LoginViewModel(get())
    }
}