package `in`.dragonbra.vapulla.db.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.GameID
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import java.util.Date
import java.util.EnumSet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A friend (or pending request) as mirrored from Steam, plus the profile extras the app
 * tracks for them. The `is*` helpers drive the friends list grouping; how a friend's status
 * is worded, coloured and marked is in `ui/screens/home/components/FriendStatus.kt`.
 */
@Immutable
@Entity(tableName = "steam_friend")
data class SteamFriend(
    @PrimaryKey val id: Long,
    val name: String = "",
    val avatar: String? = Utils.Constants.ALL_ZEROS,
    val relation: EFriendRelationship = EFriendRelationship.None,
    val state: EPersonaState = EPersonaState.Offline,
    val gameAppID: Int = 0,
    val gameID: GameID = GameID(0),
    val gameName: String = "",
    /** Rich presence key/values from the game (e.g. `status`, `steam_display`). */
    val richPresence: Map<String, String> = emptyMap(),
    val lastLogOn: Date = Date(0),
    val lastLogOff: Date = Date(0),
    val stateFlags: EnumSet<EPersonaStateFlag> = emptyEnumSet(),
    val statusFlags: EnumSet<EClientPersonaStateFlag> = emptyEnumSet(),
    val nickname: String = "",
    /**
     * Epoch milliseconds of the newest stored chat message. Not kept up to date in the
     * database: the friends list fills it in memory from the stored messages.
     */
    val lastMessageTime: Long = -1,
    /** Unread chat messages. Filled in memory for the friends list, like [lastMessageTime]. */
    val newMessageCount: Int = 0,
    val aliases: ImmutableList<String> = persistentListOf(),
) {

    /** Every state from Online to LookingToPlay, so away and busy count; Invisible does not. */
    val isOnline: Boolean
        get() = state.code() in EPersonaState.Online.code()..EPersonaState.LookingToPlay.code()

    val isOffline: Boolean
        get() = state == EPersonaState.Offline

    val nameOrNickname: String
        get() = nickname.ifEmpty { name.ifEmpty { "<unknown>" } }

    val isPlayingGame: Boolean
        get() = isOnline && (gameName.isNotEmpty() || gameAppID != 0)

    /**
     * The game's own presence line (`status`), when it publishes a readable one. Some games
     * send unresolved localisation tokens such as `#Steam_Citadel_InGame`; those are skipped.
     */
    val richPresenceStatus: String
        get() = richPresence["status"].orEmpty().takeUnless { it.startsWith("#") }.orEmpty()

    val isAwayOrSnooze: Boolean
        get() = state == EPersonaState.Away ||
            state == EPersonaState.Snooze ||
            state == EPersonaState.Busy

    val isInGameAwayOrSnooze: Boolean
        get() = isPlayingGame && isAwayOrSnooze

    val isRequestRecipient: Boolean
        get() = relation == EFriendRelationship.RequestRecipient

    val isBlocked: Boolean
        get() = relation == EFriendRelationship.Blocked ||
            relation == EFriendRelationship.Ignored ||
            relation == EFriendRelationship.IgnoredFriend

    /** Still a friend on Steam, but blocked by us. Listed in its own section. */
    val isBlockedFriend: Boolean
        get() = relation == EFriendRelationship.IgnoredFriend

    val isFriend: Boolean
        get() = relation == EFriendRelationship.Friend
}
