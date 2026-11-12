package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EResult
import kotlinx.collections.immutable.ImmutableList

/** A game the account owns, from `Player.GetOwnedGames`. Playtimes are minutes. */
@Immutable
data class OwnedGame(
    val appId: Int,
    val name: String,
    val playtimeForever: Int,
    val playtimeTwoWeeks: Int,
    val lastPlayed: Long,
    val familyShared: Boolean,
)

/** Achievement completion for one app, from `Player.GetAchievementsProgress`. */
@Immutable
data class AchievementProgress(
    val appId: Int,
    val unlocked: Int,
    val total: Int,
    val percentage: Float,
)

/** Outcome of `SteamApps.RequestFreeLicense`. */
@Immutable
data class FreeLicenseResult(
    val result: EResult,
    val grantedApps: ImmutableList<Int>,
    val grantedPackages: ImmutableList<Int>,
)

/** The local account's Steam level and the badge for one app, from `Player.GetGameBadgeLevels`. */
@Immutable
data class BadgeLevels(val playerLevel: Int, val gameBadgeLevel: Int)

/** A friend's showcased badge, from `Player.GetFavoriteBadge`. */
@Immutable
data class FavoriteBadge(val badgeId: Int, val appId: Int, val level: Int)

/** A group chat the account belongs to, from `ChatRoom.GetMyChatRoomGroups`. */
@Immutable
data class ChatGroup(
    val groupId: Long,
    val name: String,
    val tagline: String,
    /** Full avatar URL when Steam provides one, otherwise empty. */
    val avatarUrl: String,
    val activeMembers: Int,
    val clanId: Int,
    val appId: Int,
    val rooms: ImmutableList<ChatRoomInfo>,
)

/** One text channel inside a [ChatGroup]. */
@Immutable
data class ChatRoomInfo(
    val chatId: Long,
    val name: String,
    val lastMessage: String,
    val timeLastMessage: Long,
)

/**
 * One line of a group chat channel, from `ChatRoom.GetMessageHistory` or pushed by
 * `ChatRoomClient.NotifyIncomingChatMessage`.
 */
@Immutable
data class GroupChatMessage(
    val groupId: Long,
    val chatId: Long,
    val senderId: Long,
    val senderName: String,
    /** Full avatar URL, or empty when Steam did not tell us the sender's avatar. */
    val senderAvatarUrl: String,
    /** BB Code text; for a server message, a sentence describing what happened. */
    val message: String,
    /** Epoch milliseconds. */
    val timestamp: Long,
    /** Tells apart messages sent within the same second. */
    val ordinal: Int,
    val fromLocal: Boolean,
    /** Someone joined, left and the like, rather than something a member wrote. */
    val isServerMessage: Boolean,
) {
    /** Unique within a channel. */
    val key: String get() = "$timestamp:$ordinal"
}

/** One page of channel history, newest first. */
@Immutable
data class GroupChatPage(val messages: ImmutableList<GroupChatMessage>, val moreAvailable: Boolean)

/** A game server from the master server list, enriched with its A2S_INFO reply when reachable. */
@Immutable
data class GameServer(
    val host: String,
    val port: Int,
    val authedPlayers: Int,
    val info: ServerInfo? = null,
) {
    val address: String get() = "$host:$port"
}

/** The public A2S_INFO fields of a Source engine server. */
@Immutable
data class ServerInfo(
    val name: String,
    val map: String,
    val game: String,
    val players: Int,
    val maxPlayers: Int,
    val bots: Int,
    val passwordProtected: Boolean,
    val vacSecured: Boolean,
)

/** A file in the account's Steam Cloud for one app, from `Cloud.EnumerateUserFiles`. */
@Immutable
data class CloudFile(
    val appId: Int,
    val filename: String,
    val size: Long,
    val timestamp: Long,
    val sha: String,
    val platforms: ImmutableList<String>,
)

/** A Workshop item, from `PublishedFile.QueryFiles`. */
@Immutable
data class WorkshopItem(
    val id: Long,
    val title: String,
    val description: String,
    val previewUrl: String,
    val creator: Long,
    val fileSize: Long,
    val timeUpdated: Long,
    val subscriptions: Int,
    val favorited: Int,
)

/** One page of Workshop results. [nextPage] is null on the last page. */
@Immutable
data class WorkshopPage(val items: ImmutableList<WorkshopItem>, val total: Int, val nextPage: Int?)

/** Sort order for Workshop queries; the values are Steam's `EPublishedFileQueryType`. */
enum class WorkshopSort(val queryType: Int, val label: String) {
    Trending(3, "Trending"),
    MostSubscribed(9, "Most subscribed"),
    MostRecent(1, "Most recent"),
    TopRated(0, "Top rated"),
    LastUpdated(21, "Last updated"),
}

/** A device signed in to the account, from `Authentication.EnumerateTokens`. */
@Immutable
data class AuthSession(
    val tokenId: Long,
    val description: String,
    val platform: String,
    val loggedIn: Boolean,
    val isThisDevice: Boolean,
    val firstSeen: Long,
    val lastSeen: Long,
    val lastSeenCity: String,
    val lastSeenCountry: String,
)

/** One achievement of a game with the account's own state, from `SteamUserStats.getUserStats`. */
@Immutable
data class Achievement(
    /** The API name, which is what Steam keys everything else by. */
    val name: String,
    val title: String,
    val description: String,
    /** Full URL of the coloured icon when unlocked, of the grey one when locked. */
    val iconUrl: String,
    val unlocked: Boolean,
    /** Epoch seconds, 0 while locked. */
    val unlockTime: Long,
    val hidden: Boolean,
    /** Share of all players who have it, 0 to 100; null when Steam did not report it. */
    val globalPercent: Float?,
    /** Progress towards a stat-linked achievement and its target; null when it has none. */
    val progress: Float?,
    val progressMax: Float?,
)

/** One of the numbers a game tracks for the account, already formatted for display. */
@Immutable
data class GameStat(val name: String, val value: String)

/** The account's achievements and stats for one game. */
@Immutable
data class GameStats(
    val achievements: ImmutableList<Achievement>,
    val stats: ImmutableList<GameStat>,
)
