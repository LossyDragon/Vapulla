package `in`.dragonbra.vapulla.service.download

import androidx.compose.runtime.Immutable
import `in`.dragonbra.vapulla.db.entity.Download
import `in`.dragonbra.vapulla.db.entity.DownloadState
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamApp.OS
import `in`.dragonbra.vapulla.db.entity.SteamApp.OSArch

/**
 * What downloading an app for one platform involves: which depots, and how big they are.
 *
 * @property os the platform the depots were picked for; [OS.none] when the app has no
 *   platform-specific depots at all.
 * @property installBytes size on disk once installed.
 * @property downloadBytes compressed size that travels over the network.
 */
@Immutable
data class DownloadPlan(
    val os: OS,
    val depotIds: List<Int>,
    val installBytes: Long,
    val downloadBytes: Long,
    val branch: String = DownloadPlanner.BRANCH,
    val language: String = DownloadPlanner.LANGUAGE,
)

/**
 * Picks the depots of an app to download, from its PICS app info.
 *
 * The depot downloader can pick depots itself, but only while it downloads, which leaves
 * nothing to show beforehand and no total to measure progress against. So the choice is made
 * here with the same rules (platform, architecture, language, low violence) and handed to
 * the downloader as an explicit depot list.
 */
object DownloadPlanner {

    const val BRANCH = "public"
    const val LANGUAGE = "english"

    private val platforms = listOf(OS.windows, OS.linux, OS.macos)

    /**
     * The branches [app] can be downloaded from, the public one first. A branch behind a
     * password is only among them once [unlock] has put its manifests in: until then they
     * are encrypted, so there is nothing to plan with.
     */
    fun branches(app: SteamApp): List<String> {
        val others = app.branches.values
            .filter { branch ->
                branch.name != BRANCH && app.depots.values.any { branch.name in it.manifests }
            }
            .map { it.name }
            .sorted()

        return listOf(BRANCH) + others
    }

    /** The branches of [app] that are behind a password and have not been opened. */
    fun lockedBranches(app: SteamApp): List<String> = app.branches.values
        .filter { branch ->
            branch.pwdRequired && app.depots.values.none { branch.name in it.manifests }
        }
        .map { it.name }
        .sorted()

    /**
     * [app] with the [manifests] of a password-protected [branch] put in, as Steam hands
     * them out for the right password. Depots the branch does not touch are left alone.
     */
    fun unlock(
        app: SteamApp,
        branch: String,
        manifests: Map<Int, SteamApp.ManifestInfo>,
    ): SteamApp = app.copy(
        depots = app.depots.mapValues { (depotId, depot) ->
            manifests[depotId]
                ?.let { depot.copy(manifests = depot.manifests + (branch to it)) }
                ?: depot
        },
    )

    /** The languages [app] has depots of its own for, English first. */
    fun languages(app: SteamApp): List<String> {
        val others = app.depots.values
            .map { it.language }
            .filter { it.isNotEmpty() && it != LANGUAGE }
            .distinct()
            .sorted()

        return listOf(LANGUAGE) + others
    }

    /** One plan per platform the app has content for, Windows first. */
    fun plans(
        app: SteamApp,
        ownedDepotIds: Set<Int>,
        branch: String = BRANCH,
        language: String = LANGUAGE,
    ): List<DownloadPlan> {
        val offered = platforms.filter { os ->
            os in app.osList || app.depots.values.any { os in it.osList }
        }

        return offered.ifEmpty { listOf(OS.none) }
            .mapNotNull { plan(app, it, ownedDepotIds, branch, language) }
    }

    /** The depots [app] installs on [os], or null when there is nothing to download. */
    fun plan(
        app: SteamApp,
        os: OS,
        ownedDepotIds: Set<Int>,
        branch: String = BRANCH,
        language: String = LANGUAGE,
    ): DownloadPlan? {
        val matching = app.depots.values.filter { depot ->
            // Depots without a manifest of their own are skipped: ones shared from another
            // app (redistributables, mostly) and ones only on locked branches.
            val manifest = depot.manifest(branch)
            val anyPlatform = depot.osList.all { it == OS.none }

            manifest != null &&
                manifest.gid != 0L &&
                (os == OS.none || anyPlatform || os in depot.osList) &&
                (depot.language.isEmpty() || depot.language == language) &&
                !depot.lowViolence
        }

        // 64-bit where there is a choice, as a 64-bit Steam client would.
        val forArch = matching.filter { it.osArch != OSArch.Arch32 }.ifEmpty { matching }

        // Leaves out DLC the account does not have. When none of the depots are licensed
        // (a free app not claimed yet, or licenses still syncing) Steam is left to decide.
        val owned = forArch.filter { it.depotId in ownedDepotIds }.ifEmpty { forArch }

        if (owned.isEmpty()) {
            return null
        }

        return DownloadPlan(
            os = os,
            depotIds = owned.map { it.depotId }.sorted(),
            installBytes = owned.sumOf { it.manifest(branch)?.size ?: 0L },
            downloadBytes = owned.sumOf { it.manifest(branch)?.download ?: 0L },
            branch = branch,
            language = language,
        )
    }

    /**
     * The manifests the depots [depotIds] of [app] have on [branch] right now, as one text
     * such as `441:123,442:456`. Two of these are only ever compared with each other.
     */
    fun manifestIds(app: SteamApp, depotIds: List<Int>, branch: String): String =
        depotIds.sorted().joinToString(",") { id ->
            "$id:${app.depots[id]?.manifest(branch)?.gid ?: 0L}"
        }

    /**
     * True when Steam has newer content for a completed [download] than what was downloaded:
     * a depot's manifest is no longer the one it completed with. Downloads completed before
     * manifests were recorded cannot tell, and say no.
     */
    fun hasUpdate(download: Download, app: SteamApp?): Boolean = app != null &&
        download.state == DownloadState.COMPLETED &&
        download.manifestIds.isNotEmpty() &&
        download.manifestIds != manifestIds(app, download.depotIds, download.branch)

    /**
     * The manifest a depot is downloaded with on [branch]. A depot that the branch does not
     * change is taken from the public one, which is what the downloader does too.
     */
    fun SteamApp.DepotInfo.manifest(branch: String): SteamApp.ManifestInfo? =
        manifests[branch] ?: manifests[BRANCH]
}
