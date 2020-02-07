package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.GameSchema
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameSchemaDao {
    @Query("SELECT * FROM game_schema WHERE id = :id")
    fun find(id: Int): GameSchema?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg gameSchema: GameSchema)
}