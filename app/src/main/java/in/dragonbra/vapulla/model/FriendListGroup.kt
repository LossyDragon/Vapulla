package `in`.dragonbra.vapulla.model

data class FriendListGroup(
    val groupName: String,
    val groupCount: Int,
    val groupList: List<FriendListItem>,
    val isCollapsed: Boolean
)
