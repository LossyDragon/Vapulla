package `in`.dragonbra.vapulla.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
import `in`.dragonbra.javasteam.types.GameID
import `in`.dragonbra.vapulla.ui.icons.VR
import `in`.dragonbra.vapulla.ui.theme.friendAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendBlocked
import `in`.dragonbra.vapulla.ui.theme.friendInGame
import `in`.dragonbra.vapulla.ui.theme.friendInGameAwayOrSnooze
import `in`.dragonbra.vapulla.ui.theme.friendOffline
import `in`.dragonbra.vapulla.ui.theme.friendOnline
import `in`.dragonbra.vapulla.util.Utils
import java.util.Date
import java.util.EnumSet

private typealias EPersonaStateFlags = EnumSet<EPersonaStateFlag>

@Entity(tableName = "steam_friend")
data class SteamFriend(
    @PrimaryKey var id: Long,
    var name: String = "",
    var avatar: String? = Utils.Constants.MISSING_AVATAR_URL,
    var relation: EFriendRelationship = EFriendRelationship.None,
    var state: EPersonaState = EPersonaState.Offline,
    var gameAppID: Int = 0,
    var gameID: GameID = GameID(0),
    var gameDataBlob: ByteArray = byteArrayOf(0),
    var gameName: String = "",
    var lastLogOn: Date = Date(0),
    var lastLogOff: Date = Date(0),
    var stateFlags: EPersonaStateFlags = EnumSet.noneOf(EPersonaStateFlag::class.java),
    var statusFlags: EnumSet<EClientPersonaStateFlag> = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
    var typingTs: Long = -1,
    var nickname: String = "",
    var lastMessage: String = "",
    var lastMessageTime: Long = -1,
    var newMessageCount: Int = 0,
) {

    val isOnline: Boolean
        get() = (state.code() in 1..6)

    val isOffline: Boolean
        get() = state == EPersonaState.Offline

    val nameOrNickname: String
        get() = nickname.ifEmpty { name.ifEmpty { "<unknown>" } }

    val isPlayingGame: Boolean
        get() = if (isOnline) gameName.isNotEmpty() || gameAppID != 0 else false

    val isPlayingGameName: String
        get() = if (isPlayingGame) {
            gameName.ifEmpty { "Playing game id: $gameAppID" }
        } else {
            if (isBlocked) {
                relation.name
            } else {
                if (state == EPersonaState.Offline) {
                    lastLogOff.toString()
                } else {
                    state.name
                }
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SteamFriend

        if (id != other.id) return false
        if (gameAppID != other.gameAppID) return false
        if (lastLogOn != other.lastLogOn) return false
        if (lastLogOff != other.lastLogOff) return false
        if (typingTs != other.typingTs) return false
        if (lastMessageTime != other.lastMessageTime) return false
        if (newMessageCount != other.newMessageCount) return false
        if (name != other.name) return false
        if (avatar != other.avatar) return false
        if (relation != other.relation) return false
        if (state != other.state) return false
        if (gameID != other.gameID) return false
        if (!gameDataBlob.contentEquals(other.gameDataBlob)) return false
        if (gameName != other.gameName) return false
        if (stateFlags != other.stateFlags) return false
        if (statusFlags != other.statusFlags) return false
        if (nickname != other.nickname) return false
        if (lastMessage != other.lastMessage) return false
        if (isOnline != other.isOnline) return false
        if (isOffline != other.isOffline) return false
        if (isPlayingGame != other.isPlayingGame) return false
        if (isAwayOrSnooze != other.isAwayOrSnooze) return false
        if (isInGameAwayOrSnooze != other.isInGameAwayOrSnooze) return false
        if (isRequestRecipient != other.isRequestRecipient) return false
        if (isBlocked != other.isBlocked) return false
        if (isFriend != other.isFriend) return false
        if (nameOrNickname != other.nameOrNickname) return false
        if (isPlayingGameName != other.isPlayingGameName) return false
        if (statusColor != other.statusColor) return false
        if (statusIcon != other.statusIcon) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + gameAppID
        result = 31 * result + lastLogOn.hashCode()
        result = 31 * result + lastLogOff.hashCode()
        result = 31 * result + typingTs.hashCode()
        result = 31 * result + lastMessageTime.hashCode()
        result = 31 * result + newMessageCount
        result = 31 * result + name.hashCode()
        result = 31 * result + (avatar?.hashCode() ?: 0)
        result = 31 * result + relation.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + gameID.hashCode()
        result = 31 * result + gameDataBlob.contentHashCode()
        result = 31 * result + gameName.hashCode()
        result = 31 * result + stateFlags.hashCode()
        result = 31 * result + statusFlags.hashCode()
        result = 31 * result + nickname.hashCode()
        result = 31 * result + lastMessage.hashCode()
        result = 31 * result + isOnline.hashCode()
        result = 31 * result + isOffline.hashCode()
        result = 31 * result + isPlayingGame.hashCode()
        result = 31 * result + isAwayOrSnooze.hashCode()
        result = 31 * result + isInGameAwayOrSnooze.hashCode()
        result = 31 * result + isRequestRecipient.hashCode()
        result = 31 * result + isBlocked.hashCode()
        result = 31 * result + isFriend.hashCode()
        result = 31 * result + nameOrNickname.hashCode()
        result = 31 * result + isPlayingGameName.hashCode()
        result = 31 * result + statusColor.hashCode()
        result = 31 * result + (statusIcon?.hashCode() ?: 0)
        return result
    }
}