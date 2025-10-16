package `in`.dragonbra.vapulla.adapter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.ui.icons.VR
import `in`.dragonbra.vapulla.ui.theme.friendAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendBlocked
import `in`.dragonbra.vapulla.ui.theme.friendInGame
import `in`.dragonbra.vapulla.ui.theme.friendInGameAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendOffline
import `in`.dragonbra.vapulla.ui.theme.friendOnline

data class FriendListItem(
    @PrimaryKey var id: Long,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "avatar") var avatar: String?,
    @ColumnInfo(name = "relation") var relation: Int,
    @ColumnInfo(name = "state") var state: Int?,
    @ColumnInfo(name = "game_app_id") var gameAppId: Int,
    @ColumnInfo(name = "playing_game_name") var gameName: String?,
    @ColumnInfo(name = "last_log_on") var lastLogOn: Long,
    @ColumnInfo(name = "last_log_off") var lastLogOff: Long,
    @ColumnInfo(name = "state_flags") var stateFlags: Int,
    @ColumnInfo(name = "typing_timestamp") var typingTs: Long,
    @ColumnInfo(name = "last_message") var lastMessage: String?,
    @ColumnInfo(name = "last_message_time") var lastMessageTime: Long?,
    @ColumnInfo(name = "new_message_count") var newMessageCount: Int?,
    @ColumnInfo(name = "nickname") var nickname: String?
) {

    val isOnline: Boolean
        get() = (state in 1..6)

    val isOffline: Boolean
        get() = state == EPersonaState.Offline.code()

    val nameOrNickname: String
        get() = nickname.orEmpty().ifEmpty { name.orEmpty().ifEmpty { "<unknown>" } }

    val isPlayingGame: Boolean
        get() = if (isOnline) gameAppId > 0 || gameName.orEmpty().isEmpty().not() else false

    val isPlayingGameName: String
        get() = if (isPlayingGame) {
            gameName.orEmpty().ifEmpty { "Playing game id: $gameAppId" }
        } else {
            if (isBlocked) {
                EFriendRelationship.from(relation).name
            } else {
                EPersonaState.from(state!!).name
            }
        }

    val isAwayOrSnooze: Boolean
        get() = state == EPersonaState.Away.code() ||
                state == EPersonaState.Snooze.code() ||
                state == EPersonaState.Busy.code()

    val isInGameAwayOrSnooze: Boolean
        get() = isPlayingGame && isAwayOrSnooze

    val isRequestRecipient: Boolean
        get() = relation == EFriendRelationship.RequestRecipient.code()

    val isBlocked: Boolean
        get() = relation == EFriendRelationship.Blocked.code() ||
                relation == EFriendRelationship.Ignored.code() ||
                relation == EFriendRelationship.IgnoredFriend.code()

    val isFriend: Boolean
        get() = relation == EFriendRelationship.Friend.code()

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
            EPersonaStateFlag.from(stateFlags)
                .contains(EPersonaStateFlag.ClientTypeVR) -> Icons.Default.VR

            EPersonaStateFlag.from(stateFlags)
                .contains(EPersonaStateFlag.ClientTypeTenfoot) -> Icons.Default.SportsEsports

            EPersonaStateFlag.from(stateFlags)
                .contains(EPersonaStateFlag.ClientTypeMobile) -> Icons.Default.Smartphone

            EPersonaStateFlag.from(stateFlags)
                .contains(EPersonaStateFlag.ClientTypeWeb) -> Icons.Default.Web

            else -> null
        }
}