package `in`.dragonbra.vapulla.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import `in`.dragonbra.vapulla.data.converters.AppTypeConverters
import `in`.dragonbra.vapulla.data.converters.FriendTypeConverters
import `in`.dragonbra.vapulla.data.converters.LicenseTypeConverters
import `in`.dragonbra.vapulla.data.converters.ListTypeConverter
import `in`.dragonbra.vapulla.data.dao.ChatMessageDao
import `in`.dragonbra.vapulla.data.dao.EmoticonDao
import `in`.dragonbra.vapulla.data.dao.SteamAppDao
import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
import `in`.dragonbra.vapulla.data.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.data.entity.SteamLicense

@Database(
    entities = [SteamFriend::class, ChatMessage::class, Emoticon::class, SteamApp::class, SteamLicense::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(FriendTypeConverters::class, LicenseTypeConverters::class, AppTypeConverters::class, ListTypeConverter::class)
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