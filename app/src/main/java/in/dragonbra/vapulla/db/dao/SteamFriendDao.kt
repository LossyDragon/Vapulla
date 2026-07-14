package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import kotlinx.coroutines.flow.Flow

@Dao
interface SteamFriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: SteamFriend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<SteamFriend>)

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    suspend fun find(id: Long): SteamFriend?

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    fun findFlow(id: Long): Flow<SteamFriend?>

    @Update
    suspend fun update(friend: SteamFriend)

    @Update
    suspend fun update(list: List<SteamFriend>)

    @Update
    suspend fun updateAll(friends: List<SteamFriend>)

    @Query("SELECT * FROM steam_friend")
    fun getFriendsFlow(): Flow<List<SteamFriend>>

    @Query("UPDATE steam_friend SET nickname = '' WHERE nickname != '' AND id NOT IN (:ids)")
    suspend fun clearNicknamesExcept(ids: List<Long>)

    @Query("SELECT * FROM steam_friend WHERE gameAppID > 0")
    suspend fun findFriendsInGame(): List<SteamFriend>

    @Delete
    suspend fun remove(list: List<SteamFriend>)

    @Delete
    suspend fun remove(friend: SteamFriend)

    @Query("DELETE FROM steam_friend WHERE id = :friendId")
    suspend fun remove(friendId: Long)

    @Query("DELETE FROM steam_friend")
    suspend fun delete()
}
