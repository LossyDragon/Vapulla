package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One line of a chat with [friendId]. Messages typed locally are stored right away with
 * [timestampConfirmed] false and reconciled when Steam echoes them back in the history.
 */
@Immutable
@Entity(tableName = "chat_message", indices = [Index("friendId")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val message: String,
    val timestamp: Long,
    val friendId: Long,
    val fromLocal: Boolean,
    val unread: Boolean,
    val timestampConfirmed: Boolean,
)
