package `in`.dragonbra.vapulla.util

import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.data.entity.SteamApp.DepotInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.OS
import `in`.dragonbra.vapulla.data.entity.SteamApp.OSArch
import `in`.dragonbra.vapulla.data.entity.SteamApp.LibraryAssetsInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.LibraryCapsuleInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.LibraryHeroInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.LibraryLogoInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.UFS
import `in`.dragonbra.vapulla.data.entity.SteamApp.ManifestInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.Language
import `in`.dragonbra.vapulla.data.entity.SteamApp.AppType
import `in`.dragonbra.vapulla.data.entity.SteamApp.BranchInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.ReleaseState
import `in`.dragonbra.vapulla.data.entity.SteamApp.ControllerSupport
import `in`.dragonbra.vapulla.data.entity.SteamApp.ConfigInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.LaunchInfo
import `in`.dragonbra.vapulla.data.entity.SteamApp.SaveFilePattern
import `in`.dragonbra.vapulla.data.entity.SteamApp.PathType
import timber.log.Timber
import java.util.Date

/**
 * Extension functions relating to [KeyValue] as the receiver type.
 */

fun KeyValue.generateSteamApp(): SteamApp {
    val commonNode = this["common"]
    val extendedNode = this["extended"]
    val commonExtendedNode = commonNode["extended"]
    val configNode = this["config"]
    val commonConfigNode = commonNode["config"]
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
        demoOfAppId = commonExtendedNode["demoofappid"].asInteger(),
        developer = extendedNode["developer"].value.orEmpty(),
        publisher = extendedNode["publisher"].value.orEmpty(),
        homepageUrl = extendedNode["homepage"].value.orEmpty(),
        gameManualUrl = commonExtendedNode["gamemanualurl"].value.orEmpty(),
        loadAllBeforeLaunch = commonExtendedNode["loadallbeforelaunch"].asBoolean(),
        dlcAppIds = emptyList(),
        isFreeApp = commonExtendedNode["isfreeapp"].asBoolean(),
        dlcForAppId = commonExtendedNode["dlcforappid"].asInteger(),
        mustOwnAppToPurchase = commonExtendedNode["mustownapptopurchase"].asInteger(),
        dlcAvailableOnStore = commonExtendedNode["dlcavailableonstore"].asBoolean(),
        optionalDlc = commonExtendedNode["optionaldlc"].asBoolean(),
        gameDir = commonExtendedNode["gamedir"].value.orEmpty(),
        installScript = commonExtendedNode["installscript"].value.orEmpty(),
        noServers = commonExtendedNode["noservers"].asBoolean(),
        order = commonExtendedNode["order"].asBoolean(),
        primaryCache = commonExtendedNode["primarycache"].asInteger(),
        validOSList = OS.from(commonExtendedNode["validoslist"].value),
        thirdPartyCdKey = commonExtendedNode["thirdpartycdkey"].asBoolean(),
        visibleOnlyWhenInstalled = commonExtendedNode["visibleonlywheninstalled"].asBoolean(),
        visibleOnlyWhenSubscribed = commonExtendedNode["visibleonlywhensubscribed"].asBoolean(),
        launchEulaUrl = commonExtendedNode["launcheula"].value.orEmpty(),
        requireDefaultInstallFolder = commonConfigNode["requiredefaultinstallfolder"].asBoolean(),
        contentType = commonConfigNode["contentType"].asInteger(),
        installDir = commonConfigNode["installdir"].value.orEmpty(),
        useLaunchCmdLine = commonConfigNode["uselaunchcommandline"].asBoolean(),
        launchWithoutWorkshopUpdates = commonConfigNode["launchwithoutworkshopupdates"].asBoolean(),
        useMms = commonConfigNode["usemms"].asBoolean(),
        installScriptSignature = commonConfigNode["installscriptsignature"].value.orEmpty(),
        installScriptOverride = commonConfigNode["installscriptoverride"].asBoolean(),
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

@Suppress("unused")
fun KeyValue.printAllKeyValues(depth: Int = 0) {
    val parent = this
    var tabString = ""

    for (i in 0..depth) {
        tabString += "\t"
    }

    if (parent.children.isNotEmpty()) {
        Timber.i("$tabString${parent.name}")

        for (child in parent.children) {
            child.printAllKeyValues(depth + 1)
        }
    } else {
        Timber.i("$tabString${parent.name}: ${parent.value}")
    }
}