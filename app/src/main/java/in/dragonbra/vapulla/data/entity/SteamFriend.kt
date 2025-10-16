package `in`.dragonbra.vapulla.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.vapulla.ui.icons.VR
import `in`.dragonbra.vapulla.ui.theme.friendAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendBlocked
import `in`.dragonbra.vapulla.ui.theme.friendInGame
import `in`.dragonbra.vapulla.ui.theme.friendInGameAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendOffline
import `in`.dragonbra.vapulla.ui.theme.friendOnline
import `in`.dragonbra.vapulla.util.Utils
import java.util.EnumSet

private typealias EPersonaStateFlags = EnumSet<EPersonaStateFlag>

@Entity(tableName = "steam_friend")
data class SteamFriend(
    @PrimaryKey var id: Long,
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "avatar")
    var avatar: String? = Utils.Constants.MISSING_AVATAR_URL,
    @ColumnInfo(name = "relation")
    var relation: EFriendRelationship = EFriendRelationship.None,
    @ColumnInfo(name = "state")
    var state: EPersonaState = EPersonaState.Offline,
    @ColumnInfo(name = "game_app_id")
    var gameAppId: Int = 0,
    @ColumnInfo(name = "game_name")
    var gameName: String = "",
    @ColumnInfo(name = "last_log_on")
    var lastLogOn: Long = 0,
    @ColumnInfo(name = "last_log_off")
    var lastLogOff: Long = 0,
    @ColumnInfo(name = "state_flags")
    var stateFlags: EPersonaStateFlags = EnumSet.noneOf(EPersonaStateFlag::class.java),
    @ColumnInfo(name = "typing_timestamp")
    var typingTs: Long = -1,
    @ColumnInfo(name = "nickname")
    var nickname: String = "",
    @ColumnInfo(name = "last_message")
    var lastMessage: String = "",
    @ColumnInfo(name = "last_message_time")
    var lastMessageTime: Long = -1,
    @ColumnInfo(name = "new_message_count")
    var newMessageCount: Int = 0,
) {

    val isOnline: Boolean
        get() = (state.code() in 1..6)

    val isOffline: Boolean
        get() = state == EPersonaState.Offline

    val nameOrNickname: String
        get() = nickname.ifEmpty { name.ifEmpty { "<unknown>" } }

    val isPlayingGame: Boolean
        get() = if (isOnline) gameAppId > 0 || gameName.isEmpty().not() else false

    val isPlayingGameName: String
        get() = if (isPlayingGame) {
            gameName.ifEmpty { "Playing game id: $gameAppId" }
        } else {
            if (isBlocked) {
                relation.name
            } else {
                state.name
            }
        }

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

    val isFriend: Boolean
        get() = relation == EFriendRelationship.Friend

    val statusColor: Color
        get() = when {
            isBlocked -> friendBlocked
            isOffline -> friendOffline
            isInGameAwayOrSnooze -> friendInGameAwayOrSnooze
            isAwayOrSnooze -> friendAwayOrSnooze
            isPlayingGame -> friendInGame
            isOnline -> friendOnline
            else -> friendOffline
        }

    val statusIcon: ImageVector?
        get() = when {
            isRequestRecipient -> Icons.Default.PersonAddAlt1
            isAwayOrSnooze -> Icons.Default.Bedtime
            stateFlags.contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR
            stateFlags.contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports
            stateFlags.contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone
            stateFlags.contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web
            else -> null
        }
}