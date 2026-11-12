package `in`.dragonbra.vapulla.service.api

/**
 * The showcase APIs, one per JavaSteam surface, created by the service once the client is
 * connected and dropped when it disconnects. Screens get them through
 * [in.dragonbra.vapulla.service.SteamService.apis].
 */
class SteamApis(
    val library: LibraryApi,
    val profile: ProfileApi,
    val chatRooms: ChatRoomsApi,
    val servers: ServerBrowserApi,
    val cloud: CloudApi,
    val workshop: WorkshopApi,
    val sessions: SessionsApi,
)
