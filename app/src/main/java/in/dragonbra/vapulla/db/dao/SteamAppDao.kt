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

@Dao
interface SteamAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: SteamApp)

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

    @Query(
        "UPDATE steam_app SET playtimeForever = :forever, playtimeTwoWeeks = :twoWeeks, " +
            "lastPlayed = :lastPlayed WHERE id = :appId",
    )
    suspend fun updatePlaytime(appId: Int, forever: Int, twoWeeks: Int, lastPlayed: Long)

    /** Full row as a flow, for the game detail screen. */
    @Query("SELECT * FROM steam_app WHERE id = :appId")
    fun findAppFlow(appId: Int): Flow<SteamApp?>

    /**
     * Paged list projection. Only the columns rendered by the games list are selected so a
     * page load (which re-runs on every invalidation of `steam_app`) never has to decode the
     * large JSON columns of [SteamApp].
     */
    @Query(
        "SELECT id, name, type, developer, publisher, releaseDate, metacriticScore, " +
            "clientIconHash, ownerAccountId, headerImage, playtimeForever, lastPlayed " +
            "FROM steam_app " +
            "WHERE id != 480 " +
            "AND packageId != :invalidPkgId " +
            "AND type != 0 " +
            "AND (type & :appType) != 0 " +
            "AND (:query = '' OR LOWER(name) LIKE '%' || LOWER(:query) || '%') " +
            "ORDER BY LOWER(name)",
    )
    fun getAllOwnedAppsPaged(
        appType: Int,
        query: String = "",
        invalidPkgId: Int = Int.MAX_VALUE,
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
            "AND packageId != :invalidPkgId " +
            "AND (:query = '' OR LOWER(name) LIKE '%' || LOWER(:query) || '%') " +
            "ORDER BY LOWER(name)",
    )
    fun findServerGames(
        query: String = "",
        gameType: Int = SteamApp.AppType.game.code,
        invalidPkgId: Int = Int.MAX_VALUE,
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

    /** Licensed apps whose app info never arrived, e.g. because an ingest was interrupted. */
    @Query("SELECT id FROM steam_app WHERE receivedPICS = 0 AND packageId != :invalidPkgId")
    suspend fun findAppsAwaitingPics(invalidPkgId: Int = Int.MAX_VALUE): List<Int>

    @Query("SELECT * FROM steam_app WHERE id = :appId")
    fun findBlocking(appId: Int): SteamApp?
}
