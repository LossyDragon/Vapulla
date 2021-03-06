package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.adapter.FriendListItem
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SteamFriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg steamFriends: SteamFriend)

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    fun find(id: Long): SteamFriend?

    @Query("SELECT " +
            "  sf.*, " +
            "  max(cm.timestamp) AS last_message_time, " +
            "  ifnull(gs.name, sf.game_name) AS playing_game_name " +
            "FROM steam_friend sf " +
            "LEFT JOIN chat_message cm " +
            "ON sf.id = cm.friend_id " +
            "LEFT JOIN game_schema gs " +
            "ON gs.id = sf.game_app_id " +
            "WHERE sf.id = :id")
    fun findLive(id: Long): LiveData<FriendListItem>

    @Update
    fun update(vararg steamFriends: SteamFriend)

    @Query("SELECT " +
            "  sf.*, " +
            "  cm.message as last_message, " +
            "  max(cm.timestamp) as last_message_time, " +
            "  ifnull(gs.name, sf.game_name) as playing_game_name, " +
            "  sum(cm.unread) as new_message_count " +
            "FROM steam_friend sf " +
            "LEFT JOIN chat_message cm " +
            "ON sf.id = cm.friend_id " +
            "LEFT JOIN game_schema gs " +
            "ON gs.id = sf.game_app_id " +
            "WHERE sf.relation = 2 " +
            "   OR sf.relation = 3 " +
            "GROUP BY sf.id")
    fun getLive(): LiveData<List<FriendListItem>>

    @Query("UPDATE steam_friend SET nickname = NULL")
    fun clearNicknames()

    @Delete
    fun remove(vararg friends: SteamFriend)

    @Query("DELETE FROM steam_friend")
    fun delete()

    @Query("UPDATE steam_friend SET state = 0")
    fun clearOnlineState()
}
