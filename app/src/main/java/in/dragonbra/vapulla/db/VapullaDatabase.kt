package `in`.dragonbra.vapulla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import `in`.dragonbra.vapulla.db.converters.AppTypeConverters
import `in`.dragonbra.vapulla.db.converters.FriendTypeConverters
import `in`.dragonbra.vapulla.db.converters.LicenseTypeConverters
import `in`.dragonbra.vapulla.db.converters.ListTypeConverter
import `in`.dragonbra.vapulla.db.converters.ProfileTypeConverter
import `in`.dragonbra.vapulla.db.dao.ChatMessageDao
import `in`.dragonbra.vapulla.db.dao.EmoticonDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.db.entity.SteamLicense

/**
 * The app's single Room database. Everything the UI shows is read from here; the service
 * writes into it as Steam sends updates.
 */
@Database(
    entities = [SteamFriend::class, ChatMessage::class, Emoticon::class, SteamApp::class, SteamLicense::class],
    version = 10,
    exportSchema = true,
)
@TypeConverters(
    FriendTypeConverters::class,
    LicenseTypeConverters::class,
    AppTypeConverters::class,
    ListTypeConverter::class,
    ProfileTypeConverter::class,
)
abstract class VapullaDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "vapulla.db"
    }

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun emoticonDao(): EmoticonDao

    abstract fun steamAppDao(): SteamAppDao

    abstract fun steamLicenseDao(): SteamLicenseDao
}
