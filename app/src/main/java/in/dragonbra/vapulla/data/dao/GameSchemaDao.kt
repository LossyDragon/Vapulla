package `in`.dragonbra.vapulla.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.data.entity.GameSchema

@Dao
interface GameSchemaDao {
    @Query("SELECT * FROM game_schema WHERE id = :id")
    fun find(id: Int): GameSchema?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg gameSchema: GameSchema)

    @Query("DELETE FROM game_schema")
    fun delete()
}
