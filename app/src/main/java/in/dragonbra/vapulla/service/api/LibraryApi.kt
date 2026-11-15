package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetAchievementsProgress_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetFriendsGameplayInfo_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameAchievements_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.callback.UserStatsCallback
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamApp.ManifestInfo
import `in`.dragonbra.vapulla.util.SteamTimeoutException
import `in`.dragonbra.vapulla.util.generateSteamApp
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Library features: player counts, achievements and stats via the `SteamUserStats` handler,
 * ownership and playtime via the `Player` unified service, and PICS info and free licenses
 * via the `SteamApps` handler.
 */
class LibraryApi(
    private val steamApps: SteamApps,
    private val userStats: SteamUserStats,
    private val player: Player,
    private val localSteamId: () -> SteamID?,
) {
    /** Players in-game right now, or null if Steam does not report it for this app. */
    suspend fun currentPlayers(appId: Int): Int? {
        val callback = userStats.getNumberOfCurrentPlayers(appId)
            .awaitOrNull("GetNumberOfCurrentPlayers")
            ?: return null

        return callback.numPlayers.takeIf { callback.result == EResult.OK }
    }

    /**
     * Every game the account can play, including family-shared ones, with playtime. Empty
     * when Steam does not answer.
     */
    suspend fun ownedGames(): List<OwnedGame> {
        val steamId = localSteamId() ?: return emptyList()
        val request = CPlayer_GetOwnedGames_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setIncludeAppinfo(true)
            .setIncludePlayedFreeGames(true)
            .setIncludeFreeSub(true)
            .setIncludeFamilyLicenses(true)
            .setSkipUnvettedApps(false)
            .build()

        val body = player.getOwnedGames(request).bodyOrNull("GetOwnedGames") ?: return emptyList()

        return body.gamesList.map { game ->
            OwnedGame(
                appId = game.appid,
                name = game.name,
                playtimeForever = game.playtimeForever,
                playtimeTwoWeeks = game.playtime2Weeks,
                lastPlayed = game.rtimeLastPlayed.toUInt().toLong(),
            )
        }
    }

    /** Which friends play, played, own or want [appId]. Null when Steam does not say. */
    suspend fun friendsGameplay(appId: Int): FriendsGameplay? {
        val request = CPlayer_GetFriendsGameplayInfo_Request.newBuilder()
            .setAppid(appId)
            .build()

        val body = player.getFriendsGameplayInfo(request).bodyOrNull("GetFriendsGameplayInfo")
            ?: return null

        return FriendsGameplay(
            playingNow = body.inGameList.map { it.steamid }.toImmutableList(),
            playedRecently = body.playedRecentlyList.map { it.steamid }.toImmutableList(),
            playedBefore = body.playedEverList.map { it.steamid }.toImmutableList(),
            own = body.ownsList.map { it.steamid }.toImmutableList(),
            wishlisted = body.inWishlistList.map { it.steamid }.toImmutableList(),
        )
    }

    /**
     * Achievement completion for the given apps, keyed by app ID. Empty when Steam does not
     * answer.
     */
    suspend fun achievementsProgress(appIds: List<Int>): Map<Int, AchievementProgress> {
        val steamId = localSteamId() ?: return emptyMap()
        val request = CPlayer_GetAchievementsProgress_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .addAllAppids(appIds)
            .setIncludeUnvettedApps(true)
            .build()

        val body = player.getAchievementsProgress(request).bodyOrNull("GetAchievementsProgress")
            ?: return emptyMap()

        return body.achievementProgressList.associate { progress ->
            progress.appid to AchievementProgress(
                appId = progress.appid,
                unlocked = progress.unlocked,
                total = progress.total,
                percentage = progress.percentage,
            )
        }
    }

    /**
     * The app's current PICS info straight from Steam, or null if Steam does not know the app.
     * Used where the stored row may be out of date, such as picking depots to download.
     *
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun appInfo(appId: Int): SteamApp? =
        productInfo(appId, isApp = true)?.keyValues?.generateSteamApp()

    /**
     * Opens a [branch] of [appId] that is behind a password. Steam turns the password into a
     * key, and with the key hands out the part of the app info it otherwise keeps encrypted.
     *
     * @return the manifest each depot has on the branch, by depot ID; null when Steam does
     *   not accept the password for this branch.
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun unlockBranch(
        appId: Int,
        branch: String,
        password: String,
    ): Map<Int, ManifestInfo>? {
        val key = steamApps.checkAppBetaPassword(appId, password).awaitOrThrow()
            .betaPasswords[branch]
            ?: return null
        val token = accessToken(appId, isApp = true)
        val depots = steamApps.picsGetPrivateBeta(appId, token, branch, key).awaitOrThrow()
            .depotSection

        return depots.children.mapNotNull { depot ->
            val depotId = depot.name?.toIntOrNull() ?: return@mapNotNull null
            val manifest = depot["manifests"][branch]
            if (manifest["gid"].value == null) return@mapNotNull null

            depotId to ManifestInfo(
                name = branch,
                gid = manifest["gid"].asLong(),
                size = manifest["size"].asLong(),
                download = manifest["download"].asLong(),
            )
        }.toMap()
    }

    /**
     * An app's or a package's PICS info exactly as Steam sends it, or null if Steam does not
     * know the ID. The account's access token is asked for first, since without it Steam
     * leaves out everything that is not public.
     *
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun productInfo(id: Int, isApp: Boolean): PicsProduct? {
        val token = accessToken(id, isApp)
        val request = PICSRequest(id, token)

        val job = if (isApp) {
            steamApps.picsGetProductInfo(app = request)
        } else {
            steamApps.picsGetProductInfo(`package` = request)
        }

        val info = job.awaitOrThrow().results
            .firstNotNullOfOrNull { if (isApp) it.apps[id] else it.packages[id] }
            ?: return null

        return PicsProduct(
            id = id,
            isApp = isApp,
            changeNumber = info.changeNumber,
            token = token,
            missingToken = info.isMissingToken,
            keyValues = info.keyValues,
        )
    }

    /** The account's PICS access token for an app or a package; 0 when Steam gives none. */
    private suspend fun accessToken(id: Int, isApp: Boolean): Long {
        val tokens = if (isApp) {
            steamApps.picsGetAccessTokens(app = id).awaitOrThrow().appTokens
        } else {
            steamApps.picsGetAccessTokens(`package` = id).awaitOrThrow().packageTokens
        }
        return tokens[id] ?: 0L
    }

    /**
     * Adds a free-to-play or free-on-demand app to the account.
     *
     * @throws SteamTimeoutException if Steam does not answer.
     */
    suspend fun requestFreeLicense(appId: Int): FreeLicenseResult {
        val callback = steamApps.requestFreeLicense(appId).awaitOrThrow()
        return FreeLicenseResult(
            result = callback.result,
            grantedApps = callback.grantedApps.toImmutableList(),
            grantedPackages = callback.grantedPackages.toImmutableList(),
        )
    }

    /**
     * The account's achievements and stats for a game. `SteamUserStats.getUserStats` returns
     * the values together with the game's stats schema, which names them; how rare each
     * achievement is comes from `Player.GetGameAchievements`.
     *
     * @return null when Steam refuses or does not answer; it refuses, for one, a game that
     *   has no stats at all.
     */
    suspend fun gameStats(appId: Int): GameStats? {
        val steamId = localSteamId() ?: return null

        val callback = userStats.getUserStats(appId, steamId).awaitOrNull("GetUserStats")
            ?: return null
        if (callback.result != EResult.OK) {
            Timber.w("GetUserStats for $appId answered ${callback.result}")
            return null
        }

        val globalPercent = globalAchievementPercentages(appId)

        val achievements = callback.getExpandedAchievements()
            // Blocks the schema could not expand carry no name and cannot be shown.
            .filter { !it.name.isNullOrEmpty() }
            .map { achievement ->
                val name = achievement.name.orEmpty()
                val icon = if (achievement.isUnlocked) achievement.icon else achievement.iconGray

                Achievement(
                    name = name,
                    title = achievement.displayName?.takeIf { it.isNotBlank() } ?: name,
                    description = achievement.description.orEmpty(),
                    iconUrl = icon?.takeIf { it.isNotBlank() }
                        ?.let { "$ACHIEVEMENT_ICON_URL$appId/$it" }
                        .orEmpty(),
                    unlocked = achievement.isUnlocked,
                    unlockTime = achievement.unlockTimestamp.toLong(),
                    hidden = achievement.hidden,
                    globalPercent = globalPercent[name],
                    progress = achievement.progressCurrent.takeIf { achievement.hasProgress },
                    progressMax = achievement.progressMax.takeIf { achievement.hasProgress },
                )
            }
            // Newest unlock first, then the locked ones from most to least common.
            .sortedWith(
                compareByDescending<Achievement> { it.unlocked }
                    .thenByDescending { it.unlockTime }
                    .thenByDescending { it.globalPercent ?: 0f },
            )

        return GameStats(
            achievements = achievements.toImmutableList(),
            stats = readStats(callback).toImmutableList(),
        )
    }

    /**
     * Achievement API name to the percentage of players who have unlocked it. Empty when
     * Steam does not answer.
     */
    private suspend fun globalAchievementPercentages(appId: Int): Map<String, Float> {
        val request = CPlayer_GetGameAchievements_Request.newBuilder()
            .setAppid(appId)
            .setLanguage(STEAM_LANGUAGE)
            .build()

        val body = player.getGameAchievements(request).bodyOrNull("GetGameAchievements")
            ?: return emptyMap()

        return body.achievementsList.mapNotNull { achievement ->
            achievement.playerPercentUnlocked.toFloatOrNull()?.let {
                achievement.internalName to it
            }
        }.toMap()
    }

    /**
     * Pairs the stat values with their names from the schema. Entries with `bits` are the
     * achievement blocks, and a stat without a value has simply never been set.
     */
    private fun readStats(callback: UserStatsCallback): List<GameStat> {
        val schemaStats = callback.schemaKeyValues["stats"]
        if (schemaStats == KeyValue.INVALID) {
            return emptyList()
        }

        val values = callback.stats.associate { it.statId to it.statValue }

        return schemaStats.children.mapNotNull { entry ->
            val raw = entry.name?.toIntOrNull()?.let(values::get) ?: return@mapNotNull null
            if (entry["bits"] != KeyValue.INVALID) {
                return@mapNotNull null
            }

            val label = entry["display"]["name"].value?.takeIf { it.isNotBlank() }
                ?: entry["name"].value
                ?: return@mapNotNull null

            // Floats and average rates travel as the bit pattern of a float.
            val value = when (entry["type"].value?.uppercase()) {
                "2", "3", "FLOAT", "AVGRATE" -> "%,.2f".format(Float.fromBits(raw))
                else -> "%,d".format(raw)
            }

            GameStat(name = label, value = value)
        }.sortedBy { it.name.lowercase() }
    }

    private companion object {
        const val ACHIEVEMENT_ICON_URL =
            "https://cdn.steamstatic.com/steamcommunity/public/images/apps/"
    }
}
