package `in`.dragonbra.vapulla.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import kotlinx.coroutines.flow.Flow

@Dao
interface SteamFriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: SteamFriend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: List<SteamFriend>)

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    suspend fun find(id: Long): SteamFriend?

    @Update
    suspend fun update(friend: SteamFriend)

    @Update
    suspend fun update(list: List<SteamFriend>)

    @Update
    suspend fun updateAll(friends: List<SteamFriend>)

    @Query("SELECT * FROM steam_friend")
    fun getFriendsFlow(): Flow<List<SteamFriend>>

    @Query("UPDATE steam_friend SET nickname = ''")
    suspend fun clearNicknames()

    @Query("SELECT * FROM steam_friend WHERE gameAppID > 0")
    suspend fun findFriendsInGame(): List<SteamFriend>

    @Delete
    suspend fun remove(list: List<SteamFriend>)

    @Delete
    suspend fun remove(friend: SteamFriend)

    @Query("DELETE FROM steam_friend")
    suspend fun delete()
}