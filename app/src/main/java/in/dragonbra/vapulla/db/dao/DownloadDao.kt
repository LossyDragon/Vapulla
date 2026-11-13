package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("SELECT * FROM download ORDER BY addedAt")
    fun observeAll(): Flow<List<Download>>

    @Query("SELECT * FROM download WHERE appId = :appId")
    fun observe(appId: Int): Flow<Download?>

    @Query("SELECT * FROM download WHERE appId = :appId")
    suspend fun find(appId: Int): Download?

    /** The download whose turn it is. */
    @Query("SELECT * FROM download WHERE state = 'QUEUED' ORDER BY addedAt LIMIT 1")
    suspend fun nextQueued(): Download?

    @Query("UPDATE download SET state = :state, message = :message WHERE appId = :appId")
    suspend fun setState(appId: Int, state: DownloadState, message: String? = null)

    @Query("UPDATE download SET downloadedBytes = :bytes WHERE appId = :appId")
    suspend fun updateProgress(appId: Int, bytes: Long)

    @Query(
        "UPDATE download SET state = 'COMPLETED', downloadedBytes = totalBytes, " +
            "message = :message, finishedAt = :finishedAt WHERE appId = :appId",
    )
    suspend fun complete(appId: Int, message: String?, finishedAt: Long)

    /** Pauses a download that has not started yet; a running one is stopped by the manager. */
    @Query("UPDATE download SET state = 'PAUSED' WHERE appId = :appId AND state = 'QUEUED'")
    suspend fun pauseQueued(appId: Int)

    /** Puts a paused or failed download back in the queue. */
    @Query(
        "UPDATE download SET state = 'QUEUED', message = NULL " +
            "WHERE appId = :appId AND state IN ('PAUSED', 'FAILED')",
    )
    suspend fun requeue(appId: Int)

    /** Downloads left running by a process that died go back in the queue. */
    @Query("UPDATE download SET state = 'QUEUED' WHERE state = 'DOWNLOADING'")
    suspend fun requeueInterrupted()

    /** Space that unfinished downloads other than [exceptAppId] will still fill. */
    @Query(
        "SELECT COALESCE(SUM(totalBytes - downloadedBytes), 0) FROM download " +
            "WHERE state IN ('QUEUED', 'DOWNLOADING', 'PAUSED') AND appId != :exceptAppId",
    )
    suspend fun reservedBytes(exceptAppId: Int): Long

    @Query("DELETE FROM download WHERE appId = :appId")
    suspend fun delete(appId: Int)

    @Query("DELETE FROM download WHERE state IN ('COMPLETED', 'FAILED')")
    suspend fun clearFinished()
}
