package `in`.dragonbra.vapulla.service.api

/**
 * The feature APIs, one per JavaSteam surface, created by
 * [in.dragonbra.vapulla.service.SteamConnection] with each client and dropped when it is torn
 * down. Screens get them through [in.dragonbra.vapulla.service.SteamService.apis].
 */
class SteamApis(
    val library: LibraryApi,
    val profile: ProfileApi,
    val chat: ChatApi,
    val friends: FriendsApi,
    val chatRooms: ChatRoomsApi,
    val media: ChatMediaApi,
    val servers: ServerBrowserApi,
    val cloud: CloudApi,
    val workshop: WorkshopApi,
    val sessions: SessionsApi,
)
