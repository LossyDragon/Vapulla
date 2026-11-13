package `in`.dragonbra.vapulla.ui.screens.cloud

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.service.api.CloudFile
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.PullToRefreshContent
import `in`.dragonbra.vapulla.ui.composables.RefreshableMessage
import `in`.dragonbra.vapulla.ui.composables.SkeletonList
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.groupedListShape
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import kotlinx.collections.immutable.persistentListOf

@Composable
fun CloudFilesScreen(viewModel: CloudFilesViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    CloudFilesContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onDownload = viewModel::download,
    )
}

@Composable
private fun CloudFilesContent(
    uiState: CloudFilesUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDownload: (CloudFile) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cloud saves") },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshContent(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
        ) {
            when {
                uiState.isLoading && uiState.files.isEmpty() ->
                    SkeletonList(contentPadding = listContentPadding(padding))

                uiState.error != null -> RefreshableMessage(message = uiState.error)

                uiState.files.isEmpty() -> RefreshableMessage(
                    message = "No cloud files for this app",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = listContentPadding(padding),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(uiState.files, key = { _, file -> file.filename }) { index, file ->
                        FileRow(
                            file = file,
                            downloading = uiState.downloading == file.filename,
                            shape = groupedListShape(index, uiState.files.size),
                            onClick = { onDownload(file) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileRow(
    file: CloudFile,
    downloading: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !downloading, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoPill(text = file.size.toFileSize())
                    if (file.timestamp > 0) InfoPill(text = (file.timestamp * 1000).toTimeAgo())
                    file.platforms.forEach { InfoPill(text = it) }
                }
            }
            if (downloading) {
                LoadingIndicator(modifier = Modifier.size(28.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    fun file(name: String, size: Long, vararg platforms: String) = CloudFile(
        appId = 440,
        filename = name,
        size = size,
        timestamp = 1_760_722_982,
        sha = "",
        platforms = persistentListOf(*platforms),
    )

    VapullaTheme {
        CloudFilesContent(
            uiState = CloudFilesUiState(
                files = persistentListOf(
                    file("cfg/config.cfg", 4_096, "Windows", "Linux"),
                    file("saves/slot1.sav", 2_500_000, "Windows"),
                    file("screenshots/thumbnail.jpg", 180_000),
                ),
                isLoading = false,
                downloading = "saves/slot1.sav",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onRefresh = {},
            onDownload = {},
        )
    }
}
