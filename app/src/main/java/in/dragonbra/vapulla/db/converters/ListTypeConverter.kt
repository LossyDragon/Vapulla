package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Lists and maps of plain values. IDs are stored comma-separated, which SQL can read; text,
 * which may itself hold a comma, is stored as JSON.
 */
class ListTypeConverter {
    @TypeConverter
    fun fromIntList(value: List<Int>): String = value.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> = if (value.isEmpty()) {
        emptyList()
    } else {
        value.split(",").map { it.toInt() }
    }

    @TypeConverter
    fun fromStringList(list: ImmutableList<String>): String = DbJson.encodeToString(list.toList())

    @TypeConverter
    fun toStringList(value: String): ImmutableList<String> =
        DbJson.decodeFromString<List<String>>(value).toImmutableList()

    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String = DbJson.encodeToString(map)

    @TypeConverter
    fun toStringMap(value: String): Map<String, String> =
        if (value.isEmpty()) emptyMap() else DbJson.decodeFromString(value)
}
