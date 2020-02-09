package `in`.dragonbra.vapulla.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_schema")
data class GameSchema(
        @ColumnInfo(name = "id") @PrimaryKey var id: Int,
        @ColumnInfo(name = "name") var name: String?,
        @ColumnInfo(name = "modify_date") var modifyDate: Long
)
