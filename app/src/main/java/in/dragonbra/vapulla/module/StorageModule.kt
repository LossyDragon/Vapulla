package `in`.dragonbra.vapulla.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object StorageModule {

    @Provides
    @Singleton
    fun provideAccountManager(@ApplicationContext context: Context) = AccountManager(context)

    @Provides
    @Singleton
    fun provideVapullaDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(
            context,
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideSteamFriendDao(db: VapullaDatabase) = db.steamFriendDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(db: VapullaDatabase) = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideGameSchemaDao(db: VapullaDatabase) = db.gameSchemaDao()

    @Provides
    @Singleton
    fun provideEmoticonDao(db: VapullaDatabase) = db.emoticonDao()
}
