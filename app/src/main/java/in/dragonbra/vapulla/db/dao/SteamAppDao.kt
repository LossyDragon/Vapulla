package `in`.dragonbra.vapulla.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamAppChangeInfo
import `in`.dragonbra.vapulla.db.entity.SteamAppListItem
import `in`.dragonbra.vapulla.db.entity.SteamAppName
import kotlinx.coroutines.flow.Flow

/**
 * The apps Steam has told the app about. Not all of them are owned: an app no license grants
 * has [SteamApp.NO_PACKAGE] as its package, which is what the library queries leave out.
 */
@Dao
interface SteamAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: List<SteamApp>)

    /**
     * Inserts stub rows for apps that are not yet known. Existing rows are left untouched.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(apps: List<SteamApp>)

    @Update
    suspend fun update(app: SteamApp)

    @Query("UPDATE steam_app SET packageId = :packageId WHERE id IN (:appIds)")
    suspend fun updatePackageId(packageId: Int, appIds: List<Int>)

    /** The apps whose name contains [query], by name; only the first [limit] of them. */
    @Query(
        "SELECT id, name FROM steam_app WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY LOWER(name) LIMIT :limit",
    )
    suspend fun searchNames(query: String, limit: Int): List<SteamAppName>

    @Query("DELETE FROM steam_app")
    suspend fun deleteAll()

    /** The apps a package grants, as far as the table knows. */
    @Query("SELECT id FROM steam_app WHERE packageId = :packageId")
    suspend fun findAppIdsOfPackage(packageId: Int): List<Int>

    @Query(
        "UPDATE steam_app SET playtimeForever = :forever, playtimeTwoWeeks = :twoWeeks, " +
            "lastPlayed = :lastPlayed WHERE id = :appId",
    )
    suspend fun updatePlaytime(appId: Int, forever: Int, twoWeeks: Int, lastPlayed: Long)

    /** Full rows of a few apps as a flow; meant for a handful, as the rows are large. */
    @Query("SELECT * FROM steam_app WHERE id IN (:appIds)")
    fun findAppsFlow(appIds: List<Int>): Flow<List<SteamApp>>

    /** Full row as a flow, for the game detail screen. */
    @Query("SELECT * FROM steam_app WHERE id = :appId")
    fun findAppFlow(appId: Int): Flow<SteamApp?>

    /**
     * The library: owned apps of the kinds in [appType] (bits of [SteamApp.AppType]) whose
     * name contains [query], alphabetical. Spacewar (480), Steamworks' test app that every
     * account has, is left out.
     *
     * [within] narrows it to some apps, such as those of a collection: their IDs with a comma
     * before and after each, e.g. ",440,620,"; empty for all. Written as text because a list
     * of IDs would be one SQL variable each, and SQLite takes only so many.
     *
     * Only the columns rendered by the games list are selected so a page load (which re-runs
     * on every invalidation of `steam_app`) never has to decode the JSON columns of [SteamApp].
     */
    @Query(
        "SELECT id, name, type, developer, publisher, releaseDate, metacriticScore, " +
            "clientIconHash, ownerAccountId, headerImage, playtimeForever, lastPlayed " +
            "FROM steam_app " +
            "WHERE id != 480 " +
            "AND packageId != ${SteamApp.NO_PACKAGE} " +
            "AND (type & :appType) != 0 " +
            "AND (:query = '' OR name LIKE '%' || :query || '%') " +
            "AND (:within = '' OR instr(:within, ',' || id || ',') > 0) " +
            "ORDER BY LOWER(name)",
    )
    fun getAllOwnedAppsPaged(
        appType: Int,
        query: String = "",
        within: String = "",
    ): PagingSource<Int, SteamAppListItem>

    /**
     * Owned games that register servers with Steam, alphabetical. The Steam client keys
     * on `extended/gamedir`: it is set for every game that uses the master server and left
     * at the Steamworks placeholder `ValveTestApp<id>` for the rest. `noservers` hides the few
     * exceptions. The same projection as the library list keeps decoding cheap.
     */
    @Query(
        "SELECT id, name, type, developer, publisher, releaseDate, metacriticScore, " +
            "clientIconHash, ownerAccountId, headerImage, playtimeForever, lastPlayed " +
            "FROM steam_app " +
            "WHERE receivedPICS = 1 AND noServers = 0 AND (type & :gameType) != 0 " +
            "AND gameDir != '' AND gameDir NOT LIKE 'ValveTestApp%' " +
            "AND packageId != ${SteamApp.NO_PACKAGE} " +
            "AND (:query = '' OR name LIKE '%' || :query || '%') " +
            "ORDER BY LOWER(name)",
    )
    fun findServerGames(
        query: String = "",
        gameType: Int = SteamApp.AppType.game.code,
    ): Flow<List<SteamAppListItem>>

    @Query("SELECT * FROM steam_app WHERE id = :appId")
    suspend fun findApp(appId: Int): SteamApp?

    @Query("SELECT name FROM steam_app WHERE id = :appId")
    suspend fun findAppName(appId: Int): String?

    /** Names of the given apps that have one. Callers should chunk [appIds] like below. */
    @Query("SELECT id, name FROM steam_app WHERE id IN (:appIds) AND name != ''")
    suspend fun findNames(appIds: List<Int>): List<SteamAppName>

    /** The name of every app that has one, for labelling lists of app IDs. */
    @Query("SELECT id, name FROM steam_app WHERE name != ''")
    suspend fun getAllNames(): List<SteamAppName>

    /**
     * Batched lookup of the few columns needed to detect PICS changes. Callers should chunk
     * [appIds] to stay well under SQLite's bound-parameter limit.
     */
    @Query("SELECT id, packageId, lastChangeNumber FROM steam_app WHERE id IN (:appIds)")
    suspend fun findChangeInfo(appIds: List<Int>): List<SteamAppChangeInfo>

    /** The IDs of the games the account owns, the library without Spacewar (480). */
    @Query(
        "SELECT id FROM steam_app WHERE id != 480 AND packageId != ${SteamApp.NO_PACKAGE} " +
            "AND (type & ${SteamApp.GAME_TYPE}) != 0",
    )
    suspend fun ownedGameIds(): List<Int>

    /** Licensed apps whose app info never arrived, e.g. because an ingest was interrupted. */
    @Query(
        "SELECT id FROM steam_app WHERE receivedPICS = 0 AND packageId != ${SteamApp.NO_PACKAGE}",
    )
    suspend fun findAppsAwaitingPics(): List<Int>
}
