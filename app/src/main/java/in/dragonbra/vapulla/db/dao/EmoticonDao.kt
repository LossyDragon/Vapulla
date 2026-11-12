package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import `in`.dragonbra.vapulla.db.entity.Emoticon

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emoticons: List<Emoticon>)

    @Query("DELETE FROM emoticon")
    suspend fun deleteAll()

    /** Steam sends the full list each time, so the table is swapped wholesale. */
    @Transaction
    suspend fun replaceAll(emoticons: List<Emoticon>) {
        deleteAll()
        insertAll(emoticons)
    }
}
