package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.ELicenseFlags
import `in`.dragonbra.vapulla.db.serializers.DateSerializer
import `in`.dragonbra.vapulla.db.serializers.OsEnumSetSerializer
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import java.util.Date
import java.util.EnumSet
import kotlinx.serialization.Serializable

/**
 * A Steam app from PICS, keyed by app ID. Rows start as stubs created from a license's app
 * list and are filled in once the app's PICS info arrives ([receivedPICS]). Only the parts of
 * the app info that the app shows or downloads with are kept; the PICS explorer shows the
 * rest. The structured fields (depots, branches, ufs) are stored as JSON columns.
 */
@Immutable
@Entity(tableName = "steam_app")
data class SteamApp(
    @PrimaryKey val id: Int,
    /** The license that grants the app, or [NO_PACKAGE] for an app that is not owned. */
    val packageId: Int = NO_PACKAGE,
    val ownerAccountId: List<Int> = emptyList(),
    val licenseFlags: EnumSet<ELicenseFlags> = emptyEnumSet(),
    val receivedPICS: Boolean = false,
    val lastChangeNumber: Int = 0,
    /** Minutes played in total, from `Player.GetOwnedGames`. */
    val playtimeForever: Int = 0,
    /** Minutes played in the last two weeks. */
    val playtimeTwoWeeks: Int = 0,
    /** Epoch seconds of the last session, 0 when never played. */
    val lastPlayed: Long = 0L,
    val depots: Map<Int, DepotInfo> = emptyMap(),
    val branches: Map<String, BranchInfo> = emptyMap(),

    // From the `common` section
    val name: String = "",
    val type: AppType = AppType.invalid,
    val osList: EnumSet<OS> = EnumSet.of(OS.none),
    val releaseState: ReleaseState = ReleaseState.disabled,
    /** Epoch seconds, 0 when unknown. */
    val releaseDate: Long = 0L,
    val metacriticScore: Byte = 0,
    val clientIconHash: String = "",
    /** File name of the store header image, by language. */
    val headerImage: Map<Language, String> = emptyMap(),
    val reviewPercentage: Byte = 0,
    val controllerSupport: ControllerSupport = ControllerSupport.none,
    /** True when the app has a public Steam Workshop (`common/workshop_visible`). */
    val workshopVisible: Boolean = false,

    // From the `extended` section
    val developer: String = "",
    val publisher: String = "",
    val isFreeApp: Boolean = false,
    /** Set for a game that registers servers with Steam; see `SteamAppDao.findServerGames`. */
    val gameDir: String = "",
    val noServers: Boolean = false,

    // From the `config` and `ufs` sections
    /** Name of the folder the Steam client installs the app to. */
    val installDir: String = "",
    val ufs: UFS = UFS(),
) {

    /** Steam Cloud is enabled when PICS gives the app a storage quota or file limit. */
    val hasCloudSaves: Boolean
        get() = ufs.quota > 0 || ufs.maxNumFiles > 0

    /** 460x215 store header; see [steamHeaderImageUrl]. */
    val headerImageUrl: String
        get() = steamHeaderImageUrl(id, headerImage)

    /** Steam Cloud limits (`ufs`, the "user file system"). */
    @Serializable
    data class UFS(val quota: Int = 0, val maxNumFiles: Int = 0)

    @Serializable
    data class DepotInfo(
        val depotId: Int,
        /** The DLC the depot belongs to, or [Int.MAX_VALUE] for the app itself. */
        val dlcAppId: Int,
        /** The app the depot is shared from, or [Int.MAX_VALUE] when it is the app's own. */
        val depotFromApp: Int,
        val sharedInstall: Boolean,
        @Serializable(with = OsEnumSetSerializer::class)
        val osList: EnumSet<OS>,
        val osArch: OSArch,
        val manifests: Map<String, ManifestInfo>,
        val encryptedManifests: Map<String, ManifestInfo>,
        // Added later, so rows stored before then decode with the defaults.
        /** `config/language`; empty for a depot that every language installs. */
        val language: String = "",
        val lowViolence: Boolean = false,
    )

    @Serializable
    data class ManifestInfo(val name: String, val gid: Long, val size: Long, val download: Long)

    @Serializable
    data class BranchInfo(
        val name: String,
        val buildId: Long,
        val pwdRequired: Boolean,
        @Serializable(with = DateSerializer::class)
        val timeUpdated: Date,
    )

    enum class OSArch(val keyValName: String) {
        Arch32("32"),
        Arch64("64"),
        Unknown("unknown"),
        ;

        companion object {
            fun from(keyValue: String?): OSArch =
                entries.find { it.keyValName == keyValue } ?: Unknown
        }
    }

    /** A platform as a bit, so that a set of them fits one number. Named as PICS names them. */
    @Suppress("EnumEntryName")
    enum class OS(val code: Int) {
        none(0),
        windows(1),
        macos(2),
        linux(4),
        ;

        companion object {
            fun code(value: EnumSet<OS>): Int = value.fold(none.code) { acc, os -> acc or os.code }

            /** Reads a PICS list such as `windows,linux`; an empty one is [none]. */
            fun from(keyValue: String?): EnumSet<OS> {
                if (keyValue.isNullOrEmpty()) return EnumSet.of(none)

                return keyValue
                    .splitToSequence(',')
                    .map {
                        val trimmed = it.trim()
                        entries.find { os -> os.name.equals(trimmed, ignoreCase = true) } ?: none
                    }
                    .toCollection(emptyEnumSet())
                    .ifEmpty { EnumSet.of(none) }
            }

            fun from(code: Int?): EnumSet<OS> {
                if (code == null || code == 0) return EnumSet.of(none)

                return entries.filterTo(emptyEnumSet()) { os ->
                    os.code != 0 && (code and os.code) == os.code
                }
            }
        }
    }

    @Suppress("EnumEntryName")
    enum class ControllerSupport(val code: Int) {
        none(0),
        partial(1),
        full(2),
        ;

        companion object {
            fun from(keyValue: String?): ControllerSupport =
                entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: none

            fun from(code: Int): ControllerSupport = entries.find { it.code == code } ?: none
        }
    }

    /** The languages Steam has store images in, named as PICS names them. */
    @Suppress("EnumEntryName")
    enum class Language {
        english,
        german,
        french,
        italian,
        koreana,
        spanish,
        schinese,
        sc_schinese,
        tchinese,
        russian,
        japanese,
        polish,
        brazilian,
        latam,
        vietnamese,
        portuguese,
        danish,
        dutch,
        swedish,
        norwegian,
        finnish,
        turkish,
        thai,
        czech,
        unknown,
        ;

        companion object {
            fun from(keyValue: String?): Language =
                entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: unknown
        }
    }

    @Suppress("EnumEntryName")
    enum class ReleaseState(val code: Int) {
        disabled(0),
        released(1),
        prerelease(2),
        ;

        companion object {
            fun from(keyValue: String?): ReleaseState =
                entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: disabled

            fun from(code: Int): ReleaseState = entries.find { it.code == code } ?: disabled
        }
    }

    /**
     * What kind of thing an app is. Each kind is a bit, so that the library can ask for
     * several at once ([toFlags]). The names are Steam's, its spelling of `deprected` included.
     */
    @Suppress("EnumEntryName")
    enum class AppType(val code: Int) {
        invalid(0),
        game(0x01),
        application(0x02),
        tool(0x04),
        demo(0x08),
        deprected(0x10),
        dlc(0x20),
        guide(0x40),
        driver(0x80),
        config(0x100),
        hardware(0x200),
        franchise(0x400),
        video(0x800),
        plugin(0x1000),
        music(0x2000),
        series(0x4000),
        comic(0x8000),
        beta(0x10000),
        shortcut(0x20000),
        ;

        companion object {
            fun from(keyValue: String?): AppType =
                entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: invalid

            fun toFlags(value: EnumSet<AppType>): Int =
                value.fold(0) { acc, appType -> acc or appType.code }

            fun fromCode(code: Int): AppType = entries.find { it.code == code } ?: invalid
        }
    }

    companion object {
        /** The [packageId] of an app no license grants: known to the app, but not owned. */
        const val NO_PACKAGE = Int.MAX_VALUE
    }
}

/**
 * Builds the URL of a store asset referenced by PICS. Values that carry a content hash
 * (e.g. `abc123/header.jpg`) are served from the shared store_item_assets CDN; bare file
 * names (e.g. `header.jpg`) still live on the classic per-app path.
 */
fun steamStoreAssetUrl(appId: Int, asset: String): String = if (asset.contains('/')) {
    "https://shared.steamstatic.com/store_item_assets/steam/apps/$appId/$asset"
} else {
    "https://cdn.akamai.steamstatic.com/steam/apps/$appId/$asset"
}

/**
 * The 460x215 store header of an app. Uses the English asset from PICS when there is one and
 * falls back to the unversioned path, which exists for almost every store app.
 */
fun steamHeaderImageUrl(appId: Int, headerImage: Map<SteamApp.Language, String>): String =
    steamStoreAssetUrl(appId, headerImage[SteamApp.Language.english] ?: "header.jpg")
