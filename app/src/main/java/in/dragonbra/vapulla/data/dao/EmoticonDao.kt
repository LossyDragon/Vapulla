package `in`.dragonbra.vapulla.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.data.entity.Emoticon
import kotlinx.coroutines.flow.Flow

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg emoticon: Emoticon)

    @Query("SELECT * FROM emoticon ORDER BY isSticker DESC, appId DESC, name DESC")
    fun getLive(): Flow<List<Emoticon>>

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    fun find(): List<Emoticon>

    @Query("DELETE FROM emoticon")
    fun delete()
}
