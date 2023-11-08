package `in`.dragonbra.vapulla.compose.screens.home

import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.model.FriendListItem

enum class EStatusGroup(val type: String) {
    REQUEST("request"),
    RECENT("recent"),
    IN_GAME("in_game"),
    ONLINE("online"),
    OFFLINE("offline"),
    BLOCKED("blocked")
}

data class CollapsableStatusGroup(
    val type: EStatusGroup,
    val title: String,
    val items: List<FriendListItem>,
    val collapsed: Boolean,
)

data class HomeState(
    val avatarHash: String = "",
    val filteredFriendsList: List<CollapsableStatusGroup> = listOf(),
    val friendsList: List<CollapsableStatusGroup> = listOf(),
    val isRefreshing: Boolean = false,
    val isSearching: Boolean = false,
    val nickname: String = "",
    val status: EPersonaState = EPersonaState.Offline,
    val updateTime: Long = 0L
)
