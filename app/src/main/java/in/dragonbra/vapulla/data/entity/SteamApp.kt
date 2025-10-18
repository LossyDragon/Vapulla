package `in`.dragonbra.vapulla.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steam_app")
data class SteamApp(
    @PrimaryKey
    val id: Int,

    @ColumnInfo("name")
    val name: String = "",

    @ColumnInfo("last_change_number")
    val lastChangeNumber: Int = 0,
)