package `in`.dragonbra.vapulla.di

import androidx.room.Room
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.db.VapullaDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val DatabaseModules = module {
    single<VapullaDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME,
        ).apply {
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration(true)
            }
        }.build()
    }

    /* DAOs */
    single { get<VapullaDatabase>().steamFriendDao() }
    single { get<VapullaDatabase>().chatMessageDao() }
    single { get<VapullaDatabase>().emoticonDao() }
    single { get<VapullaDatabase>().steamAppDao() }
    single { get<VapullaDatabase>().steamLicenseDao() }
}
