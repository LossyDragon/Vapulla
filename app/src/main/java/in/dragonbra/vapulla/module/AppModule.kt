package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.retrofit.StoreFront
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context) =
        NotificationManagerCompat.from(context)

    @Provides
    @Singleton
    fun provideGameSchemaManager(gameSchemaDao: GameSchemaDao, storeFront: StoreFront) =
        GameSchemaManager(gameSchemaDao, storeFront)

    @Provides
    @Singleton
    fun provideLevelManager(steamApi: SteamApi) =
        ProfileManager(steamApi)
}
