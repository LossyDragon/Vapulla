package `in`.dragonbra.vapulla.module

import `in`.dragonbra.vapulla.retrofit.Imgur
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.retrofit.StoreFront
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val BASE_STEAM_API_URL = "https://api.steampowered.com/"
private const val BASE_STEAM_STORE_URL = "http://store.steampowered.com/api/"
private const val BASE_IMGUR_URL = "https://api.imgur.com/"

@InstallIn(SingletonComponent::class)
@Module
object RetrofitModule {

    @Provides
    @Singleton
    fun provideStoreFront(): StoreFront {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_STEAM_STORE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(StoreFront::class.java)
    }

    @Provides
    @Singleton
    fun provideSteamApi(): SteamApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_STEAM_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SteamApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImgur(): Imgur {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_IMGUR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(Imgur::class.java)
    }
}
