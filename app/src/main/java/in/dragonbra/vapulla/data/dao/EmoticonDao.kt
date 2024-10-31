package `in`.dragonbra.vapulla.data.dao

import androidx.room.*
import `in`.dragonbra.vapulla.data.entity.Emoticon
import kotlinx.coroutines.flow.Flow

@Dao
interface EmoticonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emoticon: Emoticon)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(emoticons: List<Emoticon>)

    @Query("SELECT * FROM emoticon ORDER BY isSticker DESC, appId DESC, name DESC")
    fun getAll(): Flow<List<Emoticon>>

    @Query("SELECT * FROM emoticon WHERE name LIKE :searchQuery ORDER BY name ASC")
    fun searchEmoticons(searchQuery: String): Flow<List<Emoticon>>

    @Query("SELECT * FROM emoticon ORDER BY name ASC")
    suspend fun find(): List<Emoticon>

    @Query("DELETE FROM emoticon")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(emoticons: List<Emoticon>) {
        deleteAll()
        insertAll(emoticons)
    }

    @Query("SELECT COUNT(*) FROM emoticon")
    fun getCount(): Flow<Int>

    @Query("SELECT * FROM emoticon WHERE isSticker = :isSticker ORDER BY name ASC")
    fun getByType(isSticker: Boolean): Flow<List<Emoticon>>
}
