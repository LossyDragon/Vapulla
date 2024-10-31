package `in`.dragonbra.vapulla.data.dao

import androidx.room.*
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.model.FriendListItem
import kotlinx.coroutines.flow.Flow

@Dao
@RewriteQueriesToDropUnusedColumns
interface SteamFriendDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(steamFriend: SteamFriend)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steamFriends: List<SteamFriend>)

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    suspend fun find(id: Long): SteamFriend?

    @Query("SELECT * FROM steam_friend WHERE id IN (:ids)")
    suspend fun findAll(ids: List<Long>): List<SteamFriend>

    @Query("SELECT * FROM steam_friend WHERE id = :id")
    fun findAsFlow(id: Long): Flow<SteamFriend?>

    @Query("""
    SELECT
        sf.*,
        max(cm.timestamp) AS last_message_time,
        ifnull(gs.name, sf.game_name) AS playing_game_name,
        cm.message as last_message,
        sum(CASE WHEN cm.is_unread = 1 THEN 1 ELSE 0 END) as new_message_count
    FROM steam_friend sf
    LEFT JOIN chat_message cm ON sf.id = cm.account_id
    LEFT JOIN game_schema gs ON gs.id = sf.game_app_id
    WHERE sf.id = :id
    GROUP BY sf.id
""")
    fun getFriendDetails(id: Long): Flow<FriendListItem>

    @Query("""
    SELECT
        sf.*,
        cm.message as last_message,
        max(cm.timestamp) as last_message_time,
        ifnull(gs.name, sf.game_name) as playing_game_name,
        sum(CASE WHEN cm.is_unread = 1 THEN 1 ELSE 0 END) as new_message_count
        FROM steam_friend sf
        LEFT JOIN chat_message cm ON sf.id = cm.account_id
        LEFT JOIN game_schema gs ON gs.id = sf.game_app_id
        WHERE sf.relation IN (2, 3)
        GROUP BY sf.id
        ORDER BY CASE
        WHEN last_message_time IS NULL THEN 0
        ELSE 1
        END DESC, last_message_time DESC
    """)
    fun getFriendsList(): Flow<List<FriendListItem>>

    @Query("""
        SELECT
            sf.*,
            cm.message as last_message,
            max(cm.timestamp) as last_message_time,
            ifnull(gs.name, sf.game_name) as playing_game_name,
            sum(CASE WHEN cm.is_unread = 1 THEN 1 ELSE 0 END) as new_message_count
        FROM steam_friend sf
        LEFT JOIN chat_message cm ON sf.id = cm.account_id
        LEFT JOIN game_schema gs ON gs.id = sf.game_app_id
        WHERE sf.state IS NOT NULL
        AND sf.state != 0
        GROUP BY sf.id
        ORDER BY sf.state ASC, sf.name ASC
    """)
    fun getOnlineFriends(): Flow<List<FriendListItem>>

    @Update
    suspend fun update(steamFriend: SteamFriend)

    @Update
    suspend fun updateAll(steamFriends: List<SteamFriend>)

    @Query("UPDATE steam_friend SET nickname = NULL")
    suspend fun clearNicknames()

    @Delete
    suspend fun remove(steamFriend: SteamFriend)

    @Delete
    suspend fun removeAll(friends: List<SteamFriend>)

    @Query("DELETE FROM steam_friend")
    suspend fun deleteAll()

    @Query("UPDATE steam_friend SET state = 0")
    suspend fun clearOnlineState()

    @Transaction
    suspend fun replaceAll(steamFriends: List<SteamFriend>) {
        deleteAll()
        insertAll(steamFriends)
    }

    @Query("SELECT COUNT(*) FROM steam_friend WHERE state IS NOT NULL AND state != 0")
    fun getOnlineFriendsCount(): Flow<Int>

    @Query(" SELECT COUNT(*) FROM steam_friend WHERE game_app_id > 0 OR game_name IS NOT NULL")
    fun getInGameFriendsCount(): Flow<Int>
}
