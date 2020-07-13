package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.data.dao.GameSchemaDao
import `in`.dragonbra.vapulla.manager.GameSchemaManager
import `in`.dragonbra.vapulla.manager.ProfileManager
import `in`.dragonbra.vapulla.retrofit.Imgur
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.retrofit.StoreFront
import `in`.dragonbra.vapulla.service.ImgurAuthService
import android.content.ClipboardManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule(val context: Context) {

    @Provides
    @Singleton
    fun provideContext() = context

    @Provides
    @Singleton
    fun provideNotificationManager(context: Context) =
            NotificationManagerCompat.from(context)

    @Provides
    @Singleton
    fun provideClipboardManager(context: Context) =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideGameSchemaManager(gameSchemaDao: GameSchemaDao, storeFront: StoreFront) =
            GameSchemaManager(gameSchemaDao, storeFront)

    @Provides
    @Singleton
    fun provideImgurAuthService(imgur: Imgur) =
            ImgurAuthService(context, imgur)

    @Provides
    @Singleton
    fun provideLevelManager(steamApi: SteamApi) =
            ProfileManager(steamApi)
}
