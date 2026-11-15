package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Friends, pending requests and blocked users. Whole-row updates come from the friends sync;
 * a change to a single column has a query of its own so that it cannot undo one of those.
 */
@Dao
interface SteamFriendDao {

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

    @Query("SELECT * FROM steam_friend")
    fun getFriendsFlow(): Flow<List<SteamFriend>>

    /** Removes the nickname of every friend that is not in [ids]. */
    @Query("UPDATE steam_friend SET nickname = '' WHERE nickname != '' AND id NOT IN (:ids)")
    suspend fun clearNicknamesExcept(ids: List<Long>)

    /**
     * Sets one friend's nickname; empty removes it. Writes that one column, so it cannot undo
     * a presence update made to the same row at the same time.
     */
    @Query("UPDATE steam_friend SET nickname = :nickname WHERE id = :id AND nickname != :nickname")
    suspend fun setNickname(id: Long, nickname: String)

    /** Stores the earlier profile names of one friend, again as a single column. */
    @Query("UPDATE steam_friend SET aliases = :aliases WHERE id = :id")
    suspend fun setAliases(id: Long, aliases: ImmutableList<String>)

    /** Names the game of every friend playing [appId] who is not shown with that name yet. */
    @Query(
        "UPDATE steam_friend SET gameName = :name WHERE gameAppID = :appId AND gameName != :name",
    )
    suspend fun setGameName(appId: Int, name: String)

    @Query("SELECT * FROM steam_friend WHERE gameAppID > 0")
    suspend fun findFriendsInGame(): List<SteamFriend>

    /** Friends whose stored presence is anything other than plain offline. */
    @Query("SELECT * FROM steam_friend WHERE state != 0 OR gameAppID != 0 OR gameName != ''")
    suspend fun findWithPresence(): List<SteamFriend>

    @Delete
    suspend fun remove(list: List<SteamFriend>)

    @Query("DELETE FROM steam_friend WHERE id = :friendId")
    suspend fun remove(friendId: Long)

    @Query("DELETE FROM steam_friend")
    suspend fun deleteAll()
}
