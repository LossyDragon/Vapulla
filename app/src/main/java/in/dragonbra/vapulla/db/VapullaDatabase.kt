package `in`.dragonbra.vapulla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import `in`.dragonbra.vapulla.db.converters.AppTypeConverters
import `in`.dragonbra.vapulla.db.converters.DateTypeConverter
import `in`.dragonbra.vapulla.db.converters.FriendTypeConverters
import `in`.dragonbra.vapulla.db.converters.LicenseTypeConverters
import `in`.dragonbra.vapulla.db.converters.ListTypeConverter
import `in`.dragonbra.vapulla.db.converters.ReactionTypeConverter
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
    version = 17,
    exportSchema = true,
)
@TypeConverters(
    FriendTypeConverters::class,
    LicenseTypeConverters::class,
    AppTypeConverters::class,
    ListTypeConverter::class,
    DateTypeConverter::class,
    ReactionTypeConverter::class,
)
abstract class VapullaDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "vapulla.db"

        /** Adds the `download` table; nothing existing changes. */
        private val MIGRATION_10_11 = object : Migration(10, 11) {
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
        private val MIGRATION_11_12 = object : Migration(11, 12) {
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
        private val MIGRATION_12_13 = object : Migration(12, 13) {
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

        /** Downloads remember the manifests they completed with, to tell when one is outdated. */
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `download` ADD COLUMN `manifestIds` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Downloads remember the password of a branch that is behind one. */
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `download` ADD COLUMN `branchPassword` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /**
         * Drops the columns nothing read: most of the PICS app info on `steam_app`, and the
         * game data blob, typing time and last message on `steam_friend`.
         */
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.rebuildTable(
                    table = "steam_app",
                    columns = listOf(
                        "`id` INTEGER NOT NULL",
                        "`packageId` INTEGER NOT NULL",
                        "`ownerAccountId` TEXT NOT NULL",
                        "`licenseFlags` INTEGER NOT NULL",
                        "`receivedPICS` INTEGER NOT NULL",
                        "`lastChangeNumber` INTEGER NOT NULL",
                        "`playtimeForever` INTEGER NOT NULL",
                        "`playtimeTwoWeeks` INTEGER NOT NULL",
                        "`lastPlayed` INTEGER NOT NULL",
                        "`depots` TEXT NOT NULL",
                        "`branches` TEXT NOT NULL",
                        "`name` TEXT NOT NULL",
                        "`type` INTEGER NOT NULL",
                        "`osList` INTEGER NOT NULL",
                        "`releaseState` INTEGER NOT NULL",
                        "`releaseDate` INTEGER NOT NULL",
                        "`metacriticScore` INTEGER NOT NULL",
                        "`clientIconHash` TEXT NOT NULL",
                        "`headerImage` TEXT NOT NULL",
                        "`reviewPercentage` INTEGER NOT NULL",
                        "`controllerSupport` INTEGER NOT NULL",
                        "`workshopVisible` INTEGER NOT NULL",
                        "`developer` TEXT NOT NULL",
                        "`publisher` TEXT NOT NULL",
                        "`isFreeApp` INTEGER NOT NULL",
                        "`gameDir` TEXT NOT NULL",
                        "`noServers` INTEGER NOT NULL",
                        "`installDir` TEXT NOT NULL",
                        "`ufs` TEXT NOT NULL",
                    ),
                )
                db.rebuildTable(
                    table = "steam_friend",
                    columns = listOf(
                        "`id` INTEGER NOT NULL",
                        "`name` TEXT NOT NULL",
                        "`avatar` TEXT",
                        "`relation` INTEGER NOT NULL",
                        "`state` INTEGER NOT NULL",
                        "`gameAppID` INTEGER NOT NULL",
                        "`gameID` INTEGER NOT NULL",
                        "`gameName` TEXT NOT NULL",
                        "`richPresence` TEXT NOT NULL",
                        "`lastLogOn` INTEGER NOT NULL",
                        "`lastLogOff` INTEGER NOT NULL",
                        "`stateFlags` INTEGER NOT NULL",
                        "`statusFlags` INTEGER NOT NULL",
                        "`nickname` TEXT NOT NULL",
                        "`lastMessageTime` INTEGER NOT NULL",
                        "`newMessageCount` INTEGER NOT NULL",
                        "`aliases` TEXT NOT NULL",
                    ),
                )
            }
        }

        /**
         * Chat messages gain Steam's ordinal, which with the time names a message, and the
         * reactions given to them. Messages already stored get ordinal 0 until the chat's
         * history is next fetched.
         */
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `chat_message` ADD COLUMN `ordinal` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE `chat_message` ADD COLUMN `reactions` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Every migration there is, oldest first, for the database builder. */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
        )

        /**
         * Rebuilds [table] with only [columns], each given as its definition (the name in
         * backticks, then its type), and `id` as the primary key. The values of the columns
         * that stay are copied across. SQLite on older Android cannot drop a column, so this
         * is how one goes.
         */
        private fun SupportSQLiteDatabase.rebuildTable(table: String, columns: List<String>) {
            val names = columns.joinToString { it.substringBefore(' ') }

            execSQL(
                "CREATE TABLE `${table}_new` (${columns.joinToString()}, PRIMARY KEY(`id`))",
            )
            execSQL("INSERT INTO `${table}_new` ($names) SELECT $names FROM `$table`")
            execSQL("DROP TABLE `$table`")
            execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
        }
    }

    abstract fun steamFriendDao(): SteamFriendDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun emoticonDao(): EmoticonDao

    abstract fun steamAppDao(): SteamAppDao

    abstract fun steamLicenseDao(): SteamLicenseDao

    abstract fun downloadDao(): DownloadDao
}
