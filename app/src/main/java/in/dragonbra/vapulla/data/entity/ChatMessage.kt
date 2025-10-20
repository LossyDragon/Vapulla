package `in`.dragonbra.vapulla.data.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "chat_message", indices = [Index("friendId")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var message: String,
    var timestamp: Long,
    var friendId: Long,
    var fromLocal: Boolean,
    var unread: Boolean,
    var timestampConfirmed: Boolean
) {
    companion object {
        private val DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }

    @Ignore
    val formattedTs = DATE_FORMAT.format(Date(timestamp)).uppercase()

    @Ignore
    constructor(
        message: String,
        timestamp: Long,
        friendId: Long,
        fromLocal: Boolean,
        unread: Boolean,
        timestampConfirmed: Boolean
    ) : this(0L, message, timestamp, friendId, fromLocal, unread, timestampConfirmed)
}