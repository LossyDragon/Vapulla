package `in`.dragonbra.vapulla.service.api

/** The language Steam is asked to write names and texts in; the app is English only. */
const val STEAM_LANGUAGE = "english"

/** Now, in the whole seconds since 1970 that Steam's messages count time in. */
internal fun steamTimeNow(): Int = (System.currentTimeMillis() / 1000).toInt()

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
    val favorites: FavoritesApi,
    val friendPreferences: FriendPreferencesApi,
    val chatRooms: ChatRoomsApi,
    val media: ChatMediaApi,
    val servers: ServerBrowserApi,
    val cloud: CloudApi,
    val workshop: WorkshopApi,
    val sessions: SessionsApi,
    val remoteClients: RemoteClientsApi,
    val screenshots: ScreenshotsApi,
    val manifests: ManifestApi,
    val invites: InvitesApi,
    val store: StoreApi,
    val web: WebSessionApi,
    val leaderboards: LeaderboardsApi,
    val family: FamilyApi,
    val authTickets: AuthTicketApi,
    val notifications: NotificationsApi,
)
