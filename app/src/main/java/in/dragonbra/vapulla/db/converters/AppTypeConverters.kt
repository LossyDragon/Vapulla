package `in`.dragonbra.vapulla.db.converters

import androidx.room.TypeConverter
import `in`.dragonbra.vapulla.db.entity.SteamApp
import java.util.EnumSet

/**
 * The enums of [SteamApp] are stored as numbers, its structured fields (depots, branches,
 * images by language, cloud limits) as JSON.
 */
class AppTypeConverters {

    @TypeConverter
    fun fromAppType(appType: SteamApp.AppType): Int = appType.code

    @TypeConverter
    fun toAppType(appType: Int): SteamApp.AppType = SteamApp.AppType.fromCode(appType)

    @TypeConverter
    fun fromOS(os: EnumSet<SteamApp.OS>): Int = SteamApp.OS.code(os)

    @TypeConverter
    fun toOS(os: Int): EnumSet<SteamApp.OS> = SteamApp.OS.from(os)

    @TypeConverter
    fun fromReleaseState(releaseState: SteamApp.ReleaseState): Int = releaseState.code

    @TypeConverter
    fun toReleaseState(releaseState: Int): SteamApp.ReleaseState =
        SteamApp.ReleaseState.from(releaseState)

    @TypeConverter
    fun fromControllerSupport(controllerSupport: SteamApp.ControllerSupport): Int =
        controllerSupport.code

    @TypeConverter
    fun toControllerSupport(controllerSupport: Int): SteamApp.ControllerSupport =
        SteamApp.ControllerSupport.from(controllerSupport)

    @TypeConverter
    fun fromDepots(depots: Map<Int, SteamApp.DepotInfo>): String = DbJson.encodeToString(depots)

    @TypeConverter
    fun toDepots(depots: String): Map<Int, SteamApp.DepotInfo> = DbJson.decodeFromString(depots)

    @TypeConverter
    fun fromBranches(branches: Map<String, SteamApp.BranchInfo>): String =
        DbJson.encodeToString(branches)

    @TypeConverter
    fun toBranches(branches: String): Map<String, SteamApp.BranchInfo> =
        DbJson.decodeFromString(branches)

    @TypeConverter
    fun fromLangMap(langMap: Map<SteamApp.Language, String>): String =
        DbJson.encodeToString(langMap)

    @TypeConverter
    fun toLangMap(langMap: String): Map<SteamApp.Language, String> =
        DbJson.decodeFromString(langMap)

    @TypeConverter
    fun fromUFS(ufs: SteamApp.UFS): String = DbJson.encodeToString(ufs)

    @TypeConverter
    fun toUFS(ufs: String): SteamApp.UFS = DbJson.decodeFromString(ufs)
}
