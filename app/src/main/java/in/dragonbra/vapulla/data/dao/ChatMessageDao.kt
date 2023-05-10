package `in`.dragonbra.vapulla.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import `in`.dragonbra.vapulla.data.entity.ChatMessage

@Dao
interface ChatMessageDao {

    @Query(
        "SELECT * FROM chat_message WHERE message = :message AND " +
            "timestamp = :timestamp AND account_id = :accountid AND " +
            "from_local = :fromLocal"
    )
    fun find(message: String, timestamp: Long, accountid: Long, fromLocal: Boolean): ChatMessage?

    @Query(
        "SELECT * FROM chat_message WHERE message = :message AND " +
            "account_id = :accountid AND from_local = :fromLocal"
    )
    fun find(message: String, accountid: Long, fromLocal: Boolean): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg messages: ChatMessage)

    @Query("SELECT * FROM chat_message WHERE account_id = :friendId ORDER BY timestamp DESC")
    fun findLivePaged(friendId: Long): LiveData<List<ChatMessage>>

    @Update
    fun update(vararg messages: ChatMessage)

    @Query("UPDATE chat_message SET is_unread = 0 WHERE account_id = :accountid")
    fun markRead(accountid: Long)

    @Query("DELETE FROM chat_message WHERE id = :accountid")
    fun remove(accountid: Long)

    @Query("DELETE FROM chat_message")
    fun delete()
}
