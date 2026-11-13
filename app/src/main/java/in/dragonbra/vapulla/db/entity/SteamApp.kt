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
 * list and are filled in once the app's PICS info arrives ([receivedPICS]). The structured
 * fields (depots, branches, assets, config, ufs) are stored as JSON columns.
 */
@Immutable
@Entity(tableName = "steam_app")
data class SteamApp(
    @PrimaryKey val id: Int,
    val packageId: Int = Int.MAX_VALUE,
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
    // Common
    val name: String = "",
    val type: AppType = AppType.invalid,
    val osList: EnumSet<OS> = EnumSet.of(OS.none),
    val releaseState: ReleaseState = ReleaseState.disabled,
    val releaseDate: Long = 0L,
    val metacriticScore: Byte = 0,
    val metacriticFullUrl: String = "",
    val logoHash: String = "",
    val logoSmallHash: String = "",
    val iconHash: String = "",
    val clientIconHash: String = "",
    val clientTgaHash: String = "",
    val smallCapsule: Map<Language, String> = emptyMap(),
    val headerImage: Map<Language, String> = emptyMap(),
    val libraryAssets: LibraryAssetsInfo = LibraryAssetsInfo(),
    val primaryGenre: Boolean = false,
    val reviewScore: Byte = 0,
    val reviewPercentage: Byte = 0,
    val controllerSupport: ControllerSupport = ControllerSupport.none,
    /** True when the app has a public Steam Workshop (`common/workshop_visible`). */
    val workshopVisible: Boolean = false,

    // Extended
    val demoOfAppId: Int = Int.MAX_VALUE,
    val developer: String = "",
    val publisher: String = "",
    val homepageUrl: String = "",
    val gameManualUrl: String = "",
    val loadAllBeforeLaunch: Boolean = false,
    val dlcAppIds: List<Int> = emptyList(),
    val isFreeApp: Boolean = false,
    val dlcForAppId: Int = Int.MAX_VALUE,
    val mustOwnAppToPurchase: Int = Int.MAX_VALUE,
    val dlcAvailableOnStore: Boolean = false,
    val optionalDlc: Boolean = false,
    val gameDir: String = "",
    val installScript: String = "",
    val noServers: Boolean = false,
    val order: Boolean = false,
    val primaryCache: Int = 0,
    val validOSList: EnumSet<OS> = EnumSet.of(OS.none),
    val thirdPartyCdKey: Boolean = false,
    val visibleOnlyWhenInstalled: Boolean = false,
    val visibleOnlyWhenSubscribed: Boolean = false,
    val launchEulaUrl: String = "",

    // Config
    val requireDefaultInstallFolder: Boolean = false,
    val contentType: Int = 0,
    val installDir: String = "",
    val useLaunchCmdLine: Boolean = false,
    val launchWithoutWorkshopUpdates: Boolean = false,
    val useMms: Boolean = false,
    val installScriptSignature: String = "",
    val installScriptOverride: Boolean = false,
    val config: ConfigInfo = ConfigInfo(),
    val ufs: UFS = UFS(),
) {

    /** Steam Cloud is enabled when PICS gives the app a storage quota or file limit. */
    val hasCloudSaves: Boolean
        get() = ufs.quota > 0 || ufs.maxNumFiles > 0

    val logoUrl: String
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$logoHash.jpg"
    val logoSmallUrl: String
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$logoSmallHash.jpg"
    val iconUrl: String
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$iconHash.jpg"
    val clientIconUrl: String
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$clientIconHash.ico"
    val clientTgaUrl: String
        get() = "https://steamcdn-a.akamaihd.net/steamcommunity/public/images/apps/$id/$clientTgaHash.tga"

    // source: https://github.com/Nemirtingas/games-infos/blob/3915100198bac34553b3c862f9e295d277f5520a/steam_retriever/Program.cs#L589C43-L589C89
    fun getSmallCapsuleUrl(language: Language = Language.english): String? =
        smallCapsule[language]?.let {
            steamStoreAssetUrl(id, it)
        }

    fun getHeaderImageUrl(language: Language = Language.english): String? =
        headerImage[language]?.let {
            steamStoreAssetUrl(id, it)
        }

    fun getCapsuleUrl(language: Language = Language.english, large: Boolean = false): String? =
        if (large) {
            libraryAssets.libraryCapsule.image2x[language]?.let {
                steamStoreAssetUrl(id, it)
            }
        } else {
            libraryAssets.libraryCapsule.image[language]?.let {
                steamStoreAssetUrl(id, it)
            }
        }

    fun getHeroUrl(language: Language = Language.english, large: Boolean = false): String? =
        if (large) {
            libraryAssets.libraryHero.image2x[language]?.let {
                "https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/$id/$it"
            }
        } else {
            libraryAssets.libraryHero.image[language]?.let {
                "https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/$id/$it"
            }
        }

    fun getLogoUrl(language: Language = Language.english, large: Boolean = false): String? =
        if (large) {
            libraryAssets.libraryLogo.image2x[language]?.let {
                "https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/$id/$it"
            }
        } else {
            libraryAssets.libraryLogo.image[language]?.let {
                "https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/$id/$it"
            }
        }

    @Serializable
    data class UFS(
        val quota: Int = 0,
        val maxNumFiles: Int = 0,
        val saveFilePatterns: List<SaveFilePattern> = emptyList(),
    )

    @Serializable
    data class SaveFilePattern(val root: PathType, val path: String, val pattern: String)

    @Serializable
    data class ConfigInfo(
        val installDir: String = "",
        val launch: List<LaunchInfo> = emptyList(),
        val steamControllerTemplateIndex: Int = 0,
        val steamControllerTouchTemplateIndex: Int = 0,
        // val steamControllerTouchConfigDetails: TouchConfigDetails,
    )

    @Serializable
    data class LaunchInfo(
        val executable: String,
        val workingDir: String,
        val description: String,
        val type: String,
        @Serializable(with = OsEnumSetSerializer::class)
        val configOS: EnumSet<OS>,
        val configArch: OSArch,
    )

    @Serializable
    data class LibraryAssetsInfo(
        val libraryCapsule: LibraryCapsuleInfo = LibraryCapsuleInfo(),
        val libraryHero: LibraryHeroInfo = LibraryHeroInfo(),
        val libraryLogo: LibraryLogoInfo = LibraryLogoInfo(),
    )

    @Serializable
    data class LibraryCapsuleInfo(
        val image: Map<Language, String> = emptyMap(),
        val image2x: Map<Language, String> = emptyMap(),
    )

    @Serializable
    data class LibraryHeroInfo(
        val image: Map<Language, String> = emptyMap(),
        val image2x: Map<Language, String> = emptyMap(),
    )

    @Serializable
    data class LibraryLogoInfo(
        val image: Map<Language, String> = emptyMap(),
        val image2x: Map<Language, String> = emptyMap(),
    )

    @Serializable
    data class DepotInfo(
        val depotId: Int,
        val dlcAppId: Int,
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
            fun from(keyValue: String?): OSArch {
                if (keyValue == null) return Unknown

                return entries.find { it.keyValName == keyValue } ?: Unknown
            }
        }
    }

    @Suppress("EnumEntryName")
    enum class OS(val code: Int) {
        none(0),
        windows(1),
        macos(2),
        linux(4),
        ;

        companion object {
            fun code(value: EnumSet<OS>): Int = value.fold(none.code) { acc, os -> acc or os.code }

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

    enum class ControllerSupport(val code: Int) {
        none(0),
        partial(1),
        full(2),
        ;

        companion object {
            fun from(keyValue: String?): ControllerSupport {
                if (keyValue == null) return none

                return entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: none
            }

            fun from(code: Int): ControllerSupport = entries.find { it.code == code } ?: none
        }
    }

    enum class PathType {
        GameInstall,
        SteamUserData,
        WinMyDocuments,
        WinAppDataLocal,
        WinAppDataLocalLow,
        WinAppDataRoaming,
        WinSavedGames,
        LinuxHome,
        LinuxXdgDataHome,
        LinuxXdgConfigHome,
        MacHome,
        MacAppSupport,
        None,
        ;

        companion object {
            val DEFAULT = SteamUserData

            /** Accepts the bare name or Steam's `%Name%` placeholder form, case-insensitively. */
            fun from(keyValue: String?): PathType {
                val name = keyValue?.trim('%') ?: return None
                return entries.find { it.name.equals(name, ignoreCase = true) } ?: None
            }
        }
    }

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
            fun from(keyValue: String?): Language {
                if (keyValue == null) return unknown

                return entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: unknown
            }
        }
    }

    enum class ReleaseState(val code: Int) {
        disabled(0),
        released(1),
        prerelease(2),
        ;

        companion object {
            fun from(keyValue: String?): ReleaseState {
                if (keyValue == null) return disabled

                return entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: disabled
            }

            fun from(code: Int): ReleaseState = entries.find { it.code == code } ?: disabled
        }
    }

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
            fun from(keyValue: String?): AppType {
                if (keyValue == null) return invalid

                return entries.find { it.name.equals(keyValue, ignoreCase = true) } ?: invalid
            }

            fun fromFlags(flags: Int): EnumSet<AppType> {
                if (flags == 0) return EnumSet.of(invalid)

                return entries.filterTo(emptyEnumSet()) { appType ->
                    appType.code != 0 && (flags and appType.code) == appType.code
                }
            }

            fun toFlags(value: EnumSet<AppType>): Int =
                value.fold(0) { acc, appType -> acc or appType.code }

            fun fromCode(code: Int): AppType = entries.find { it.code == code } ?: invalid
        }
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
