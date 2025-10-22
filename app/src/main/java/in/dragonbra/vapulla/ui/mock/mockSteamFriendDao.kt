//package `in`.dragonbra.vapulla.ui.mock
//
//import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
//import `in`.dragonbra.javasteam.enums.EFriendRelationship
//import `in`.dragonbra.javasteam.enums.EPersonaState
//import `in`.dragonbra.javasteam.enums.EPersonaStateFlag
//import `in`.dragonbra.javasteam.types.GameID
//import `in`.dragonbra.vapulla.data.dao.SteamFriendDao
//import `in`.dragonbra.vapulla.data.entity.SteamFriend
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.MutableStateFlow
//import java.util.EnumSet
//import kotlin.time.Duration.Companion.seconds
//
//private val mockFriends = listOf(
//    // Friends in game
//    SteamFriend(
//        id = 76561198000000001L,
//        name = "Vapulla InGame 1",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Online,
//        gameAppID = 440,
//        gameID = GameID(440),
//        gameName = "Team Fortress 2",
//        stateFlags = EPersonaStateFlag.from(1), // HasRichPresence
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastMessage = "Hey, what's up",
//        nickname = "Gaming Buddy 1",
//        lastLogOn = System.currentTimeMillis(),
//    ),
//    SteamFriend(
//        id = 76561198000000002L,
//        name = "Vapulla InGame 2",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Busy,
//        gameAppID = 730,
//        gameID = GameID(730),
//        gameName = "Counter-Strike 2",
//        stateFlags = EPersonaStateFlag.from(3), // HasRichPresence + InJoinableGame
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastMessage = "Playing CS!",
//        newMessageCount = 16,
//        nickname = "Gaming Buddy 2",
//        lastLogOn = System.currentTimeMillis(),
//    ),
//    // Friends online
//    SteamFriend(
//        id = 76561198000000003L,
//        name = "Vapulla Online 1",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Online,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(0), // No flags
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastMessage = "Just chilling",
//        nickname = "Online Friend 1",
//        lastLogOn = System.currentTimeMillis(),
//    ),
//    SteamFriend(
//        id = 76561198000000004L,
//        name = "Vapulla Online 2",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Online,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(512), // ClientTypeMobile
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastMessage = "Hey there!",
//        newMessageCount = 3,
//        nickname = "Online Friend 2",
//        lastLogOn = System.currentTimeMillis(),
//    ),
//    // Friends away/snooze
//    SteamFriend(
//        id = 76561198000000005L,
//        name = "Vapulla Away",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Away,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(0),
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        nickname = "Away Friend",
//        lastLogOn = System.currentTimeMillis() - 1800000, // 30 min ago
//    ),
//    // Friends offline
//    SteamFriend(
//        id = 76561198000000006L,
//        name = "Vapulla Offline 1",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Offline,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(0),
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastLogOff = System.currentTimeMillis() - 3600000, // 1 hour ago
//        nickname = "Offline Friend 1",
//    ),
//    SteamFriend(
//        id = 76561198000000007L,
//        name = "Vapulla Offline 2",
//        relation = EFriendRelationship.Friend,
//        state = EPersonaState.Offline,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(0),
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastLogOff = System.currentTimeMillis() - 86400000, // 1 day ago
//        nickname = "Offline Friend 2",
//    ),
//    // Friend request
//    SteamFriend(
//        id = 76561198000000008L,
//        name = "Vapulla Request",
//        relation = EFriendRelationship.RequestRecipient,
//        state = EPersonaState.Online,
//        gameAppID = 0,
//        stateFlags = EPersonaStateFlag.from(0),
//        statusFlags = EnumSet.noneOf(EClientPersonaStateFlag::class.java),
//        lastLogOn = System.currentTimeMillis(),
//    )
//)
//
//val mockSteamFriendDao = object : SteamFriendDao {
//    override suspend fun insert(friend: SteamFriend) {}
//
//    override suspend fun insert(list: List<SteamFriend>) {}
//
//    override suspend fun find(id: Long): SteamFriend? = null
//
//    override suspend fun update(friend: SteamFriend) {}
//
//    override suspend fun update(list: List<SteamFriend>) {}
//
//    override suspend fun updateAll(friends: List<SteamFriend>) {}
//
//    override fun getFriendsFlow(): Flow<List<SteamFriend>> = MutableStateFlow(mockFriends)
//
//    override suspend fun clearNicknames() {}
//
//    override suspend fun findFriendsInGame(): List<SteamFriend> = mockFriends.filter { it.gameAppID > 0 }
//
//    override suspend fun remove(list: List<SteamFriend>) {}
//
//    override suspend fun remove(friend: SteamFriend) {}
//
//    override suspend fun delete() {}
//}