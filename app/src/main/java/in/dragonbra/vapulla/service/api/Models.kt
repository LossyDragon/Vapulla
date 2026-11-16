package `in`.dragonbra.vapulla.service.api

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.ECommunityPrivacy
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
data class FavoriteBadge(
    val badgeId: Int,
    /** The game the badge is from; 0 for one that belongs to no game. */
    val appId: Int,
    val level: Int,
    /** What the badge is called; empty when that is not known. */
    val name: String = "",
)

/** A badge crafted from a game's trading cards; a game has an ordinary and a foil one. */
@Immutable
data class GameBadge(val level: Int, val isFoil: Boolean)

/** How many of a badge's tasks are done. */
@Immutable
data class BadgeProgress(val done: Int, val total: Int)

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
    val reactions: ImmutableList<MessageReaction> = persistentListOf(),
    /** The message calls on the signed-in account: by name, or with `@all` or `@here`. */
    val mentionsMe: Boolean = false,
) {
    /** Unique within a channel. */
    val key: String get() = "$timestamp:$ordinal"
}

/**
 * One reaction on a chat message, as a chat screen shows it.
 *
 * @property reaction what Steam calls it: an emoticon's name between colons, e.g.
 *   ":steamhappy:", or a sticker's name as it is.
 * @property count how many people gave it.
 * @property mine the signed-in account is one of them.
 */
@Immutable
data class MessageReaction(
    val reaction: String,
    val isSticker: Boolean,
    val count: Int,
    val mine: Boolean,
)

/**
 * A reaction given to a message of a group chat channel, or taken back, as Steam pushes it.
 *
 * @property timestamp of the message, in epoch milliseconds; with [ordinal] it names it.
 * @property fromLocal it was the signed-in account's doing, here or on another device.
 */
data class GroupReactionChange(
    val groupId: Long,
    val chatId: Long,
    val timestamp: Long,
    val ordinal: Int,
    val reaction: String,
    val isSticker: Boolean,
    val isAdd: Boolean,
    val fromLocal: Boolean,
)

/**
 * A member of a group chat.
 *
 * @property accountId the low half of [steamId], which is what a mention names someone by.
 * @property rank "Owner", "Officer" or "Moderator"; empty for an ordinary member.
 */
@Immutable
data class ChatGroupMember(
    val steamId: Long,
    val accountId: Int,
    val name: String,
    val avatarUrl: String,
    val rank: String,
)

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

/**
 * A Steam client signed in to the account on another machine, from the `ClientComm` service.
 *
 * @property id what Steam calls the client instance; it names the client in every request.
 */
@Immutable
data class RemoteClient(
    val id: Long,
    val machineName: String,
    val osName: String,
    /** Free space on the machine for installing games, in bytes. */
    val freeBytes: Long,
    /** The apps running on it now. */
    val runningAppIds: ImmutableList<Int>,
)

/** One game as a [RemoteClient] has it: installed or not, and how a download of it stands. */
@Immutable
data class RemoteApp(
    val appId: Int,
    val name: String,
    val installed: Boolean,
    val running: Boolean,
    /** Being downloaded, updated or removed, or waiting in the queue for it. */
    val changing: Boolean,
    val paused: Boolean,
    val bytesDownloaded: Long,
    val bytesToDownload: Long,
    /** Bytes a second, 0 when nothing is arriving. */
    val downloadRate: Long,
    /** Seconds the client expects the download to take still; 0 when it does not say. */
    val secondsRemaining: Int,
    /** Place in the client's download queue, from 0; negative when it is not in it. */
    val queuePosition: Int,
    /** Bytes an install needs, as far as the client knows; 0 for an installed game. */
    val bytesRequired: Long,
)

/**
 * One item of a Steam profile's decoration, from the Points Shop. Addresses are complete;
 * a kind of file the item does not come in is empty.
 *
 * @property imageSmallUrl the item itself for an avatar frame or an animated avatar.
 * @property imageLargeUrl the still picture of a background.
 * @property movieMp4Url a background that moves, as H.264 video. Android decodes that in
 *   hardware on every device, so it is preferred over [movieWebmUrl], the same as VP9.
 */
@Immutable
data class ProfileItem(
    val name: String,
    val imageSmallUrl: String,
    val imageLargeUrl: String,
    val movieMp4Url: String,
    val movieWebmUrl: String,
) {
    /** The video of a moving background, or empty when the item is a still. */
    val movieUrl: String get() = movieMp4Url.ifEmpty { movieWebmUrl }
}

/** What a user has put on their profile; each is null when nothing of the kind is equipped. */
@Immutable
data class EquippedProfileItems(
    val background: ProfileItem? = null,
    val miniBackground: ProfileItem? = null,
    val avatarFrame: ProfileItem? = null,
    val animatedAvatar: ProfileItem? = null,
)

/**
 * Who may see each part of the account's profile, from `Player.GetPrivacySettings`. A part
 * Steam sent no setting for, or one this app does not know, is `Invalid`.
 *
 * @property profile the profile as a whole; the others only matter where this lets someone in.
 * @property ownedGames the games owned, with achievements and what is being played.
 * @property playtime the total time played, which can be hidden while the games are shown.
 */
@Immutable
data class PrivacySettings(
    val profile: ECommunityPrivacy,
    val ownedGames: ECommunityPrivacy,
    val playtime: ECommunityPrivacy,
    val friendsList: ECommunityPrivacy,
    val inventory: ECommunityPrivacy,
    val gifts: ECommunityPrivacy,
)
