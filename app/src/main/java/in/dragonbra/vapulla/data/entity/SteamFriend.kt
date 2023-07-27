package `in`.dragonbra.vapulla.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "steam_friend")
data class SteamFriend(
    @PrimaryKey var id: Long,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "avatar") var avatar: String?,
    @ColumnInfo(name = "relation") var relation: Int,
    @ColumnInfo(name = "state") var state: Int?,
    @ColumnInfo(name = "game_app_id") var gameAppId: Int,
    @ColumnInfo(name = "game_name") var gameName: String?,
    @ColumnInfo(name = "last_log_on") var lastLogOn: Long,
    @ColumnInfo(name = "last_log_off") var lastLogOff: Long,
    @ColumnInfo(name = "state_flags") var stateFlags: Int,
    @ColumnInfo(name = "typing_timestamp") var typingTs: Long,
    @ColumnInfo(name = "nickname") var nickname: String?
) {
    @Ignore
    constructor(id: Long) : this(
        id = id,
        name = null,
        avatar = null,
        relation = 0,
        state = null,
        gameAppId = 0,
        gameName = null,
        lastLogOn = 0,
        lastLogOff = 0,
        stateFlags = 0,
        typingTs = 0L,
        nickname = null
    )
}
