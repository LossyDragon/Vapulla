package `in`.dragonbra.vapulla.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import `in`.dragonbra.vapulla.service.ServiceConnection
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ServiceConnectionModule {

    @Provides
    @Singleton
    fun provideServiceConnection() = ServiceConnection()
}