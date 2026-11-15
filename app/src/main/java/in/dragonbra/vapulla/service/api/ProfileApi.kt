package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetFavoriteBadge_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameBadgeLevels_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetPlayerLinkDetails_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.types.SteamID
import timber.log.Timber

/** Profile extras from the `Player` unified service: Steam level, badges and profile names. */
class ProfileApi(private val player: Player) {

    /**
     * The local account's Steam level, plus its badge level for [appId] when given. Steam
     * only answers this for the signed-in account.
     */
    suspend fun badgeLevels(appId: Int = 0): BadgeLevels? {
        val request = CPlayer_GetGameBadgeLevels_Request.newBuilder()
            .apply { if (appId > 0) appid = appId }
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

    /**
     * The profile names of [steamIds], whoever they are, friends or not. Missing for any
     * Steam will not name, and empty when it does not answer.
     */
    suspend fun names(steamIds: List<Long>): Map<Long, String> {
        if (steamIds.isEmpty()) return emptyMap()

        val request = CPlayer_GetPlayerLinkDetails_Request.newBuilder()
            .addAllSteamids(steamIds)
            .build()

        val response = player.getPlayerLinkDetails(request).await()
        if (response.result != EResult.OK) {
            Timber.w("GetPlayerLinkDetails failed: ${response.result}")
            return emptyMap()
        }

        return response.body.accountsList.associate {
            it.publicData.steamid to it.publicData.personaName
        }
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
