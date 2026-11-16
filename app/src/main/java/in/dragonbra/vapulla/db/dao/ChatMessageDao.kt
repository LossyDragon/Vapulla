package `in`.dragonbra.vapulla.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.StoredReaction
import kotlinx.coroutines.flow.Flow

/**
 * The stored conversation with one friend at a glance: when its newest message was sent, in
 * epoch milliseconds, and how many of the friend's messages have not been read yet.
 */
data class ChatSummary(val friendId: Long, val timestamp: Long, val unread: Int)

/** The stored chat messages of every friend. */
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

    /** Marks as read what the friend sent up to [until], in epoch milliseconds. */
    @Query(
        "UPDATE chat_message SET unread = 0 " +
            "WHERE friendId = :friendId AND fromLocal = 0 AND timestamp <= :until",
    )
    suspend fun markMessagesAsReadUntil(friendId: Long, until: Long)

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

    /**
     * Marks a locally sent message as accepted by Steam, with Steam's text for it and the
     * timestamp and ordinal Steam knows it by.
     */
    @Query(
        "UPDATE chat_message SET message = :message, timestamp = :timestamp, " +
            "ordinal = :ordinal, timestampConfirmed = 1 WHERE id = :id",
    )
    suspend fun confirm(id: Long, message: String, timestamp: Long, ordinal: Int)

    @Query("SELECT * FROM chat_message WHERE id = :id")
    suspend fun get(id: Long): ChatMessage?

    /** The message Steam names by [timestamp], in epoch milliseconds, and [ordinal]. */
    @Query(
        "SELECT * FROM chat_message WHERE friendId = :friendId AND timestamp = :timestamp " +
            "AND ordinal = :ordinal AND timestampConfirmed = 1 LIMIT 1",
    )
    suspend fun findByOrdinal(friendId: Long, timestamp: Long, ordinal: Int): ChatMessage?

    /** Replaces a message's reactions, and its ordinal for a row stored before it was kept. */
    @Query("UPDATE chat_message SET ordinal = :ordinal, reactions = :reactions WHERE id = :id")
    suspend fun setReactions(id: Long, ordinal: Int, reactions: List<StoredReaction>)

    @Query("DELETE FROM chat_message WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()
}
