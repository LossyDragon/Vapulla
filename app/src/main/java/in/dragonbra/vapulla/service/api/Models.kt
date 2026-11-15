package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.types.KeyValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/*
 * Times in these models are seconds since 1970 unless a property says otherwise; only chat
 * messages carry milliseconds, to match the stored friend chat.
 */

/**
 * Raw PICS info of one app or package.
 *
 * @property changeNumber the change of Steam's catalogue that last touched it.
 * @property token the access token the request was made with; 0 when Steam gave none.
 * @property missingToken Steam held details back because the account has no token for it.
 */
class PicsProduct(
    val id: Int,
    val isApp: Boolean,
    val changeNumber: Int,
    val token: Long,
    val missingToken: Boolean,
    val keyValues: KeyValue,
)

/**
 * A game the account owns, from `Player.GetOwnedGames`. Playtimes are minutes.
 *
 * @property lastPlayed seconds since 1970; 0 when never played.
 */
@Immutable
data class OwnedGame(
    val appId: Int,
    val name: String,
    val playtimeForever: Int,
    val playtimeTwoWeeks: Int,
    val lastPlayed: Long,
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
    val rooms: ImmutableList<ChatRoomInfo>,
)

/** One text channel inside a [ChatGroup]. [timeLastMessage] is seconds since 1970. */
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

/**
 * A file in the account's Steam Cloud for one app, from `Cloud.EnumerateUserFiles`.
 *
 * @property timestamp when it was last changed, in seconds since 1970.
 */
@Immutable
data class CloudFile(
    val appId: Int,
    val filename: String,
    val size: Long,
    val timestamp: Long,
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

/**
 * One page of a list Steam hands out in pages, such as Workshop items or screenshots.
 *
 * @property total how many items there are over all pages.
 * @property nextPage null on the last page.
 */
@Immutable
data class Page<T>(val items: ImmutableList<T>, val total: Int, val nextPage: Int?) {
    companion object {
        /** A page with nothing on it, for when Steam does not answer. */
        fun <T> empty(): Page<T> = Page(persistentListOf(), 0, null)

        /** Page number [page] (counted from 1) of [perPage] items each, out of [total]. */
        fun <T> of(items: List<T>, page: Int, perPage: Int, total: Int): Page<T> = Page(
            items = items.toImmutableList(),
            total = total,
            nextPage = (page + 1).takeIf { items.isNotEmpty() && page * perPage < total },
        )
    }
}

/** Sort order for Workshop queries; the values are Steam's `EPublishedFileQueryType`. */
enum class WorkshopSort(val queryType: Int, val label: String) {
    Trending(3, "Trending"),
    MostSubscribed(9, "Most subscribed"),
    MostRecent(1, "Most recent"),
    TopRated(0, "Top rated"),
    LastUpdated(21, "Last updated"),
}

/**
 * A device signed in to the account, from `Authentication.EnumerateTokens`. [firstSeen] and
 * [lastSeen] are seconds since 1970.
 */
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

/**
 * A friend invite link, from the `UserAccount` service.
 *
 * @property token what Steam knows the link by; the last part of [link].
 * @property uses how many people the link was made for.
 * @property durationSeconds how long the link still lasts; Steam counts it down.
 * @property createdAt when it was made, in seconds since 1970.
 */
@Immutable
data class FriendInvite(
    val token: String,
    val link: String,
    val uses: Int,
    val durationSeconds: Long,
    val createdAt: Long,
)

/**
 * One hit of a store search.
 *
 * @property price ready to show: an amount, "Free" or "Coming soon"; empty when not known.
 * @property originalPrice the price before a discount; empty when there is none.
 */
@Immutable
data class StoreSearchResult(
    val appId: Int,
    val name: String,
    val imageUrl: String,
    val price: String,
    val originalPrice: String = "",
    val discountPercent: Int = 0,
)

/**
 * A game's store page, from the `StoreBrowse` service.
 *
 * @property price what it costs now, as the store writes it; empty when it cannot be bought.
 * @property originalPrice the price before a discount; empty when there is none.
 * @property reviewLabel the store's verdict, e.g. "Very Positive"; empty without reviews.
 * @property releaseDate seconds since 1970; 0 when the store gives no date.
 * @property screenshots addresses of the page's screenshots, in the store's order.
 */
@Immutable
data class StorePage(
    val appId: Int,
    val name: String,
    val description: String,
    val headerUrl: String,
    val developers: String,
    val publishers: String,
    val isFree: Boolean,
    val price: String,
    val originalPrice: String,
    val discountPercent: Int,
    val reviewLabel: String,
    val reviewPercent: Int,
    val reviewCount: Int,
    val releaseDate: Long,
    val isComingSoon: Boolean,
    val isEarlyAccess: Boolean,
    val windows: Boolean,
    val mac: Boolean,
    val linux: Boolean,
    val tags: ImmutableList<String>,
    val screenshots: ImmutableList<String>,
    val storeUrl: String,
)

/**
 * One of a game's leaderboards.
 *
 * @property entries how many players have a score on it.
 * @property displayType Steam's `ELeaderboardDisplayType`: what a score on it means.
 */
@Immutable
data class Leaderboard(val id: Int, val name: String, val entries: Int, val displayType: Int)

/**
 * One player's place on a leaderboard.
 *
 * @property name the player's profile name; empty when Steam did not give one.
 * @property score ready to show: a count, or a time for a leaderboard of times.
 */
@Immutable
data class LeaderboardScore(val rank: Int, val steamId: Long, val name: String, val score: String)

/**
 * How the account's friends stand with one game, from `Player.GetFriendsGameplayInfo`. Each
 * list holds the Steam IDs of the friends it applies to.
 */
@Immutable
data class FriendsGameplay(
    val playingNow: ImmutableList<Long>,
    val playedRecently: ImmutableList<Long>,
    val playedBefore: ImmutableList<Long>,
    val own: ImmutableList<Long>,
    val wishlisted: ImmutableList<Long>,
)

/**
 * The account's Steam Family, from the `FamilyGroups` service.
 *
 * @property freeSpots how many more members it has room for.
 * @property members the account itself first, then the others by when they joined.
 */
@Immutable
data class Family(
    val id: Long,
    val name: String,
    val freeSpots: Int,
    val members: ImmutableList<FamilyMember>,
)

/**
 * One member of a Steam Family.
 *
 * @property name their profile name; empty when Steam did not give one.
 * @property role "Adult" or "Child", as the family has them; "Member" for any other role.
 * @property joinedAt seconds since 1970.
 * @property isSelf this member is the signed-in account.
 */
@Immutable
data class FamilyMember(
    val steamId: Long,
    val name: String,
    val role: String,
    val joinedAt: Long,
    val isSelf: Boolean,
)

/** A game a Steam Family shares with the account; [owners] are the members who own it. */
@Immutable
data class SharedGame(val appId: Int, val name: String, val owners: ImmutableList<Long>)

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

/** A screenshot the account uploaded to Steam, from `PublishedFile.GetUserFiles`. */
@Immutable
data class Screenshot(
    val id: Long,
    val appId: Int,
    /** The game it was taken in; empty when Steam did not name the app. */
    val appName: String,
    val caption: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val width: Int,
    val height: Int,
    /** Epoch seconds. */
    val timeCreated: Long,
)
