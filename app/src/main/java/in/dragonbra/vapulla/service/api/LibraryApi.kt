package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetAchievementsProgress_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetOwnedGames_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.future.await
import timber.log.Timber

/**
 * Library features: player counts via the `SteamUserStats` handler, ownership and playtime
 * via the `Player` unified service, and free licenses via the `SteamApps` handler.
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
}
