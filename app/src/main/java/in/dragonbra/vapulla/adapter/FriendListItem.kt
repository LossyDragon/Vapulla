package `in`.dragonbra.vapulla.adapter

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.util.Strings
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import java.util.*
import java.util.regex.Pattern

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

    fun isInGame() = gameAppId > 0 || !Strings.isNullOrEmpty(gameName)

    fun isOnline() = state?.let { it > 0 } ?: false

    fun isRequestRecipient() = relation == EFriendRelationship.RequestRecipient.code()

    fun isItemRecentChat(recentsTimeout: Long, updateTime: Long): Boolean =
            recentsTimeout == 0L || (recentsTimeout > 0L && lastMessageTime?.let {
                it >= updateTime - recentsTimeout
            } == true)

    // Retrieves the 1st alphanumeric letter in someones name. Then return it as uppercase.
    fun getFirstLetter(): String {
        val pattern = Pattern.compile("(\\b[a-zA-Z0-9])").matcher(name!!)
        return if (pattern.find()) {
            pattern.toMatchResult().group().toUpperCase(Locale.ROOT)
        } else {
            "?"
        }
    }
}
