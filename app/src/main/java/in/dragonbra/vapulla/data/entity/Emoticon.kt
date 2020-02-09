package `in`.dragonbra.vapulla.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emoticon")
data class Emoticon(@PrimaryKey val name: String, val isSticker: Boolean)
