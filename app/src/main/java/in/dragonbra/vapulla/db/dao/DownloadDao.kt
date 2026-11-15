package `in`.dragonbra.vapulla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import kotlinx.coroutines.flow.Flow

/**
 * A download is found by its app ID and workshop ID together. The workshop ID is 0 for the
 * app itself, which is what the queries default to.
 */
@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: Download)

    @Query("SELECT * FROM download ORDER BY addedAt")
    fun observeAll(): Flow<List<Download>>

    /** App downloads only, without workshop items. */
    @Query("SELECT * FROM download WHERE workshopId = 0 ORDER BY addedAt")
    fun observeApps(): Flow<List<Download>>

    /** The workshop items of [appId] that were downloaded or are about to be. */
    @Query("SELECT * FROM download WHERE appId = :appId AND workshopId != 0")
    fun observeWorkshop(appId: Int): Flow<List<Download>>

    @Query("SELECT * FROM download WHERE appId = :appId AND workshopId = :workshopId")
    fun observe(appId: Int, workshopId: Long = 0L): Flow<Download?>

    @Query("SELECT * FROM download WHERE appId = :appId AND workshopId = :workshopId")
    suspend fun find(appId: Int, workshopId: Long = 0L): Download?

    /** The download whose turn it is. */
    @Query("SELECT * FROM download WHERE state = 'QUEUED' ORDER BY addedAt LIMIT 1")
    suspend fun nextQueued(): Download?

    @Query(
        "UPDATE download SET state = :state, message = :message " +
            "WHERE appId = :appId AND workshopId = :workshopId",
    )
    suspend fun setState(
        appId: Int,
        workshopId: Long,
        state: DownloadState,
        message: String? = null,
    )

    @Query(
        "UPDATE download SET downloadedBytes = :bytes " +
            "WHERE appId = :appId AND workshopId = :workshopId",
    )
    suspend fun updateProgress(appId: Int, workshopId: Long, bytes: Long)

    @Query(
        "UPDATE download SET state = 'COMPLETED', downloadedBytes = totalBytes, verify = 0, " +
            "message = :message, finishedAt = :finishedAt, manifestIds = :manifestIds " +
            "WHERE appId = :appId AND workshopId = :workshopId",
    )
    suspend fun complete(
        appId: Int,
        workshopId: Long,
        message: String?,
        finishedAt: Long,
        manifestIds: String,
    )

    /** Records that the files of a download now live in [installDir]. */
    @Query(
        "UPDATE download SET installDir = :installDir " +
            "WHERE appId = :appId AND workshopId = :workshopId",
    )
    suspend fun setInstallDir(appId: Int, workshopId: Long, installDir: String)

    /** Pauses a download that has not started yet; a running one is stopped by the manager. */
    @Query(
        "UPDATE download SET state = 'PAUSED' " +
            "WHERE appId = :appId AND workshopId = :workshopId AND state = 'QUEUED'",
    )
    suspend fun pauseQueued(appId: Int, workshopId: Long)

    /** Puts a paused or failed download back in the queue. */
    @Query(
        "UPDATE download SET state = 'QUEUED', message = NULL " +
            "WHERE appId = :appId AND workshopId = :workshopId AND state IN ('PAUSED', 'FAILED')",
    )
    suspend fun requeue(appId: Int, workshopId: Long)

    /** Queues a download that is not running to have its files checked. */
    @Query(
        "UPDATE download SET state = 'QUEUED', verify = 1, message = NULL " +
            "WHERE appId = :appId AND workshopId = :workshopId " +
            "AND state IN ('COMPLETED', 'PAUSED', 'FAILED')",
    )
    suspend fun requeueToVerify(appId: Int, workshopId: Long)

    /** Downloads left running by a process that died go back in the queue. */
    @Query("UPDATE download SET state = 'QUEUED' WHERE state = 'DOWNLOADING'")
    suspend fun requeueInterrupted()

    /** Space that unfinished downloads other than the given one will still fill. */
    @Query(
        "SELECT COALESCE(SUM(totalBytes - downloadedBytes), 0) FROM download " +
            "WHERE state IN ('QUEUED', 'DOWNLOADING', 'PAUSED') " +
            "AND NOT (appId = :exceptAppId AND workshopId = :exceptWorkshopId)",
    )
    suspend fun reservedBytes(exceptAppId: Int, exceptWorkshopId: Long = 0L): Long

    @Query("DELETE FROM download WHERE appId = :appId AND workshopId = :workshopId")
    suspend fun delete(appId: Int, workshopId: Long)

    @Query("DELETE FROM download WHERE state IN ('COMPLETED', 'FAILED')")
    suspend fun clearFinished()
}
