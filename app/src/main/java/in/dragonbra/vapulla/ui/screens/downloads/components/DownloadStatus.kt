package `in`.dragonbra.vapulla.ui.screens.downloads.components

import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.util.Utils.toFileSize

/** [progress] if it belongs to this download, which it only does while this one is running. */
fun Download.liveProgress(progress: DownloadProgress?): DownloadProgress? =
    progress?.takeIf { it.appId == appId }

/** How far along the download is, 0 to 1: the live figure while it runs, the saved one after. */
fun Download.fractionWith(progress: DownloadProgress?): Float = when {
    state == DownloadState.COMPLETED -> 1f
    else -> liveProgress(progress)?.fraction ?: fraction
}

/** One line saying where the download stands, for lists and the game detail card. */
fun Download.statusLine(progress: DownloadProgress?): String {
    val live = liveProgress(progress)
    val percent = "${(fractionWith(progress) * 100).toInt()}%"
    val total = totalBytes.toFileSize()

    return when (state) {
        DownloadState.DOWNLOADING -> when {
            live == null -> "Starting"

            // Anything but plain downloading (checking files, allocating) is worth saying.
            live.status != "Downloading" -> "$percent · ${live.status}"

            else ->
                "$percent · ${live.downloadedBytes.toFileSize()} of $total · " +
                    "${live.bytesPerSecond.toFileSize()}/s"
        }

        DownloadState.QUEUED -> if (downloadedBytes > 0) "Queued · $percent of $total" else "Queued"

        DownloadState.PAUSED -> "Paused · $percent of $total"

        DownloadState.COMPLETED -> listOfNotNull("Downloaded · $total", message).joinToString(" · ")

        DownloadState.FAILED -> message ?: "Failed"
    }
}
