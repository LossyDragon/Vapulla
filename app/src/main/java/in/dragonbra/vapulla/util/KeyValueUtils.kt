package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.db.entity.SteamApp.BranchInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.ControllerSupport
import `in`.dragonbra.vapulla.db.entity.SteamApp.DepotInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.Language
import `in`.dragonbra.vapulla.db.entity.SteamApp.ManifestInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.OS
import `in`.dragonbra.vapulla.db.entity.SteamApp.OSArch
import `in`.dragonbra.vapulla.db.entity.SteamApp.ReleaseState
import `in`.dragonbra.vapulla.db.entity.SteamApp.UFS
import java.util.Date

/**
 * Maps a PICS app info [KeyValue] tree (the `appinfo` VDF Steam sends) to a [SteamApp].
 * Missing nodes read as empty values, so partial app info still produces a row.
 */
fun KeyValue.generateSteamApp(): SteamApp {
    val common = this["common"]
    val extended = this["extended"]
    val depots = this["depots"]
    val ufs = this["ufs"]

    return SteamApp(
        id = this["appid"].asInteger(Int.MAX_VALUE),
        depots = depots.toDepots(),
        branches = depots["branches"].toBranches(),
        name = common["name"].value.orEmpty(),
        type = AppType.from(common["type"].value),
        osList = OS.from(common["oslist"].value),
        releaseState = ReleaseState.from(common["releasestate"].value),
        releaseDate = common["steam_release_date"].asLong(),
        metacriticScore = common["metacritic_score"].asByte(),
        clientIconHash = common["clienticon"].value.orEmpty(),
        headerImage = common["header_image"].toLanguageMap(),
        reviewPercentage = common["review_percentage"].asByte(),
        controllerSupport = ControllerSupport.from(common["controller_support"].value),
        workshopVisible = common["workshop_visible"].asBoolean(),
        developer = extended["developer"].value.orEmpty(),
        publisher = extended["publisher"].value.orEmpty(),
        isFreeApp = extended["isfreeapp"].asBoolean(),
        gameDir = extended["gamedir"].value.orEmpty(),
        noServers = extended["noservers"].asBoolean(),
        installDir = this["config"]["installdir"].value.orEmpty(),
        ufs = UFS(
            quota = ufs["quota"].asInteger(),
            maxNumFiles = ufs["maxnumfiles"].asInteger(),
        ),
    )
}

/** The depots of a `depots` node: its children named with a number. The rest is settings. */
private fun KeyValue.toDepots(): Map<Int, DepotInfo> = children.mapNotNull { depot ->
    val depotId = depot.name?.toIntOrNull() ?: return@mapNotNull null
    val config = depot["config"]

    depotId to DepotInfo(
        depotId = depotId,
        dlcAppId = depot["dlcappid"].asInteger(Int.MAX_VALUE),
        depotFromApp = depot["depotfromapp"].asInteger(Int.MAX_VALUE),
        sharedInstall = depot["sharedinstall"].asBoolean(),
        osList = OS.from(config["oslist"].value),
        osArch = OSArch.from(config["osarch"].value),
        manifests = depot["manifests"].toManifests(),
        encryptedManifests = depot["encryptedManifests"].toManifests(),
        language = config["language"].value.orEmpty(),
        lowViolence = config["lowviolence"].asBoolean(),
    )
}.toMap()

/** The manifests of a depot, by the branch each one is for. */
private fun KeyValue.toManifests(): Map<String, ManifestInfo> = children.mapNotNull { manifest ->
    val branch = manifest.name ?: return@mapNotNull null

    branch to ManifestInfo(
        name = branch,
        gid = manifest["gid"].asLong(),
        size = manifest["size"].asLong(),
        download = manifest["download"].asLong(),
    )
}.toMap()

private fun KeyValue.toBranches(): Map<String, BranchInfo> = children.mapNotNull { branch ->
    val name = branch.name ?: return@mapNotNull null

    name to BranchInfo(
        name = name,
        buildId = branch["buildid"].asLong(),
        pwdRequired = branch["pwdrequired"].asBoolean(),
        timeUpdated = Date(branch["timeupdated"].asLong() * 1000L),
    )
}.toMap()

/** An image's file name by language; languages the app does not know are left out. */
private fun KeyValue.toLanguageMap(): Map<Language, String> = children.mapNotNull { image ->
    val language = Language.from(image.name).takeIf { it != Language.unknown }
    val file = image.value

    if (language != null && file != null) language to file else null
}.toMap()
