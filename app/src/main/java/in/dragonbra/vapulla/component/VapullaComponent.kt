package `in`.dragonbra.vapulla.component

import `in`.dragonbra.vapulla.activity.*
import `in`.dragonbra.vapulla.module.AppModule
import `in`.dragonbra.vapulla.module.PresenterModule
import `in`.dragonbra.vapulla.module.RetrofitModule
import `in`.dragonbra.vapulla.module.StorageModule
import `in`.dragonbra.vapulla.service.SteamService
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    PresenterModule::class,
    StorageModule::class,
    RetrofitModule::class])
interface VapullaComponent {
    fun inject(steamService: SteamService)
    fun inject(homeActivity: HomeActivity)
    fun inject(loginActivity: LoginActivity)
    fun inject(chatActivity: ChatActivity)
    fun inject(profileActivity: ProfileActivity)
    fun inject(settingsActivity: SettingsActivity)
}
