package `in`.dragonbra.vapulla.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.db.entity.ChatMessage

@Dao
interface ChatMessageDao {

    /** Newest first, for the reversed chat list. */
    @Query("SELECT * FROM chat_message WHERE friendId = :friendId ORDER BY timestamp DESC")
    fun getMessagesPagingSource(friendId: Long): PagingSource<Int, ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_message SET unread = 0 WHERE friendId = :friendId AND fromLocal = 0")
    suspend fun markMessagesAsRead(friendId: Long)

    /** Exact match used to skip history entries that are already stored. */
    @Query(
        "SELECT * FROM chat_message WHERE message = :message AND timestamp = :timestamp " +
            "AND friendId = :friendId AND fromLocal = :fromLocal AND timestampConfirmed = :confirmed",
    )
    fun find(
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
    fun find(
        message: String,
        friendId: Long,
        fromLocal: Boolean,
        confirmed: Boolean,
    ): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg messages: ChatMessage)

    @Update
    fun update(message: ChatMessage)

    @Update
    fun update(list: List<ChatMessage>)

    @Query("DELETE FROM chat_message")
    fun deleteAll()
}
