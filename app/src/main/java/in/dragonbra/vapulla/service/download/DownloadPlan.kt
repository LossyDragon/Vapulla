package `in`.dragonbra.vapulla.service.download

import androidx.compose.runtime.Immutable
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

    /** One plan per platform the app has content for, Windows first. */
    fun plans(app: SteamApp, ownedDepotIds: Set<Int>): List<DownloadPlan> {
        val offered = platforms.filter { os ->
            os in app.osList || app.depots.values.any { os in it.osList }
        }

        return offered.ifEmpty { listOf(OS.none) }.mapNotNull { plan(app, it, ownedDepotIds) }
    }

    /** The depots [app] installs on [os], or null when there is nothing to download. */
    fun plan(app: SteamApp, os: OS, ownedDepotIds: Set<Int>): DownloadPlan? {
        val matching = app.depots.values.filter { depot ->
            // Depots without a public manifest of their own are skipped: ones shared from
            // another app (redistributables, mostly) and ones only on locked branches.
            val manifest = depot.manifests[BRANCH]
            val anyPlatform = depot.osList.all { it == OS.none }

            manifest != null &&
                manifest.gid != 0L &&
                (os == OS.none || anyPlatform || os in depot.osList) &&
                (depot.language.isEmpty() || depot.language == LANGUAGE) &&
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
            installBytes = owned.sumOf { it.manifests.getValue(BRANCH).size },
            downloadBytes = owned.sumOf { it.manifests.getValue(BRANCH).download },
        )
    }
}
