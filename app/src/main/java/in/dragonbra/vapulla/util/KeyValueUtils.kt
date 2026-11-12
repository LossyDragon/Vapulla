package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.db.entity.SteamApp.BranchInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.ConfigInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.ControllerSupport
import `in`.dragonbra.vapulla.db.entity.SteamApp.DepotInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.Language
import `in`.dragonbra.vapulla.db.entity.SteamApp.LaunchInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.LibraryAssetsInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.LibraryCapsuleInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.LibraryHeroInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.LibraryLogoInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.ManifestInfo
import `in`.dragonbra.vapulla.db.entity.SteamApp.OS
import `in`.dragonbra.vapulla.db.entity.SteamApp.OSArch
import `in`.dragonbra.vapulla.db.entity.SteamApp.PathType
import `in`.dragonbra.vapulla.db.entity.SteamApp.ReleaseState
import `in`.dragonbra.vapulla.db.entity.SteamApp.SaveFilePattern
import `in`.dragonbra.vapulla.db.entity.SteamApp.UFS
import java.util.Date
import timber.log.Timber

/**
 * Maps a PICS app info [KeyValue] tree (the `appinfo` VDF Steam sends) to a [SteamApp].
 * Missing nodes read as empty values, so partial app info still produces a row.
 */
fun KeyValue.generateSteamApp(): SteamApp {
    val commonNode = this["common"]
    val extendedNode = this["extended"]
    val configNode = this["config"]
    val depotsNode = this["depots"]
    val libraryAssetsNode = commonNode["library_assets_full"]

    return SteamApp(
        id = this["appid"].asInteger(Int.MAX_VALUE),
        depots = depotsNode.children
            .asSequence()
            .filter { it.name?.toIntOrNull() != null }
            .associate { currentDepot ->
                val depotId = currentDepot.name!!.toInt()
                val manifests = currentDepot["manifests"].children.generateManifest()
                val encryptedManifests =
                    currentDepot["encryptedManifests"].children.generateManifest()

                depotId to DepotInfo(
                    depotId = depotId,
                    dlcAppId = currentDepot["dlcappid"].asInteger(Int.MAX_VALUE),
                    depotFromApp = currentDepot["depotfromapp"].asInteger(Int.MAX_VALUE),
                    sharedInstall = currentDepot["sharedinstall"].asBoolean(),
                    osList = OS.from(currentDepot["config"]["oslist"].value),
                    osArch = OSArch.from(currentDepot["config"]["osarch"].value),
                    manifests = manifests,
                    encryptedManifests = encryptedManifests,
                )
            },
        branches = depotsNode["branches"].children.associate {
            it.name!! to BranchInfo(
                name = it.name!!,
                buildId = it["buildid"].asLong(),
                pwdRequired = it["pwdrequired"].asBoolean(),
                timeUpdated = Date(it["timeupdated"].asLong() * 1000L),
            )
        },
        name = commonNode["name"].value.orEmpty(),
        type = AppType.from(commonNode["type"].value),
        osList = OS.from(commonNode["oslist"].value),
        releaseState = ReleaseState.from(commonNode["releasestate"].value),
        releaseDate = commonNode["steam_release_date"].asLong(),
        metacriticScore = commonNode["metacritic_score"].asByte(),
        metacriticFullUrl = commonNode["metacritic_fullurl"].value.orEmpty(),
        logoHash = commonNode["logo"].value.orEmpty(),
        logoSmallHash = commonNode["logo_small"].value.orEmpty(),
        iconHash = commonNode["icon"].value.orEmpty(),
        clientIconHash = commonNode["clienticon"].value.orEmpty(),
        clientTgaHash = commonNode["clienttga"].value.orEmpty(),
        smallCapsule = commonNode["small_capsule"].children.toLangImgMap(),
        headerImage = commonNode["header_image"].children.toLangImgMap(),
        libraryAssets = LibraryAssetsInfo(
            libraryCapsule = LibraryCapsuleInfo(
                image = libraryAssetsNode["library_capsule"]["image"].children.toLangImgMap(),
                image2x = libraryAssetsNode["library_capsule"]["image2x"].children.toLangImgMap(),
            ),
            libraryHero = LibraryHeroInfo(
                image = libraryAssetsNode["library_hero"]["image"].children.toLangImgMap(),
                image2x = libraryAssetsNode["library_hero"]["image2x"].children.toLangImgMap(),
            ),
            libraryLogo = LibraryLogoInfo(
                image = libraryAssetsNode["library_logo"]["image"].children.toLangImgMap(),
                image2x = libraryAssetsNode["library_logo"]["image2x"].children.toLangImgMap(),
            ),
        ),
        primaryGenre = commonNode["primary_genre"].asBoolean(),
        reviewScore = commonNode["review_score"].asByte(),
        reviewPercentage = commonNode["review_percentage"].asByte(),
        controllerSupport = ControllerSupport.from(commonNode["controller_support"].value),
        workshopVisible = commonNode["workshop_visible"].asBoolean(),
        demoOfAppId = extendedNode["demoofappid"].asInteger(),
        developer = extendedNode["developer"].value.orEmpty(),
        publisher = extendedNode["publisher"].value.orEmpty(),
        homepageUrl = extendedNode["homepage"].value.orEmpty(),
        gameManualUrl = extendedNode["gamemanualurl"].value.orEmpty(),
        loadAllBeforeLaunch = extendedNode["loadallbeforelaunch"].asBoolean(),
        dlcAppIds = emptyList(),
        isFreeApp = extendedNode["isfreeapp"].asBoolean(),
        dlcForAppId = extendedNode["dlcforappid"].asInteger(),
        mustOwnAppToPurchase = extendedNode["mustownapptopurchase"].asInteger(),
        dlcAvailableOnStore = extendedNode["dlcavailableonstore"].asBoolean(),
        optionalDlc = extendedNode["optionaldlc"].asBoolean(),
        gameDir = extendedNode["gamedir"].value.orEmpty(),
        installScript = extendedNode["installscript"].value.orEmpty(),
        noServers = extendedNode["noservers"].asBoolean(),
        order = extendedNode["order"].asBoolean(),
        primaryCache = extendedNode["primarycache"].asInteger(),
        validOSList = OS.from(extendedNode["validoslist"].value),
        thirdPartyCdKey = extendedNode["thirdpartycdkey"].asBoolean(),
        visibleOnlyWhenInstalled = extendedNode["visibleonlywheninstalled"].asBoolean(),
        visibleOnlyWhenSubscribed = extendedNode["visibleonlywhensubscribed"].asBoolean(),
        launchEulaUrl = extendedNode["launcheula"].value.orEmpty(),
        requireDefaultInstallFolder = configNode["requiredefaultinstallfolder"].asBoolean(),
        contentType = configNode["contentType"].asInteger(),
        installDir = configNode["installdir"].value.orEmpty(),
        useLaunchCmdLine = configNode["uselaunchcommandline"].asBoolean(),
        launchWithoutWorkshopUpdates = configNode["launchwithoutworkshopupdates"].asBoolean(),
        useMms = configNode["usemms"].asBoolean(),
        installScriptSignature = configNode["installscriptsignature"].value.orEmpty(),
        installScriptOverride = configNode["installscriptoverride"].asBoolean(),
        config = ConfigInfo(
            installDir = configNode["installdir"].value.orEmpty(),
            launch = configNode["launch"].children.map {
                LaunchInfo(
                    executable = it["executable"].value?.replace('\\', '/').orEmpty(),
                    workingDir = it["workingdir"].value?.replace('\\', '/').orEmpty(),
                    description = it["description"].value.orEmpty(),
                    type = it["type"].value.orEmpty(),
                    configOS = OS.from(it["config"]["oslist"].value),
                    configArch = OSArch.from(it["config"]["osarch"].value),
                )
            },
            steamControllerTemplateIndex = configNode["steamcontrollertemplateindex"].asInteger(),
            steamControllerTouchTemplateIndex = configNode["steamcontrollertouchtemplateindex"].asInteger(),
        ),
        ufs = UFS(
            quota = this["ufs"]["quota"].asInteger(),
            maxNumFiles = this["ufs"]["maxnumfiles"].asInteger(),
            saveFilePatterns = this["ufs"]["savefiles"].children.map {
                SaveFilePattern(
                    root = PathType.from(it["root"].value),
                    path = it["path"].value.orEmpty(),
                    pattern = it["pattern"].value.orEmpty(),
                )
            },
        ),
    )
}

private fun List<KeyValue>.generateManifest(): Map<String, ManifestInfo> = associate { manifest ->
    manifest.name!! to ManifestInfo(
        name = manifest.name!!,
        gid = manifest["gid"].asLong(),
        size = manifest["size"].asLong(),
        download = manifest["download"].asLong(),
    )
}

private fun List<KeyValue>.toLangImgMap(): Map<Language, String> = asSequence().mapNotNull { kv ->
    Language.from(kv.name)
        .takeIf { it != Language.unknown }
        ?.let { it to kv.value!! }
}.toMap()

/** Debug helper: logs the whole [KeyValue] tree, one indented line per node. */
@Suppress("unused")
fun KeyValue.printAllKeyValues(depth: Int = 0) {
    val indent = "\t".repeat(depth + 1)

    if (children.isEmpty()) {
        Timber.i("$indent$name: $value")
    } else {
        Timber.i("$indent$name")
        children.forEach { it.printAllKeyValues(depth + 1) }
    }
}
