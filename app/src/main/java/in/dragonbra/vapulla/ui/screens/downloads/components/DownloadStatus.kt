package `in`.dragonbra.vapulla.ui.screens.downloads.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.util.Utils.toFileSize

/** [progress] if it belongs to this download, which it only does while this one is running. */
fun Download.liveProgress(progress: DownloadProgress?): DownloadProgress? =
    progress?.takeIf { it.appId == appId && it.workshopId == workshopId }

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

            // The bar stays full while files are checked, so a percentage would say nothing.
            verify && live.status != "Downloading" -> "Verifying · ${live.status}"

            // Anything but plain downloading (checking files, allocating) is worth saying.
            live.status != "Downloading" -> "$percent · ${live.status}"

            else ->
                "$percent · ${live.downloadedBytes.toFileSize()} of $total · " +
                    "${live.bytesPerSecond.toFileSize()}/s"
        }

        DownloadState.QUEUED -> when {
            verify -> "Queued to verify"
            downloadedBytes > 0 -> "Queued · $percent of $total"
            else -> "Queued"
        }

        DownloadState.PAUSED -> "Paused · $percent of $total"

        DownloadState.COMPLETED -> listOfNotNull("Downloaded · $total", message).joinToString(" · ")

        DownloadState.FAILED -> message ?: "Failed"
    }
}

/** Marks a finished download that Steam has newer content for. */
@Composable
fun UpdateAvailablePill(modifier: Modifier = Modifier) {
    InfoPill(
        text = "Update available",
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

/** Says why queued downloads are not moving while the queue is held for Wi-Fi. */
@Composable
fun WaitingForWifi(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Waiting for Wi-Fi. Downloads are set to Wi-Fi only in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
