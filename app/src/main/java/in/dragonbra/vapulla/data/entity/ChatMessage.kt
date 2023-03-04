package `in`.dragonbra.vapulla.data.entity

import android.annotation.SuppressLint
import androidx.room.*
import java.text.DateFormat
import java.util.*

@Entity(tableName = "chat_message", indices = [Index("account_id")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long,
    @ColumnInfo(name = "account_id") var accountid: Long,
    @ColumnInfo(name = "from_local") var fromLocal: Boolean,
    @ColumnInfo(name = "is_unread") var isUnread: Boolean,
    @ColumnInfo(name = "message") var message: String,
    @ColumnInfo(name = "timestamp") var timestamp: Long
) {
    companion object {
        @SuppressLint("ConstantLocale")
        private val DATE_FORMAT = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
    }

    @Ignore
    val formattedTs = DATE_FORMAT.format(Date(timestamp.times(1000))).uppercase(Locale.ROOT)

    @Ignore
    fun formattedChatTime(): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp.times(1000)
        }
        return android.text.format.DateFormat.format("h:mm a", calendar).toString()
    }

    @Ignore
    constructor(
        accountid: Long,
        fromLocal: Boolean,
        isUnread: Boolean,
        message: String,
        timestamp: Long
    ) : this(0L, accountid, fromLocal, isUnread, message, timestamp)
}
