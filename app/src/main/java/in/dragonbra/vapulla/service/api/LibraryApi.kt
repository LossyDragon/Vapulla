package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetAchievementsProgress_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameAchievements_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.callback.UserStatsCallback
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

/**
 * Library features: player counts, achievements and stats via the `SteamUserStats` handler,
 * ownership and playtime via the `Player` unified service, and free licenses via the
 * `SteamApps` handler.
 */
class LibraryApi(
    private val steamApps: SteamApps,
    private val userStats: SteamUserStats,
    private val player: Player,
    private val localSteamId: () -> SteamID?,
) {
    /** Players in-game right now, or null if Steam does not report it for this app. */
    suspend fun currentPlayers(appId: Int): Int? {
        val callback = userStats.getNumberOfCurrentPlayers(appId).await()
        return callback.numPlayers.takeIf { callback.result == EResult.OK }
    }

    /** Every game the account can play, including family-shared ones, with playtime. */
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

        val response = player.getOwnedGames(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetOwnedGames failed: ${response.result}")
            return emptyList()
        }

        return response.body.gamesList.map { game ->
            OwnedGame(
                appId = game.appid,
                name = game.name,
                playtimeForever = game.playtimeForever,
                playtimeTwoWeeks = game.playtime2Weeks,
                lastPlayed = game.rtimeLastPlayed.toLong(),
                familyShared = game.familyShared,
            )
        }
    }

    /** Achievement completion for the given apps, keyed by app ID. */
    suspend fun achievementsProgress(appIds: List<Int>): Map<Int, AchievementProgress> {
        val steamId = localSteamId() ?: return emptyMap()
        val request = CPlayer_GetAchievementsProgress_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .addAllAppids(appIds)
            .setIncludeUnvettedApps(true)
            .build()

        val response = player.getAchievementsProgress(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetAchievementsProgress failed: ${response.result}")
            return emptyMap()
        }

        return response.body.achievementProgressList.associate { progress ->
            progress.appid to AchievementProgress(
                appId = progress.appid,
                unlocked = progress.unlocked,
                total = progress.total,
                percentage = progress.percentage,
            )
        }
    }

    /** Adds a free-to-play or free-on-demand app to the account. */
    suspend fun requestFreeLicense(appId: Int): FreeLicenseResult {
        val callback = steamApps.requestFreeLicense(appId).await()
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
     * @return null if Steam refused, for one when the game has no stats at all.
     */
    suspend fun gameStats(appId: Int): GameStats? {
        val steamId = localSteamId() ?: return null

        val callback = userStats.getUserStats(appId, steamId).await()
        if (callback.result != EResult.OK) {
            Timber.w("GetUserStats for $appId answered ${callback.result}")
            return null
        }

        val globalPercent = runCatching { globalAchievementPercentages(appId) }
            .onFailure { Timber.w(it, "GetGameAchievements failed") }
            .getOrDefault(emptyMap())

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

    /** Achievement API name to the percentage of players who have unlocked it. */
    private suspend fun globalAchievementPercentages(appId: Int): Map<String, Float> {
        val request = CPlayer_GetGameAchievements_Request.newBuilder()
            .setAppid(appId)
            .setLanguage("english")
            .build()

        val response = player.getGameAchievements(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetGameAchievements failed: ${response.result}")
            return emptyMap()
        }

        return response.body.achievementsList.mapNotNull { achievement ->
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
