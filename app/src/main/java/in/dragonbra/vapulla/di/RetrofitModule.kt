package `in`.dragonbra.vapulla.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.retrofit.StoreFront
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@InstallIn(SingletonComponent::class)
@Module
object RetrofitModule {

    @Provides
    @Singleton
    fun provideStoreFront(): StoreFront {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_STEAM_STORE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(StoreFront::class.java)
    }

    @Provides
    @Singleton
    fun provideSteamApi(): SteamApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_STEAM_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SteamApi::class.java)
    }
}
