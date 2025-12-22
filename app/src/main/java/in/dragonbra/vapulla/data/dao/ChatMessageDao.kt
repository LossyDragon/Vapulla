package `in`.dragonbra.vapulla.data.dao

import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import `in`.dragonbra.vapulla.data.entity.ChatMessage

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_message WHERE friendId = :friendId ORDER BY timestamp ASC")
    fun getMessagesPagingSource(friendId: Long): PagingSource<Int, ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_message SET unread = 0 WHERE friendId = :friendId AND fromLocal = 0")
    suspend fun markMessagesAsRead(friendId: Long)

    //

    @Query("SELECT * FROM chat_message WHERE message = :message AND timestamp = :timestamp AND friendId = :friendId AND fromLocal = :fromLocal AND timestampConfirmed = :confirmed")
    fun find(
        message: String,
        timestamp: Long,
        friendId: Long,
        fromLocal: Boolean,
        confirmed: Boolean
    ): ChatMessage?

    @Query("SELECT * FROM chat_message WHERE message = :message AND friendId = :friendId AND fromLocal = :fromLocal AND timestampConfirmed = :confirmed")
    fun find(
        message: String,
        friendId: Long,
        fromLocal: Boolean,
        confirmed: Boolean
    ): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg messages: ChatMessage)

    @Query("SELECT * FROM chat_message WHERE friendId = :friendId ORDER BY timestamp DESC")
    fun findLivePaged(friendId: Long): DataSource.Factory<Int, ChatMessage>

    @Update
    fun update(message: ChatMessage)

    @Update
    fun update(list: List<ChatMessage>)

    @Query("UPDATE chat_message SET unread = 0 WHERE friendId = :friendId")
    fun markRead(friendId: Long)

    @Query("DELETE FROM chat_message")
    fun delete()
}