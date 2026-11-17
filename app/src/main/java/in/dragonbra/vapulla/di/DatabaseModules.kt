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
        ).addMigrations(*VapullaDatabase.ALL_MIGRATIONS).apply {
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
    single { get<VapullaDatabase>().downloadDao() }
}
