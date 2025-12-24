package `in`.dragonbra.vapulla.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "chat_message", indices = [Index("friendId")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val message: String,
    val timestamp: Long,
    val friendId: Long,
    val fromLocal: Boolean,
    val unread: Boolean,
    val timestampConfirmed: Boolean,
) {
    // @Ignore
    // val formattedTs = Utils.dateFormatter.format(Date(timestamp)).uppercase()

    @Ignore
    constructor(
        message: String,
        timestamp: Long,
        friendId: Long,
        fromLocal: Boolean,
        unread: Boolean,
        timestampConfirmed: Boolean,
    ) : this(0L, message, timestamp, friendId, fromLocal, unread, timestampConfirmed)
}
