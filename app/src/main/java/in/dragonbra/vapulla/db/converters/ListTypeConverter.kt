package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json

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
    fun fromStringList(value: String?): ImmutableList<String> {
        if (value == null) return persistentListOf()
        val list: List<String> = Json.decodeFromString(value)
        return list.toImmutableList()
    }

    @TypeConverter
    fun toStringList(list: ImmutableList<String>?): String =
        Json.encodeToString(list?.toList() ?: emptyList())

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> =
        if (value.isNullOrEmpty()) emptyMap() else Json.decodeFromString(value)

    @TypeConverter
    fun toStringMap(map: Map<String, String>): String = Json.encodeToString(map)
}
