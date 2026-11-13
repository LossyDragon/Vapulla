package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One app download started from the library. A row stays until the user clears it, whatever
 * its [state], so the Downloads screen doubles as a history.
 *
 * Progress while a download runs is live state in
 * [in.dragonbra.vapulla.service.download.DownloadManager]; [downloadedBytes] is only a
 * periodic snapshot of it, so a paused or interrupted download still shows how far it got.
 */
@Immutable
@Entity(tableName = "download")
data class Download(
    @PrimaryKey val appId: Int,
    val name: String,
    val headerUrl: String,
    /** The platform the depots were picked for, a [SteamApp.OS] name. */
    val os: String,
    val depotIds: List<Int>,
    /** Absolute path of the folder the app is written to. */
    val installDir: String,
    /** Size on disk of all [depotIds], from their PICS manifests. */
    val totalBytes: Long,
    val downloadedBytes: Long = 0L,
    val state: DownloadState = DownloadState.QUEUED,
    /** Why a download failed, or a note about a finished one. */
    val message: String? = null,
    /** Epoch milliseconds; the queue runs oldest first. */
    val addedAt: Long,
    val finishedAt: Long = 0L,
) {
    val fraction: Float
        get() = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
        } else {
            0f
        }
}

enum class DownloadState {
    /** Waiting for its turn, or for a Steam session to download with. */
    QUEUED,
    DOWNLOADING,

    /** Stopped by the user; stays stopped until resumed. */
    PAUSED,
    COMPLETED,
    FAILED,
    ;

    val isFinished: Boolean get() = this == COMPLETED || this == FAILED
}
