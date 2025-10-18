package `in`.dragonbra.vapulla.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.data.entity.SteamApp

@Dao
interface SteamAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: SteamApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apps: List<SteamApp>)

    @Query("SELECT * FROM steam_app WHERE id = :id")
    suspend fun find(id: Int): SteamApp?

    @Query("SELECT * FROM steam_app WHERE id = :id")
    fun findBlocking(id: Int): SteamApp?

    @Query("SELECT * FROM steam_app WHERE id IN (:appIds)")
    suspend fun find(appIds: List<Int>): List<SteamApp>
}