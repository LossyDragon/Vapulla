package `in`.dragonbra.vapulla.data.dao

import `in`.dragonbra.vapulla.data.entity.Emoticon
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg emoticon: Emoticon)

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    fun getLive(): LiveData<List<Emoticon>>

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    fun find(): List<Emoticon>

    @Query("DELETE FROM emoticon")
    fun delete()
}
