package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetFavoriteBadge_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameBadgeLevels_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.types.SteamID
import kotlinx.coroutines.future.await
import timber.log.Timber

/** Profile extras from the `Player` unified service: Steam level and badges. */
class ProfileApi(private val player: Player) {

    /**
     * The local account's Steam level, plus its badge level for [appId] when given. Steam
     * only answers this for the signed-in account.
     */
    suspend fun badgeLevels(appId: Int = 0): BadgeLevels? {
        val request = CPlayer_GetGameBadgeLevels_Request.newBuilder()
            .apply { if (appId > 0) setAppid(appId) }
            .build()

        val response = player.getGameBadgeLevels(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetGameBadgeLevels failed: ${response.result}")
            return null
        }

        return BadgeLevels(
            playerLevel = response.body.playerLevel,
            gameBadgeLevel = response.body.badgesList.maxOfOrNull { it.level } ?: 0,
        )
    }

    /** The badge a user shows off on their profile, if they picked one. */
    suspend fun favoriteBadge(steamId: SteamID): FavoriteBadge? {
        val request = CPlayer_GetFavoriteBadge_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .build()

        val response = player.getFavoriteBadge(request).await()
        if (response.result != EResult.OK || !response.body.hasFavoriteBadge) return null

        return FavoriteBadge(
            badgeId = response.body.badgeid,
            appId = response.body.appid,
            level = response.body.level,
        )
    }
}
