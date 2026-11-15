package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * One download: an app started from the library, or one of its workshop items. A row stays
 * until the user clears it, whatever its [state], so the Downloads screen doubles as a history.
 *
 * Progress while a download runs is live state in
 * [in.dragonbra.vapulla.service.download.DownloadManager]; [downloadedBytes] is only a
 * periodic snapshot of it, so a paused or interrupted download still shows how far it got.
 */
@Immutable
@Entity(tableName = "download", primaryKeys = ["appId", "workshopId"])
data class Download(
    val appId: Int,
    /** The published file ID of a workshop item; 0 when the download is the app itself. */
    val workshopId: Long = 0L,
    val name: String,
    val headerUrl: String,
    /** The platform the depots were picked for, a [SteamApp.OS] name. */
    val os: String,
    /** Empty for a workshop item, whose depot only Steam knows. */
    val depotIds: List<Int>,
    val branch: String = "public",
    val language: String = "english",
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
    /** Set while the files on disk are to be checked against Steam's, chunk by chunk. */
    val verify: Boolean = false,
    /**
     * The manifest of each depot when the download completed, as written by
     * [in.dragonbra.vapulla.service.download.DownloadPlanner.manifestIds]. A depot gets a new
     * manifest with every update, so a difference from Steam's current ones means there is
     * an update. Empty until a download completes, and for workshop items.
     */
    @ColumnInfo(defaultValue = "")
    val manifestIds: String = "",
    /**
     * The password of [branch] when it is behind one; empty otherwise. Kept so that the
     * download can be resumed, verified and updated without asking for it again.
     */
    @ColumnInfo(defaultValue = "")
    val branchPassword: String = "",
) {
    val isWorkshop: Boolean get() = workshopId != 0L

    /** Tells the notifications of finished downloads apart. */
    val notificationId: Int get() = if (isWorkshop) workshopId.hashCode() else appId

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
