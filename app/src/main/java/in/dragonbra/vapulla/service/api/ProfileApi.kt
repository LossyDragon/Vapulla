package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.ECommunityPrivacy
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetCommunityBadgeProgress_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetFavoriteBadge_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetGameBadgeLevels_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetPlayerLinkDetails_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetPrivacySettings_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.CPlayer_GetProfileItemsEquipped_Request
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.types.SteamID

/**
 * Profile extras from the `Player` unified service: Steam level, badges, profile names and
 * the account's privacy settings.
 */
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
     * The badges the signed-in account has crafted from [appId]'s trading cards, the
     * ordinary one before the foil one. Empty when it has none, or Steam does not answer.
     */
    suspend fun gameBadges(appId: Int): List<GameBadge> {
        val request = CPlayer_GetGameBadgeLevels_Request.newBuilder().setAppid(appId).build()

        val body = player.getGameBadgeLevels(request).bodyOrNull("GetGameBadgeLevels")
            ?: return emptyList()

        return body.badgesList
            .filter { it.level > 0 }
            .map { GameBadge(level = it.level, isFoil = it.borderColor != 0) }
            .sortedBy { it.isFoil }
    }

    /**
     * How far [steamId] is with the tasks of the Community Badge, the one Steam hands out
     * for trying its community features. Null when Steam does not answer or lists no tasks.
     */
    suspend fun communityBadgeProgress(steamId: SteamID): BadgeProgress? {
        val request = CPlayer_GetCommunityBadgeProgress_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setBadgeid(COMMUNITY_BADGE_ID)
            .build()

        val quests = player.getCommunityBadgeProgress(request)
            .bodyOrNull("GetCommunityBadgeProgress")
            ?.questsList
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return BadgeProgress(done = quests.count { it.completed }, total = quests.size)
    }

    /**
     * Who may see what of the signed-in account's profile. Steam answers this for that
     * account only. Null when it does not answer.
     */
    suspend fun privacySettings(): PrivacySettings? {
        val request = CPlayer_GetPrivacySettings_Request.newBuilder().build()

        val settings = player.getPrivacySettings(request).bodyOrNull("GetPrivacySettings")
            ?.privacySettings
            ?: return null

        fun privacy(state: Int) = ECommunityPrivacy.from(state) ?: ECommunityPrivacy.Invalid

        return PrivacySettings(
            profile = privacy(settings.privacyState),
            ownedGames = privacy(settings.privacyStateOwnedgames),
            playtime = privacy(settings.privacyStatePlaytime),
            friendsList = privacy(settings.privacyStateFriendslist),
            inventory = privacy(settings.privacyStateInventory),
            gifts = privacy(settings.privacyStateGifts),
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

        return FavoriteBadge(
            badgeId = body.badgeid,
            appId = body.appid,
            level = body.level,
            // Steam names no badge. The few that every account can have are named here; a
            // game's badge is named after the game by whoever shows it.
            name = if (body.appid == 0) COMMUNITY_BADGE_NAMES[body.badgeid].orEmpty() else "",
        )
    }

    /**
     * The decoration a user has equipped: avatar frame, animated avatar and backgrounds.
     *
     * @return null when Steam does not answer.
     */
    suspend fun equippedItems(steamId: SteamID): EquippedProfileItems? {
        val request = CPlayer_GetProfileItemsEquipped_Request.newBuilder()
            .setSteamid(steamId.convertToUInt64())
            .setLanguage(STEAM_LANGUAGE)
            .build()

        val body = player.getProfileItemsEquipped(request).bodyOrNull("GetProfileItemsEquipped")
            ?: return null

        return EquippedProfileItems(
            background = body.profileBackground.takeIf { body.hasProfileBackground() }?.toItem(),
            miniBackground = body.miniProfileBackground
                .takeIf { body.hasMiniProfileBackground() }?.toItem(),
            avatarFrame = body.avatarFrame.takeIf { body.hasAvatarFrame() }?.toItem(),
            animatedAvatar = body.animatedAvatar.takeIf { body.hasAnimatedAvatar() }?.toItem(),
        )
    }

    /** Steam names an item's files by their path under its community image address. */
    private fun SteammessagesPlayerSteamclient.ProfileItem.toItem(): ProfileItem? {
        fun url(path: String) = if (path.isEmpty()) "" else "$ITEM_FILES_URL$path"

        // An unset item comes as one without an ID.
        if (communityitemid == 0L) return null

        return ProfileItem(
            name = itemTitle.ifEmpty { name },
            imageSmallUrl = url(imageSmall),
            imageLargeUrl = url(imageLarge),
            // The small cut of a video is plenty for a phone.
            movieMp4Url = url(movieMp4Small.ifEmpty { movieMp4 }),
            movieWebmUrl = url(movieWebmSmall.ifEmpty { movieWebm }),
        )
    }

    private companion object {
        /** The Community Badge's number among the badges that belong to no game. */
        const val COMMUNITY_BADGE_ID = 2

        /** Badges that belong to no game, by their number, as far as they are known here. */
        val COMMUNITY_BADGE_NAMES = mapOf(
            1 to "Years of Service",
            COMMUNITY_BADGE_ID to "Community Badge",
            13 to "Game Collector",
        )

        const val ITEM_FILES_URL = "https://shared.akamai.steamstatic.com/community_assets/images/"
    }
}
