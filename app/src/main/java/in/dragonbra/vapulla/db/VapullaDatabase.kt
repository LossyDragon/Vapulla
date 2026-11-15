package `in`.dragonbra.vapulla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import `in`.dragonbra.vapulla.db.converters.AppTypeConverters
import `in`.dragonbra.vapulla.db.converters.FriendTypeConverters
import `in`.dragonbra.vapulla.db.converters.LicenseTypeConverters
import `in`.dragonbra.vapulla.db.converters.ListTypeConverter
import `in`.dragonbra.vapulla.db.converters.ProfileTypeConverter
import `in`.dragonbra.vapulla.db.dao.ChatMessageDao
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.dao.EmoticonDao
import `in`.dragonbra.vapulla.db.dao.SteamAppDao
import `in`.dragonbra.vapulla.db.dao.SteamFriendDao
import `in`.dragonbra.vapulla.db.dao.SteamLicenseDao
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.Emoticon
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.db.entity.SteamLicense

/**
 * The app's single Room database. Everything the UI shows is read from here; the service
 * writes into it as Steam sends updates.
 */
@Database(
    entities = [
        SteamFriend::class,
        ChatMessage::class,
        Emoticon::class,
        SteamApp::class,
        SteamLicense::class,
        Download::class,
    ],
    version = 15,
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

        /** Downloads remember the manifests they completed with, to tell when one is outdated. */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `download` ADD COLUMN `manifestIds` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Downloads remember the password of a branch that is behind one. */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `download` ADD COLUMN `branchPassword` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Adds the `download` table; nothing existing changes. */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `download` (" +
                        "`appId` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
                        "`headerUrl` TEXT NOT NULL, `os` TEXT NOT NULL, " +
                        "`depotIds` TEXT NOT NULL, `installDir` TEXT NOT NULL, " +
                        "`totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, " +
                        "`state` TEXT NOT NULL, `message` TEXT, `addedAt` INTEGER NOT NULL, " +
                        "`finishedAt` INTEGER NOT NULL, PRIMARY KEY(`appId`))",
                )
            }
        }

        /**
         * Downloads gain a branch, a language, a verify flag and a workshop ID. The workshop
         * ID joins the primary key, which SQLite can only do by building the table anew.
         *
         * It also adds a `workshopManifest` column, which [MIGRATION_12_13] takes out again.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `download_new` (" +
                        "`appId` INTEGER NOT NULL, `workshopId` INTEGER NOT NULL, " +
                        "`workshopManifest` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `headerUrl` TEXT NOT NULL, `os` TEXT NOT NULL, " +
                        "`depotIds` TEXT NOT NULL, `branch` TEXT NOT NULL, " +
                        "`language` TEXT NOT NULL, `installDir` TEXT NOT NULL, " +
                        "`totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, " +
                        "`state` TEXT NOT NULL, `message` TEXT, `addedAt` INTEGER NOT NULL, " +
                        "`finishedAt` INTEGER NOT NULL, `verify` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`appId`, `workshopId`))",
                )
                db.execSQL(
                    "INSERT INTO `download_new` SELECT `appId`, 0, 0, `name`, `headerUrl`, `os`, " +
                        "`depotIds`, 'public', 'english', `installDir`, `totalBytes`, " +
                        "`downloadedBytes`, `state`, `message`, `addedAt`, `finishedAt`, 0 " +
                        "FROM `download`",
                )
                db.execSQL("DROP TABLE `download`")
                db.execSQL("ALTER TABLE `download_new` RENAME TO `download`")
            }
        }

        /**
         * Drops `workshopManifest`. It let workshop items be downloaded by manifest while
         * JavaSteam turned them away by ID; with that fixed nothing reads it. SQLite on older
         * Android cannot drop a column, so the table is built anew.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `download_new` (" +
                        "`appId` INTEGER NOT NULL, `workshopId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `headerUrl` TEXT NOT NULL, `os` TEXT NOT NULL, " +
                        "`depotIds` TEXT NOT NULL, `branch` TEXT NOT NULL, " +
                        "`language` TEXT NOT NULL, `installDir` TEXT NOT NULL, " +
                        "`totalBytes` INTEGER NOT NULL, `downloadedBytes` INTEGER NOT NULL, " +
                        "`state` TEXT NOT NULL, `message` TEXT, `addedAt` INTEGER NOT NULL, " +
                        "`finishedAt` INTEGER NOT NULL, `verify` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`appId`, `workshopId`))",
                )
                db.execSQL(
                    "INSERT INTO `download_new` SELECT `appId`, `workshopId`, `name`, " +
                        "`headerUrl`, `os`, `depotIds`, `branch`, `language`, `installDir`, " +
                        "`totalBytes`, `downloadedBytes`, `state`, `message`, `addedAt`, " +
                        "`finishedAt`, `verify` FROM `download`",
                )
                db.execSQL("DROP TABLE `download`")
                db.execSQL("ALTER TABLE `download_new` RENAME TO `download`")
            }
        }
    }

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun emoticonDao(): EmoticonDao

    abstract fun steamAppDao(): SteamAppDao

    abstract fun steamLicenseDao(): SteamLicenseDao

    abstract fun downloadDao(): DownloadDao
}
