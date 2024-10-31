package `in`.dragonbra.vapulla.data.entity

import androidx.room.*

@Entity(tableName = "steam_friend")
data class SteamFriend(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "name") val name: String? = null,
    @ColumnInfo(name = "avatar") val avatar: String? = null,
    @ColumnInfo(name = "relation") val relation: Int = 0,
    @ColumnInfo(name = "state") val state: Int? = null,
    @ColumnInfo(name = "game_app_id") val gameAppId: Int = 0,
    @ColumnInfo(name = "game_name") val gameName: String? = null,
    @ColumnInfo(name = "last_log_on") val lastLogOn: Long = 0,
    @ColumnInfo(name = "last_log_off") val lastLogOff: Long = 0,
    @ColumnInfo(name = "state_flags") val stateFlags: Int = 0,
    @ColumnInfo(name = "typing_timestamp") val typingTs: Long = 0L,
    @ColumnInfo(name = "nickname") val nickname: String? = null
)
