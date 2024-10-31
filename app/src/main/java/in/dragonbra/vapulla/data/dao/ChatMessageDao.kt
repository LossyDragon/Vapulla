package `in`.dragonbra.vapulla.data.dao

import androidx.room.*
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("""
        SELECT * FROM chat_message
        WHERE message = :message
        AND timestamp = :timestamp
        AND account_id = :accountId
        AND from_local = :fromLocal
        LIMIT 1
    """)
    suspend fun findMessage(
        message: String,
        timestamp: Long,
        accountId: Long,
        fromLocal: Boolean
    ): ChatMessage?

    @Query("""
        SELECT * FROM chat_message
        WHERE message = :message
        AND account_id = :accountId
        AND from_local = :fromLocal
        ORDER BY timestamp DESC
    """)
    fun findMessages(
        message: String,
        accountId: Long,
        fromLocal: Boolean
    ): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessage>)

    @Query("""
        SELECT * FROM chat_message
        WHERE account_id = :friendId
        ORDER BY timestamp DESC
    """)
    fun getMessagesForFriend(friendId: Long): Flow<List<ChatMessage>>

    @Query("""
        SELECT * FROM chat_message
        WHERE account_id = :friendId
        AND is_unread = 1
        ORDER BY timestamp DESC
    """)
    fun getUnreadMessagesForFriend(friendId: Long): Flow<List<ChatMessage>>

    @Update
    suspend fun update(message: ChatMessage)

    @Update
    suspend fun updateAll(messages: List<ChatMessage>)

    @Query("UPDATE chat_message SET is_unread = 0 WHERE account_id = :accountId")
    suspend fun markRead(accountId: Long)

    @Query("""
        UPDATE chat_message
        SET is_unread = 0
        WHERE account_id = :accountId
        AND timestamp <= :beforeTimestamp
    """)
    suspend fun markMessagesAsReadBefore(accountId: Long, beforeTimestamp: Long)

    @Query("DELETE FROM chat_message WHERE account_id = :accountId")
    suspend fun deleteMessagesForFriend(accountId: Long)

    @Query("DELETE FROM chat_message WHERE account_id IN (:accountIds)")
    suspend fun deleteMessagesForFriends(accountIds: List<Long>)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAllMessages()

    @Transaction
    suspend fun replaceAllMessages(messages: List<ChatMessage>) {
        deleteAllMessages()
        insertAll(messages)
    }

    @Query("""
        SELECT COUNT(*)
        FROM chat_message
        WHERE account_id = :friendId
        AND is_unread = 1
    """)
    fun getUnreadMessageCount(friendId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM chat_message WHERE is_unread = 1")
    fun getTotalUnreadMessageCount(): Flow<Int>

    @Query("""
        SELECT * FROM chat_message
        WHERE account_id = :friendId
        AND timestamp > :afterTimestamp
        ORDER BY timestamp DESC
    """)
    fun getNewMessages(friendId: Long, afterTimestamp: Long): Flow<List<ChatMessage>>

    @Query("""
        SELECT * FROM chat_message
        WHERE account_id = :accountId
        AND timestamp IN (:timestamps)
    """)
    suspend fun findMessagesInTimeRange(
        accountId: Long,
        timestamps: List<Long>
    ): List<ChatMessage>
}
