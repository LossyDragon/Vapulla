package `in`.dragonbra.vapulla.data.entity

import android.annotation.SuppressLint
import androidx.room.*
import java.text.DateFormat
import java.util.*

@Entity(tableName = "chat_message", indices = [Index("account_id")])
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "from_local") val fromLocal: Boolean,
    @ColumnInfo(name = "is_unread") val isUnread: Boolean,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long
) {
    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormat = DateFormat.getDateInstance(
            DateFormat.MEDIUM,
            Locale.getDefault()
        )
    }

    val formattedDate: String
        get() = dateFormat.format(Date(timestamp * 1000))
            .uppercase(Locale.ROOT)

    val formattedTime: String
        get() = android.text.format.DateFormat.format(
            "h:mm a",
            Calendar.getInstance().apply {
                timeInMillis = timestamp * 1000
            }
        ).toString()

    @Ignore
    constructor(
        accountId: Long,
        fromLocal: Boolean,
        isUnread: Boolean,
        message: String,
        timestamp: Long
    ) : this(0L, accountId, fromLocal, isUnread, message, timestamp)
}
