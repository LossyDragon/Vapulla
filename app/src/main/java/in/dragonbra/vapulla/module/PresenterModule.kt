package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.presenter.HomePresenter
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PresenterModule {

    @Provides
    @Singleton
    fun provideHomePresenter(
        context: Context,
        steamFriendDao: SteamFriendDao,
        account: AccountManager
    ) = HomePresenter(context, steamFriendDao, account)

    @Provides
    @Singleton
    fun provideLoginPresenter(context: Context) =
        LoginPresenter(context)
}
