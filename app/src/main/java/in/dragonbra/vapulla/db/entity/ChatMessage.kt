package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * One reaction on a friend chat message, with everyone who gave it.
 *
 * @property reaction what Steam calls it: an emoticon's name between colons, e.g.
 *   ":steamhappy:", or a sticker's name as it is.
 * @property reactors account IDs, the low half of a Steam ID.
 */
@Immutable
@Serializable
data class StoredReaction(
    val reaction: String,
    val isSticker: Boolean = false,
    val reactors: List<Int> = emptyList(),
)

/**
 * One line of a chat with [friendId]. Messages typed locally are stored right away with
 * [timestampConfirmed] false and reconciled when Steam echoes them back in the history.
 *
 * @property ordinal tells apart messages Steam received within the same second. Together
 *   with [timestamp] it is how Steam names a message, to react to it for one. 0 for the
 *   first of a second, and for rows stored before it was kept.
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
    val ordinal: Int = 0,
    val reactions: List<StoredReaction> = emptyList(),
)
