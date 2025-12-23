package `in`.dragonbra.vapulla.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "emoticon")
data class Emoticon(
    @PrimaryKey val name: String,
    val appID: Int,
    val isSticker: Boolean
)