package `in`.dragonbra.vapulla.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * The stored conversation with one friend at a glance: when its newest message was sent, in
 * epoch milliseconds, and how many of the friend's messages have not been read yet.
 */
data class ChatSummary(val friendId: Long, val timestamp: Long, val unread: Int)

@Dao
interface ChatMessageDao {

    /** Newest first, for the reversed chat list. */
    @Query("SELECT * FROM chat_message WHERE friendId = :friendId ORDER BY timestamp DESC")
    fun getMessagesPagingSource(friendId: Long): PagingSource<Int, ChatMessage>

    /** One row per friend with stored messages, for Recent Chats and the unread counters. */
    @Query(
        "SELECT friendId, MAX(timestamp) AS timestamp, " +
            "SUM(unread = 1 AND fromLocal = 0) AS unread FROM chat_message GROUP BY friendId",
    )
    fun chatSummaries(): Flow<List<ChatSummary>>

    /** Stores [message] and returns its row id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_message SET unread = 0 WHERE friendId = :friendId AND fromLocal = 0")
    suspend fun markMessagesAsRead(friendId: Long)

    /** Exact match used to skip messages that are already stored. */
    @Query(
        "SELECT * FROM chat_message WHERE message = :message AND timestamp = :timestamp " +
            "AND friendId = :friendId AND fromLocal = :fromLocal AND timestampConfirmed = :confirmed",
    )
    suspend fun find(
        message: String,
        timestamp: Long,
        friendId: Long,
        fromLocal: Boolean,
        confirmed: Boolean,
    ): ChatMessage?

    /** Candidates for reconciling a locally sent message with its confirmed copy. */
    @Query(
        "SELECT * FROM chat_message WHERE message = :message AND friendId = :friendId " +
            "AND fromLocal = :fromLocal AND timestampConfirmed = :confirmed",
    )
    suspend fun find(
        message: String,
        friendId: Long,
        fromLocal: Boolean,
        confirmed: Boolean,
    ): List<ChatMessage>

    /** Marks a locally sent message as accepted by Steam, with Steam's timestamp and text. */
    @Query(
        "UPDATE chat_message SET message = :message, timestamp = :timestamp, " +
            "timestampConfirmed = 1 WHERE id = :id",
    )
    suspend fun confirm(id: Long, message: String, timestamp: Long)

    @Update
    suspend fun update(message: ChatMessage)

    @Query("DELETE FROM chat_message WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()
}
