package `in`.dragonbra.vapulla.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import `in`.dragonbra.vapulla.data.entity.Emoticon

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emoticons: List<Emoticon>)

    @Query("DELETE FROM emoticon")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(emoticons: List<Emoticon>) {
        deleteAll()
        insertAll(emoticons)
    }
}