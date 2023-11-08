@file:Suppress("BooleanMethodIsAlwaysInverted")

package `in`.dragonbra.vapulla.model

import androidx.room.ColumnInfo
import androidx.room.Ignore
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

    @Ignore
    constructor(
        name: String? = null,
        avatar: String? = null,
        relation: Int = 0,
        state: Int? = null,
        gameAppId: Int,
        gameName: String? = null,
        lastLogOn: Long = 0L,
        lastLogOff: Long = 0L,
        stateFlags: Int = 0,
        typingTs: Long = 0L,
        lastMessage: String? = null,
        lastMessageTime: Long? = null,
        newMessageCount: Int? = null,
        nickname: String? = null
    ) : this(
        0,
        name,
        avatar,
        relation,
        state,
        gameAppId,
        gameName,
        lastLogOn,
        lastLogOff,
        stateFlags,
        typingTs,
        lastMessage,
        lastMessageTime,
        newMessageCount,
        nickname
    )

    val friendName: String
        get() = if (!Strings.isNullOrEmpty(nickname)) nickname!! else name ?: "????"

    val isRequestRecipient: Boolean
        get() = relation == EFriendRelationship.RequestRecipient.code()

    val hasNickname: Boolean
        get() = !nickname.isNullOrEmpty()

    val isInGame: Boolean
        get() = if (isOnline) gameAppId > 0 || !Strings.isNullOrEmpty(gameName) else false

    val isOnline: Boolean
        get() = state?.let { it in 1..6 } ?: false

    val isOffline: Boolean
        get() = EPersonaState.from(state ?: 0) == EPersonaState.Offline

    val isInGameAwayOrSnooze: Boolean
        get() = isInGame && isAwayOrSnooze()

    val isUnread: Boolean
        get() = (newMessageCount ?: 0) > 0

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

    override fun toString(): String {
        return "FriendListItem(" +
            "id=$id, " +
            "name=$name, " +
            "avatar=$avatar, " +
            "relation=$relation, " +
            "state=$state, " +
            "gameAppId=$gameAppId, " +
            "gameName=$gameName, " +
            "lastLogOn=$lastLogOn, " +
            "lastLogOff=$lastLogOff, " +
            "stateFlags=$stateFlags, " +
            "typingTs=$typingTs, " +
            "lastMessage=$lastMessage, " +
            "lastMessageTime=$lastMessageTime, " +
            "newMessageCount=$newMessageCount, " +
            "nickname=$nickname)"
    }
}
