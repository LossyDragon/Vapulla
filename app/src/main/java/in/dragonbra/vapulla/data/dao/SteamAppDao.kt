package `in`.dragonbra.vapulla.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.data.entity.SteamApp

@Dao
interface SteamAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: SteamApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: List<SteamApp>)

    @Update
    suspend fun update(app: SteamApp)

    @Query(
        "SELECT * FROM steam_app " +
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
    ): PagingSource<Int, SteamApp>

    @Query("SELECT * FROM steam_app WHERE id = :appId")
    suspend fun findApp(appId: Int): SteamApp?

    @Query("SELECT * FROM steam_app WHERE id = :appId")
    fun findBlocking(appId: Int): SteamApp?
}
