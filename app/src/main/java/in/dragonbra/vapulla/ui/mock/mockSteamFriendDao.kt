package `in`.dragonbra.vapulla.ui.mock

import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf

val mockFriendsList = persistentMapOf(
    R.string.headerFriendRecent to persistentListOf(
        SteamFriend(
            id = 1L,
            name = "Chat 1",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Online,
        ),
        SteamFriend(
            id = 2L,
            name = "Chat 2",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Away,
        ),
    ),
    R.string.headerFriendInGame to persistentListOf(
        SteamFriend(
            id = 3L,
            name = "In Game 1",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Online,
            gameAppID = 730,
            gameName = "Counter-Strike 2",
        ),
        SteamFriend(
            id = 4L,
            name = "In Game 2",
            nickname = "Game",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Away,
            gameAppID = 570,
            gameName = "Dota 2",
        ),
    ),
    R.string.headerFriendOnline to persistentListOf(
        SteamFriend(
            id = 5L,
            name = "Online 1",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Online,
        ),
        SteamFriend(
            id = 6L,
            name = "Online 2",
            nickname = "Two",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Away,
        ),
    ),
    R.string.headerFriendOffline to persistentListOf(
        SteamFriend(
            id = 7L,
            name = "Offline 1",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Offline,
        ),
        SteamFriend(
            id = 8L,
            name = "Offline 2",
            relation = EFriendRelationship.Friend,
            state = EPersonaState.Offline,
        ),
    ),
)

val mockStickyHeaders: ImmutableSet<Int> = persistentSetOf()
