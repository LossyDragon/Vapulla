package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetFavoriteBadge_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameBadgeLevels_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetPlayerLinkDetails_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.types.SteamID

/** Profile extras from the `Player` unified service: Steam level, badges and profile names. */
class ProfileApi(private val player: Player) {

    /**
     * The signed-in account's Steam level, which `GetGameBadgeLevels` answers for that account
     * only. Null when Steam does not answer.
     */
    suspend fun steamLevel(): Int? {
        val request = CPlayer_GetGameBadgeLevels_Request.newBuilder().build()

        return player.getGameBadgeLevels(request).bodyOrNull("GetGameBadgeLevels")?.playerLevel
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

        val body = player.getPlayerLinkDetails(request).bodyOrNull("GetPlayerLinkDetails")
            ?: return emptyMap()

        return body.accountsList.associate { it.publicData.steamid to it.publicData.personaName }
    }

    /** The badge a user shows off on their profile; null if they picked none. */
    suspend fun favoriteBadge(steamId: SteamID): FavoriteBadge? {
        val request = CPlayer_GetFavoriteBadge_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .build()

        val body = player.getFavoriteBadge(request).bodyOrNull("GetFavoriteBadge")
            ?.takeIf { it.hasFavoriteBadge }
            ?: return null

        return FavoriteBadge(badgeId = body.badgeid, appId = body.appid, level = body.level)
    }
}
