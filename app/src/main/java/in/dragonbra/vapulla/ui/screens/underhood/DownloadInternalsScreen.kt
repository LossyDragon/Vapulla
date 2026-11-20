package `in`.dragonbra.vapulla.ui.screens.underhood

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import `in`.dragonbra.vapulla.db.dao.DownloadDao
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.DownloadProgress
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.EmptyMessage
import `in`.dragonbra.vapulla.ui.composables.Hint
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.CpuGroup
import `in`.dragonbra.vapulla.util.CpuUsage
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/**
 * @property download the row of the download that is running, null when none is.
 * @property progress its live figures.
 * @property speedLimit megabytes per second downloads are held to; 0 for none.
 * @property cpu how busy the app's threads were over the last second, busiest kind first.
 */
@Immutable
data class DownloadInternalsUiState(
    val download: Download? = null,
    val progress: DownloadProgress? = null,
    val speedLimit: Int = 0,
    val cpu: ImmutableList<CpuGroup> = persistentListOf(),
)

/**
 * The running download as the depot downloader sees it: chunks, bytes and connections, and
 * what the app's threads are doing meanwhile. Comparing the two shows what holds a download
 * back: the network, the CPU or the garbage collector.
 */
class DownloadInternalsViewModel(
    downloadDao: DownloadDao,
    downloads: DownloadManager,
    settings: SettingsManager,
) : ViewModel() {

    /** Sampled once a second, and only while the screen is open. */
    private val cpu = flow {
        val usage = CpuUsage()
        while (true) {
            emit(usage.sample().toImmutableList())
            delay(1.seconds)
        }
    }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<DownloadInternalsUiState> = combine(
        downloadDao.observeAll(),
        downloads.progress,
        settings.settings,
        cpu,
    ) { all, live, set, cpu ->
        DownloadInternalsUiState(
            download = all.firstOrNull { it.state == DownloadState.DOWNLOADING },
            progress = live,
            speedLimit = set.downloadSpeedLimit,
            cpu = cpu,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadInternalsUiState(),
    )
}

@Composable
fun DownloadInternalsScreen(viewModel: DownloadInternalsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DownloadInternalsContent(state = state, onBack = onBack)
}

@Composable
private fun DownloadInternalsContent(state: DownloadInternalsUiState, onBack: () -> Unit) {
    val download = state.download
    val live = state.progress

    UnderHoodScaffold(title = "Download internals", onBack = onBack) { padding ->
        if (download == null || live == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyMessage(
                    message = "Nothing is downloading. Start a download and come back to " +
                        "watch it work.",
                )
            }
            return@UnderHoodScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom())
                .verticalScroll(rememberScrollState())
                .padding(listContentPadding(padding)),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(title = download.name) {
                DetailList(
                    listOf(
                        "App ID" to download.appId.toString(),
                        "Workshop item" to
                            if (download.isWorkshop) download.workshopId.toString() else "",
                        "Branch" to download.branch,
                        "Language" to download.language,
                        "Depots" to download.depotIds.joinToString(),
                        "Doing" to live.status,
                        "Progress" to "${(live.fraction * 100).toInt()}%",
                    ),
                )
            }

            Section(title = "Chunks") {
                DetailList(
                    listOf(
                        "Written in this run" to "%,d".format(live.chunks),
                        "Per second" to "%.1f".format(live.chunksPerSecond),
                        "Fetched at once" to live.connections.toString(),
                    ),
                )
            }

            Section(title = "Bytes") {
                DetailList(
                    listOf(
                        "Received, compressed" to live.networkBytes.toFileSize(),
                        "Written, decompressed" to live.writtenBytes.toFileSize(),
                        "Compressed to" to compression(live),
                        "Network speed" to "${live.bytesPerSecond.toFileSize()}/s",
                        "Speed limit" to
                            if (state.speedLimit > 0) "${state.speedLimit} MB/s" else "None",
                    ),
                )
            }

            Section(title = "CPU") {
                val cores = Runtime.getRuntime().availableProcessors()
                val busy = state.cpu.sumOf { it.cores.toDouble() }

                DetailList(
                    listOf("In use" to "%.1f of %d cores".format(busy, cores)) +
                        state.cpu.map { group ->
                            group.name to
                                "%.2f cores · %d thread(s)".format(group.cores, group.threads)
                        },
                )
                Hint(
                    "Coroutine workers decrypt, decompress and write the chunks; HTTP " +
                        "downloads fetch them. Workers near the number of cores means the CPU " +
                        "is the limit; idle workers mean it is the network.",
                )
            }

            Hint(
                "Counted from the depot downloader's progress callbacks. Which content " +
                    "servers it uses, how often it retries and how each chunk is compressed " +
                    "are not reported by JavaSteam, so they are not shown.",
            )
        }
    }
}

/** What share of its real size the data travelled as, e.g. "64% of its size". */
private fun compression(live: DownloadProgress): String = if (live.writtenBytes > 0) {
    "${live.networkBytes * 100 / live.writtenBytes}% of its size"
} else {
    ""
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        DownloadInternalsContent(
            state = DownloadInternalsUiState(
                download = Download(
                    appId = 440,
                    name = "Team Fortress 2",
                    headerUrl = "",
                    os = "windows",
                    depotIds = listOf(441, 442),
                    installDir = "",
                    totalBytes = 12_345_678_901,
                    state = DownloadState.DOWNLOADING,
                    addedAt = 0,
                ),
                progress = DownloadProgress(
                    appId = 440,
                    workshopId = 0,
                    fraction = 0.42f,
                    downloadedBytes = 5_185_185_138,
                    totalBytes = 12_345_678_901,
                    bytesPerSecond = 13_400_000,
                    status = DownloadProgress.STATUS_DOWNLOADING,
                    chunks = 4_211,
                    chunksPerSecond = 14.5f,
                    networkBytes = 3_300_000_000,
                    writtenBytes = 5_100_000_000,
                    connections = 8,
                ),
                cpu = persistentListOf(
                    CpuGroup("Coroutine workers", 12, 3.82f),
                    CpuGroup("HTTP downloads", 8, 0.41f),
                    CpuGroup("Garbage collector", 1, 0.12f),
                ),
            ),
            onBack = {},
        )
    }
}
