package `in`.dragonbra.vapulla.di

import androidx.room.Room
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.db.VapullaDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/** The Room database and one singleton per DAO. */
val DatabaseModules = module {
    single<VapullaDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            VapullaDatabase::class.java,
            VapullaDatabase.DATABASE_NAME,
        ).apply {
            // Debug builds recreate the database on a schema bump instead of migrating.
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration(true)
            }
        }.build()
    }

    single { get<VapullaDatabase>().steamFriendDao() }
    single { get<VapullaDatabase>().chatMessageDao() }
    single { get<VapullaDatabase>().emoticonDao() }
    single { get<VapullaDatabase>().steamAppDao() }
    single { get<VapullaDatabase>().steamLicenseDao() }
}
