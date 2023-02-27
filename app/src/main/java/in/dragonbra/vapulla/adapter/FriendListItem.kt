package `in`.dragonbra.vapulla.adapter

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.util.Strings

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
    val friendName: String
        get() = if (!Strings.isNullOrEmpty(nickname)) nickname!! else name ?: "????"

    fun hasNickname() = !nickname.isNullOrEmpty()

    fun isRequestRecipient(): Boolean {
        if (relation == EFriendRelationship.RequestRecipient.code()) {
            return true
        }

        return false
    }

    fun isInGame(): Boolean {
        return if (isOnline()) gameAppId > 0 || !Strings.isNullOrEmpty(gameName) else false
    }

    fun isOnline() = state?.let { it in 1..6 } ?: false

    fun isOffline(): Boolean {
        val flags = EPersonaState.from(state ?: 0)
        if (flags == EPersonaState.Offline) {
            return true
        }

        return false
    }

    fun isInGameAwayOrSnooze(): Boolean {
        val isInGame = isInGame()
        val isAwayOrSnooze = isAwayOrSnooze()
        if (isInGame && isAwayOrSnooze) {
            return true
        }

        return false
    }

    fun isAwayOrSnooze(): Boolean {
        return when (EPersonaState.from(state ?: 0)) {
            EPersonaState.Away,
            EPersonaState.Snooze,
            EPersonaState.Busy -> true

            else -> false
        }
    }

    fun isItemRecentChat(recentsTimeout: Long, updateTime: Long): Boolean {
        val msgTime = lastMessageTime?.let { it >= updateTime - recentsTimeout }
        return recentsTimeout == 0L || (recentsTimeout > 0L && msgTime == true)
    }
}
