package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.vapulla.db.entity.StoredReaction

/** The reactions of a chat message, stored as JSON. */
class ReactionTypeConverter {
    @TypeConverter
    fun fromReactions(reactions: List<StoredReaction>): String = DbJson.encodeToString(reactions)

    @TypeConverter
    fun toReactions(value: String): List<StoredReaction> =
        if (value.isEmpty()) emptyList() else DbJson.decodeFromString(value)
}
