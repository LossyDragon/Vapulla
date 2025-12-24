package `in`.dragonbra.vapulla.data.converters

import androidx.room.TypeConverter
import `in`.dragonbra.vapulla.data.entity.SteamApp
import java.util.EnumSet
import kotlinx.serialization.json.Json

class AppTypeConverters {

    @TypeConverter
    fun fromPathType(pathType: SteamApp.PathType): String = pathType.name

    @TypeConverter
    fun toPathType(value: String): SteamApp.PathType = SteamApp.PathType.from(value)

    @TypeConverter
    fun toAppType(appType: Int): SteamApp.AppType = SteamApp.AppType.fromCode(appType)

    @TypeConverter
    fun fromAppType(appType: SteamApp.AppType): Int = appType.code

    @TypeConverter
    fun toOS(os: Int): EnumSet<SteamApp.OS> = SteamApp.OS.from(os)

    @TypeConverter
    fun fromOS(os: EnumSet<SteamApp.OS>): Int = SteamApp.OS.code(os)

    @TypeConverter
    fun toReleaseState(releaseState: Int): SteamApp.ReleaseState =
        SteamApp.ReleaseState.from(releaseState)

    @TypeConverter
    fun fromReleaseState(releaseState: SteamApp.ReleaseState): Int = releaseState.code

    @TypeConverter
    fun toControllerSupport(controllerSupport: Int): SteamApp.ControllerSupport =
        SteamApp.ControllerSupport.from(controllerSupport)

    @TypeConverter
    fun fromControllerSupport(controllerSupport: SteamApp.ControllerSupport): Int =
        controllerSupport.code

    @TypeConverter
    fun toDepots(depots: String): Map<Int, SteamApp.DepotInfo> =
        Json.decodeFromString<Map<Int, SteamApp.DepotInfo>>(depots)

    @TypeConverter
    fun fromDepots(depots: Map<Int, SteamApp.DepotInfo>): String = Json.encodeToString(depots)

    @TypeConverter
    fun toBranches(branches: String): Map<String, SteamApp.BranchInfo> =
        Json.decodeFromString<Map<String, SteamApp.BranchInfo>>(branches)

    @TypeConverter
    fun fromBranches(branches: Map<String, SteamApp.BranchInfo>): String =
        Json.encodeToString(branches)

    @TypeConverter
    fun toLangMap(langMap: String): Map<SteamApp.Language, String> =
        Json.decodeFromString<Map<SteamApp.Language, String>>(langMap)

    @TypeConverter
    fun fromLangMap(langMap: Map<SteamApp.Language, String>): String = Json.encodeToString(langMap)

    @TypeConverter
    fun toLibraryAssetsInfo(langMap: String): SteamApp.LibraryAssetsInfo =
        Json.decodeFromString<SteamApp.LibraryAssetsInfo>(langMap)

    @TypeConverter
    fun fromLibraryAssetsInfo(langMap: SteamApp.LibraryAssetsInfo): String =
        Json.encodeToString(langMap)

    @TypeConverter
    fun toConfigInfo(configInfo: String): SteamApp.ConfigInfo =
        Json.decodeFromString<SteamApp.ConfigInfo>(configInfo)

    @TypeConverter
    fun fromConfigInfo(configInfo: SteamApp.ConfigInfo): String = Json.encodeToString(configInfo)

    @TypeConverter
    fun toUFS(ufs: String): SteamApp.UFS = Json.decodeFromString<SteamApp.UFS>(ufs)

    @TypeConverter
    fun fromUFS(ufs: SteamApp.UFS): String = Json.encodeToString(ufs)
}
