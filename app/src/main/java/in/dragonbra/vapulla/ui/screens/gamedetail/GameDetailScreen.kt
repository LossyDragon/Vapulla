package `in`.dragonbra.vapulla.ui.screens.gamedetail

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.steamStoreAssetUrl
import `in`.dragonbra.vapulla.service.api.AchievementProgress
import `in`.dragonbra.vapulla.ui.composables.BackButton
import `in`.dragonbra.vapulla.ui.composables.DetailList
import `in`.dragonbra.vapulla.ui.composables.ImageOptions
import `in`.dragonbra.vapulla.ui.composables.InfoPill
import `in`.dragonbra.vapulla.ui.composables.LoadingBox
import `in`.dragonbra.vapulla.ui.composables.RemoteImage
import `in`.dragonbra.vapulla.ui.composables.Section
import `in`.dragonbra.vapulla.ui.composables.ShimmerBox
import `in`.dragonbra.vapulla.ui.composables.exceptBottom
import `in`.dragonbra.vapulla.ui.composables.listContentPadding
import `in`.dragonbra.vapulla.ui.screens.gamedetail.components.DownloadSection
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.Utils.minutesToPlaytime
import `in`.dragonbra.vapulla.util.Utils.toDateString
import `in`.dragonbra.vapulla.util.Utils.toFileSize
import `in`.dragonbra.vapulla.util.Utils.toTimeAgo
import java.util.Date
import java.util.EnumSet

@Composable
fun GameDetailScreen(
    viewModel: GameDetailViewModel,
    onBack: () -> Unit,
    onOpenCloud: (Int) -> Unit,
    onOpenWorkshop: (Int) -> Unit,
    onOpenAchievements: (Int) -> Unit,
    onOpenDownloads: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    GameDetailContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenCloud = onOpenCloud,
        onOpenWorkshop = onOpenWorkshop,
        onOpenAchievements = onOpenAchievements,
        onRequestFreeLicense = viewModel::requestFreeLicense,
        onOpenDownloads = onOpenDownloads,
        onDownload = viewModel::startDownload,
        onPauseDownload = viewModel::pauseDownload,
        onResumeDownload = viewModel::resumeDownload,
    )
}

@Composable
private fun GameDetailContent(
    uiState: GameDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenCloud: (Int) -> Unit,
    onOpenWorkshop: (Int) -> Unit,
    onOpenAchievements: (Int) -> Unit,
    onRequestFreeLicense: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    onDownload: (SteamApp.OS) -> Unit = {},
    onPauseDownload: () -> Unit = {},
    onResumeDownload: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val app = uiState.app

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = app?.name?.decodeHtml().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { BackButton(onClick = onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (app == null) {
            LoadingBox(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding.exceptBottom()),
            contentPadding = listContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { HeaderArt(app) }

            item { Pills(app, uiState) }

            item {
                ActionRow(
                    app = app,
                    onStore = { uriHandler.openUri(Utils.Constants.BASE_STEAM_STORE_URL + app.id) },
                    onCloud = { onOpenCloud(app.id) },
                    onWorkshop = { onOpenWorkshop(app.id) },
                    onFreeLicense = onRequestFreeLicense,
                )
            }

            item {
                DownloadSection(
                    download = uiState.download,
                    progress = uiState.downloadProgress,
                    plans = uiState.downloadPlans,
                    freeBytes = uiState.freeBytes,
                    onDownload = onDownload,
                    onPause = onPauseDownload,
                    onResume = onResumeDownload,
                    onOpenDownloads = onOpenDownloads,
                )
            }

            item {
                Section(title = "Details") {
                    DetailRows(app, uiState, onAchievements = { onOpenAchievements(app.id) })
                }
            }

            if (app.branches.isNotEmpty()) {
                item { Section(title = "Branches") { BranchRows(app) } }
            }

            if (app.depots.isNotEmpty()) {
                item { Section(title = "Depots") { DepotRows(app) } }
            }
        }
    }
}

@Composable
private fun HeaderArt(app: SteamApp) {
    val url = app.getHeaderImageUrl()
        ?: steamStoreAssetUrl(app.id, "header.jpg")
    RemoteImage(
        imageModel = { url },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(460f / 215f)
            .clip(MaterialTheme.shapes.extraLarge),
        imageOptions = ImageOptions(contentScale = ContentScale.Crop),
        previewPlaceholder = painterResource(R.drawable.vapulla_background),
        loading = { ShimmerBox(modifier = Modifier.fillMaxSize()) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Pills(app: SteamApp, uiState: GameDetailUiState) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InfoPill(
            text = app.type.name.uppercase(),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (app.releaseDate > 0) InfoPill(text = app.releaseDate.toDateString())
        if (app.metacriticScore > 0) {
            InfoPill(
                text = "${app.metacriticScore} Metacritic",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        uiState.playersNow?.let { players ->
            InfoPill(
                text = "%,d playing now".format(players),
                icon = Icons.Outlined.Groups,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (uiState.isLoadingExtras) {
            LoadingIndicator(modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun ActionRow(
    app: SteamApp,
    onStore: () -> Unit,
    onCloud: () -> Unit,
    onWorkshop: () -> Unit,
    onFreeLicense: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Only offer what PICS says the app supports; the screens would just be empty otherwise.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (app.hasCloudSaves) {
                FilledTonalButton(
                    onClick = onCloud,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Outlined.Cloud, contentDescription = null)
                    Text(text = "  Cloud saves")
                }
            }
            if (app.workshopVisible) {
                FilledTonalButton(
                    onClick = onWorkshop,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Outlined.Construction, contentDescription = null)
                    Text(text = "  Workshop")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onStore,
                modifier = Modifier.weight(1f),
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = null)
                Text(text = "  Store page")
            }
            if (app.isFreeApp) {
                Button(
                    onClick = onFreeLicense,
                    modifier = Modifier.weight(1f),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Icon(Icons.Outlined.CardGiftcard, contentDescription = null)
                    Text(text = "  Free license")
                }
            }
        }
    }
}

@Composable
private fun DetailRows(app: SteamApp, uiState: GameDetailUiState, onAchievements: () -> Unit) {
    uiState.achievements?.takeIf { it.total > 0 }?.let { progress ->
        // Opens the full list: every achievement with its unlock date and rarity, plus stats.
        Surface(
            onClick = onAchievements,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Achievements", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${progress.unlocked} / ${progress.total}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Show achievements",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LinearWavyProgressIndicator(
                    progress = { progress.percentage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    DetailList(
        listOf(
            "Playtime" to
                if (app.playtimeForever > 0) app.playtimeForever.minutesToPlaytime() else "",
            "Last played" to if (app.lastPlayed > 0) (app.lastPlayed * 1000).toTimeAgo() else "",
            "Developer" to app.developer.decodeHtml(),
            "Publisher" to app.publisher.decodeHtml(),
            "Platforms" to app.osList.filter { it != SteamApp.OS.none }.joinToString { it.name },
            "Controller" to
                app.controllerSupport.name.takeIf {
                    app.controllerSupport !=
                        SteamApp.ControllerSupport.none
                }.orEmpty(),
            "Release state" to app.releaseState.name,
            "Reviews" to if (app.reviewPercentage > 0) "${app.reviewPercentage}% positive" else "",
            "App ID" to app.id.toString(),
        ),
    )
}

@Composable
private fun BranchRows(app: SteamApp) {
    DetailList(
        app.branches.values
            .sortedByDescending { it.timeUpdated }
            .map { branch ->
                val lock = if (branch.pwdRequired) " 🔒" else ""
                "${branch.name}$lock" to
                    "build ${branch.buildId} · ${(branch.timeUpdated.time / 1000).toDateString()}"
            },
    )
}

/** One row per depot with its OS and the public manifest size. */
@Composable
private fun DepotRows(app: SteamApp) {
    DetailList(
        app.depots.values
            .sortedBy { it.depotId }
            .map { depot ->
                val os = depot.osList.filter {
                    it != SteamApp.OS.none
                }.joinToString { it.name }.ifEmpty { "any" }
                val size =
                    depot.manifests["public"]?.size?.takeIf { it > 0 }?.toFileSize()
                        ?: "no public manifest"
                "Depot ${depot.depotId}" to "$os · $size"
            },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    val app = SteamApp(
        id = 440,
        name = "Team Fortress 2",
        type = SteamApp.AppType.game,
        osList = EnumSet.of(SteamApp.OS.windows, SteamApp.OS.linux),
        releaseState = SteamApp.ReleaseState.released,
        releaseDate = 1_191_999_600,
        metacriticScore = 92,
        reviewPercentage = 93,
        controllerSupport = SteamApp.ControllerSupport.partial,
        workshopVisible = true,
        developer = "Valve",
        publisher = "Valve",
        isFreeApp = true,
        playtimeForever = 4_242,
        lastPlayed = 1_760_722_982,
        ufs = SteamApp.UFS(quota = 1),
        branches = mapOf(
            "public" to SteamApp.BranchInfo(
                name = "public",
                buildId = 19_000_000,
                pwdRequired = false,
                timeUpdated = Date(1_760_000_000_000),
            ),
            "staging" to SteamApp.BranchInfo(
                name = "staging",
                buildId = 19_000_100,
                pwdRequired = true,
                timeUpdated = Date(1_760_500_000_000),
            ),
        ),
        depots = mapOf(
            441 to SteamApp.DepotInfo(
                depotId = 441,
                dlcAppId = 0,
                depotFromApp = 0,
                sharedInstall = false,
                osList = EnumSet.of(SteamApp.OS.windows),
                osArch = SteamApp.OSArch.Unknown,
                manifests = mapOf(
                    "public" to SteamApp.ManifestInfo(
                        name = "public",
                        gid = 1,
                        size = 12_345_678_901,
                        download = 9_000_000_000,
                    ),
                ),
                encryptedManifests = emptyMap(),
            ),
        ),
    )

    VapullaTheme {
        GameDetailContent(
            uiState = GameDetailUiState(
                app = app,
                playersNow = 61_234,
                achievements = AchievementProgress(
                    appId = 440,
                    unlocked = 312,
                    total = 520,
                    percentage = 60f,
                ),
                isLoadingExtras = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onOpenCloud = {},
            onOpenWorkshop = {},
            onOpenAchievements = {},
            onRequestFreeLicense = {},
        )
    }
}
