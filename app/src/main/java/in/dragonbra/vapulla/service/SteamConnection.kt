package `in`.dragonbra.vapulla.service

import android.content.Context
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EGamingDeviceType
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamauthticket.SteamAuthTicket
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamcontent.SteamContent
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaChangeCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.AccountInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.EmailAddrInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.PlayingSessionStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.VanityURLChangedCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.WalletInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.manager.SettingsManager
import `in`.dragonbra.vapulla.service.api.AuthTicketApi
import `in`.dragonbra.vapulla.service.api.ChatApi
import `in`.dragonbra.vapulla.service.api.ChatMediaApi
import `in`.dragonbra.vapulla.service.api.ChatRoomsApi
import `in`.dragonbra.vapulla.service.api.CloudApi
import `in`.dragonbra.vapulla.service.api.CollectionsApi
import `in`.dragonbra.vapulla.service.api.FamilyApi
import `in`.dragonbra.vapulla.service.api.FavoritesApi
import `in`.dragonbra.vapulla.service.api.FriendPreferencesApi
import `in`.dragonbra.vapulla.service.api.FriendsApi
import `in`.dragonbra.vapulla.service.api.InvitesApi
import `in`.dragonbra.vapulla.service.api.LeaderboardsApi
import `in`.dragonbra.vapulla.service.api.LibraryApi
import `in`.dragonbra.vapulla.service.api.ManifestApi
import `in`.dragonbra.vapulla.service.api.NotificationsApi
import `in`.dragonbra.vapulla.service.api.ProfileApi
import `in`.dragonbra.vapulla.service.api.RemoteClientsApi
import `in`.dragonbra.vapulla.service.api.ScreenshotsApi
import `in`.dragonbra.vapulla.service.api.ServerBrowserApi
import `in`.dragonbra.vapulla.service.api.SessionsApi
import `in`.dragonbra.vapulla.service.api.SteamApis
import `in`.dragonbra.vapulla.service.api.SteamCollections
import `in`.dragonbra.vapulla.service.api.SteamFavorites
import `in`.dragonbra.vapulla.service.api.SteamFriendPreferences
import `in`.dragonbra.vapulla.service.api.SteamWishlist
import `in`.dragonbra.vapulla.service.api.StoreApi
import `in`.dragonbra.vapulla.service.api.WebSessionApi
import `in`.dragonbra.vapulla.service.api.WishlistApi
import `in`.dragonbra.vapulla.service.api.WorkshopApi
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.service.download.RemoteRequests
import `in`.dragonbra.vapulla.service.handler.VapullaHandler
import `in`.dragonbra.vapulla.service.sync.AnonymousPicsFeed
import `in`.dragonbra.vapulla.service.sync.FriendsSync
import `in`.dragonbra.vapulla.service.sync.LicenseSync
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils
import java.io.Closeable
import java.io.File
import java.util.EnumSet
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/** What this app calls itself to Steam: the device in the account's list of sign-ins. */
internal const val DEVICE_NAME = "Vapulla ${BuildConfig.VERSION_NAME}"

/**
 * Owns the JavaSteam client: builds it, connects, logs on, reconnects after a dropped
 * connection, and tears it down. Each client gets its own set of [SteamApis] and
 * [SessionComponent]s, which do the feature work.
 *
 * JavaSteam never reconnects by itself, so that is done here in [onDisconnected].
 *
 * @param scope the service's scope; everything launched here ends with the service.
 * @param onStatus told the connection state as short text for the foreground notification.
 * @param onShutDown told when the connection has ended for good and the service should stop;
 *   its argument is true when a signed-in session was lost rather than signed out by the user.
 */
class SteamConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val db: VapullaDatabase,
    private val account: AccountManager,
    private val onStatus: (String) -> Unit,
    private val onShutDown: (sessionLost: Boolean) -> Unit,
    private val downloads: DownloadManager,
    private val settings: SettingsManager,
) {

    // The client and what hangs off it are replaced by the thread that opens or closes the
    // connection and read by the callback loop and by requests on others, hence volatile.

    /** The feature APIs; null from the end of one connection until the next is opened. */
    @Volatile
    var apis: SteamApis? = null
        private set

    /** Steam's authentication service, for obtaining a refresh token before logging on. */
    val authentication get() = steamClient?.authentication

    /** True from [open] until the connection it started has ended. */
    val isOpen: Boolean get() = steamClient != null

    @Volatile
    private var steamClient: SteamClient? = null

    @Volatile
    private var steamUser: SteamUser? = null
    private val subscriptions: MutableSet<Closeable> = mutableSetOf()

    private var components: List<SessionComponent> = emptyList()

    @Volatile
    private var friendsSync: FriendsSync? = null

    /** Reconnect attempts made since the session was last signed in. */
    @Volatile
    private var retryCount = 0

    /**
     * What Steam calls this session among the account's clients, from the sign-in answer;
     * 0 until signed in.
     */
    @Volatile
    private var clientInstanceId = 0L

    /** Where JavaSteam keeps the CM server list between runs. */
    private val serverListFile = File(context.filesDir, "servers.bin")

    /** True once signed in: a dropped connection is then re-established and signed in again. */
    @Volatile
    private var resumeOnReconnect = false

    /** True when the current sign-in attempt already told the UI why it failed. */
    @Volatile
    private var loginErrorReported = false

    /** True from the user asking to sign out until the next sign-in attempt. */
    @Volatile
    private var userSignedOut = false

    /**
     * True when the current client is for a session without an account. Such a session
     * syncs and stores nothing, and never touches the saved sign-in or the downloads.
     */
    @Volatile
    private var anonymous = false

    /** Completed by the first connection of the client that [open] made. */
    @Volatile
    private var connectedSignal: CompletableDeferred<Unit>? = null

    /** The loop that hands what Steam sends to the callbacks below. */
    private var callbackLoopJob: Job? = null
    private var reconnectJob: Job? = null

    /** Parent of the background jobs that only make sense while signed in. */
    private var sessionJob: Job? = null

    /**
     * Starts a sign-in attempt: replaces any current client with a new one and connects it.
     *
     * @param anonymous true for a session without an account, see [logOnAnonymous].
     */
    @Synchronized
    fun open(anonymous: Boolean = false) {
        loginErrorReported = false
        userSignedOut = false
        retryCount = 0
        this.anonymous = anonymous
        connectToSteam(callbacks = initializeSteamClient())
    }

    /** Suspends until the client opened by [open] has connected. */
    suspend fun awaitConnected() {
        connectedSignal?.await() ?: throw IllegalStateException("Connection signal not available")
    }

    /** Logs on with a refresh token; the answer arrives in [onLoggedOn]. */
    suspend fun logOn(accountName: String, refreshToken: String) {
        val loginDetails = LogOnDetails(
            username = accountName,
            shouldRememberPassword = true,
            accessToken = refreshToken,
            loginID = Utils.getUniqueId(account),
            machineName = DEVICE_NAME,
            chatMode = ChatMode.NEW_STEAM_CHAT,
            // What kind of device this is, as picked in Settings; see AppSettings.
            gamingDeviceType = EGamingDeviceType.from(
                settings.settings.first().reportedDeviceType,
            ),
        )

        steamUser?.logOn(loginDetails)
    }

    /**
     * Logs on without an account. Steam hands such a session what it shows anyone: PICS, the
     * depots of public apps, the server lists. The answer arrives in [onLoggedOn].
     */
    fun logOnAnonymous() {
        steamUser?.logOnAnonymous()
    }

    /** Drops the connection. A disconnect asked for is never retried: [onDisconnected] ends it. */
    fun disconnect() {
        steamClient?.disconnect()
    }

    /**
     * Every CM server the client knows about. Blocking: the list is fetched from Steam's
     * directory when the client has none yet.
     */
    fun cmServers(): List<CmServer> {
        val client = steamClient ?: return emptyList()
        val current = client.currentEndpoint

        return client.servers.getAllEndPoints().map { server ->
            CmServer(
                host = server.host,
                port = server.port,
                protocols = server.protocolTypes.joinToString { it.name },
                current = current != null &&
                    current.hostString == server.host &&
                    current.port == server.port,
            )
        }
    }

    /**
     * Signs off and ends the connection. With [clearCredentials] the saved sign-in is forgotten
     * too, so the login screen does not sign straight back in.
     */
    fun logOut(clearCredentials: Boolean = true) {
        Timber.i("logOut(clearCredentials=$clearCredentials)")
        resumeOnReconnect = false
        userSignedOut = true

        scope.launch {
            if (clearCredentials) {
                account.clearLogin()
                eraseAccountData()
            }

            val user = steamUser
            if (user != null && steamClient?.isConnected == true) {
                // Steam answers with LoggedOffCallback, then the disconnect ends the service.
                user.logOff()
            } else {
                shutDown()
            }
        }
    }

    /**
     * Forgets a refresh token that Steam will not take again, and with it the stored data of
     * its account. The account name stays saved.
     */
    private fun forgetDeadToken(result: EResult) = scope.launch {
        Timber.i("Forgetting the saved sign-in token after $result")
        account.setRefreshToken(null)
        eraseAccountData()
    }

    /**
     * Erases what is stored about the account: its friends, chats, emoticons, library and
     * licenses, its saved persona, and the friends' chat notifications and shortcuts. Run
     * when the account is signed out for good, and when another one signs in, so that one
     * account never sees what belongs to another.
     *
     * Downloads are kept: the games they stand for are still on the device.
     */
    private suspend fun eraseAccountData() {
        Timber.i("Erasing the stored account data")

        db.steamFriendDao().deleteAll()
        db.chatMessageDao().deleteAll()
        db.emoticonDao().deleteAll()
        db.steamAppDao().deleteAll()
        db.steamLicenseDao().deleteAll()
        account.clearAccount()
        NotificationHelper.forgetFriends(context)
    }

    /**
     * Drops the current client. Its callbacks are unsubscribed first, so no
     * [DisconnectedCallback] follows and the state it would reset is reset here.
     *
     * Synchronized, like [open], because it is reached from several threads: a new sign-in,
     * the service ending, and the callback loop when the connection ends by itself.
     */
    @Synchronized
    fun close() {
        resumeOnReconnect = false

        reconnectJob?.cancel()
        reconnectJob = null

        stopSession()

        subscriptions.forEach { it.close() }
        subscriptions.clear()

        callbackLoopJob?.cancel(CancellationException("Service cleanup"))
        callbackLoopJob = null

        components = emptyList()
        friendsSync = null
        steamUser = null
        apis = null
        steamClient?.disconnect()
        steamClient = null

        connectedSignal?.cancel(CancellationException("Service cleanup"))
        connectedSignal = null

        SteamState.isLoggedIn.value = false
        SteamState.isAnonymous.value = false
        SteamAccount.signedOut()
        SteamFavorites.set(emptyList())
        SteamFriendPreferences.set(emptyMap())
        SteamWishlist.set(null)
        SteamCollections.set(emptyList())
    }

    /**
     * Ends the connection for good: the client is dropped, nothing reconnects and the UI
     * returns to sign-in. May be called from one of the client's own callbacks or from the
     * reconnect job, both of which [close] cancels: nothing here suspends, so it still runs
     * to its end.
     *
     * @param sessionLost true when a signed-in session ended without the user asking for it.
     */
    private fun shutDown(sessionLost: Boolean = SteamState.isLoggedIn.value && !userSignedOut) {
        close()

        SteamDiagnostics.record(
            if (sessionLost) "Session lost, service stopping" else "Connection ended",
        )

        onShutDown(sessionLost)
    }

    /** Starts the jobs that run for as long as this sign-in lasts. */
    private fun startSession() {
        stopSession()

        val job = SupervisorJob(scope.coroutineContext[Job])
        sessionJob = job

        val session = CoroutineScope(scope.coroutineContext + job)
        components.forEach { it.start(session) }
        if (!anonymous) session.syncAccountLevel()
    }

    private fun stopSession() {
        // Downloads need a signed-in client; the queue waits for the next license list.
        downloads.detach()

        sessionJob?.cancel(CancellationException("Session ended"))
        sessionJob = null
    }

    /**
     * Replaces any current client with a new one, with its APIs, session components and
     * callback subscriptions.
     *
     * @return the new client's callback manager, for [connectToSteam] to run.
     */
    private fun initializeSteamClient(): CallbackManager {
        Timber.d("initializeSteamClient()")

        if (steamClient != null) {
            close()
        }

        connectedSignal = CompletableDeferred()

        val client = SteamClient(steamConfiguration())

        // The app's own handler goes in first: a session component is built on it.
        client.addHandler<VapullaHandler>()

        val callbacks = CallbackManager(client)
        val steamApis = buildApis(client, callbacks)

        steamClient = client
        steamUser = requireNotNull(client.getHandler())
        apis = steamApis
        components = buildComponents(client, steamApis)

        subscriptions.addAll(
            listOf(
                callbacks.subscribe(onDisconnected),
                callbacks.subscribe(onConnected),
                callbacks.subscribe(onLoggedOn),
                callbacks.subscribe(onLoggedOff),
                callbacks.subscribe(onEmoticonList),
                callbacks.subscribe(onLicenseList),
                callbacks.subscribe(onAccountInfo),
                callbacks.subscribe(onWalletInfo),
                callbacks.subscribe(onEmailInfo),
                callbacks.subscribe(onVanityUrlChanged),
                callbacks.subscribe(onPersonaChange),
                callbacks.subscribe(onPlayingSessionState),
            ),
        )
        components.forEach { subscriptions.addAll(it.subscribe(callbacks)) }

        // Only the handlers no screen uses are dropped.
        with(client) {
            removeHandler<SteamGameCoordinator>()
            removeHandler<SteamGameServer>()
            removeHandler<SteamScreenshots>()
            removeHandler<SteamWorkshop>()
        }

        // Feed the in-app packet log; see NetworkLog.
        client.debugNetworkListener = NetworkLog

        return callbacks
    }

    /**
     * The client settings: where the CM server list is kept, which cell to ask Steam's
     * directory for servers of, and what to ask about friends. Without the cell the
     * directory hands out servers from anywhere, and the first may be a continent away.
     */
    private fun steamConfiguration(): SteamConfiguration = SteamConfiguration.create {
        it.withServerListProvider(FileServerListProvider(serverListFile))
        it.withCellID(runBlocking { account.cellId() })
        // What JavaSteam asks about every friend when the friends list arrives at sign-in.
        // Every flag stays in: Steam sends the answers to all devices of the account, and
        // they take what is not in an answer for empty. Asked without Presence, which also
        // carries the avatar, every friend was left with the placeholder for an avatar.
        it.withDefaultPersonaStateFlags(
            EnumSet.of(
                EClientPersonaStateFlag.Status,
                EClientPersonaStateFlag.PlayerName,
                EClientPersonaStateFlag.QueryPort,
                EClientPersonaStateFlag.SourceID,
                EClientPersonaStateFlag.Presence,
                EClientPersonaStateFlag.LastSeen,
                EClientPersonaStateFlag.UserClanRank,
                EClientPersonaStateFlag.GameExtraInfo,
                EClientPersonaStateFlag.GameDataBlob,
                EClientPersonaStateFlag.ClanData,
                EClientPersonaStateFlag.Facebook,
                EClientPersonaStateFlag.RichPresence,
                EClientPersonaStateFlag.Broadcast,
                EClientPersonaStateFlag.Watching,
            ),
        )
    }

    /** Creates the feature APIs on [client]'s handlers and unified services. */
    private fun buildApis(client: SteamClient, callbacks: CallbackManager): SteamApis {
        val steamApps: SteamApps = requireNotNull(client.getHandler())
        val steamFriends: SteamFriends = requireNotNull(client.getHandler())
        val userStats: SteamUserStats = requireNotNull(client.getHandler())
        val unified: SteamUnifiedMessages = requireNotNull(client.getHandler())
        val player: Player = unified.createService()
        val localSteamId = { client.steamID }

        val webSession = WebSessionApi(client.authentication, account, localSteamId)
        val profile = ProfileApi(player)

        return SteamApis(
            library = LibraryApi(
                steamApps = steamApps,
                userStats = userStats,
                player = player,
                localSteamId = localSteamId,
            ),
            profile = profile,
            chat = ChatApi(
                context = context,
                scope = scope,
                db = db,
                friendMessages = unified.createService(),
                localSteamId = localSteamId,
            ),
            friends = FriendsApi(db = db, steamFriends = steamFriends, player = player),
            favorites = FavoritesApi(unified.createService()),
            friendPreferences = FriendPreferencesApi(player),
            chatRooms = ChatRoomsApi(
                chatRoom = unified.createService(),
                friends = steamFriends,
                callbacks = callbacks,
                scope = scope,
                localSteamId = localSteamId,
            ),
            media = ChatMediaApi(
                steamCloud = requireNotNull(client.getHandler<SteamCloud>()),
                webSession = webSession,
            ),
            servers = ServerBrowserApi(requireNotNull(client.getHandler<SteamMasterServer>())),
            cloud = CloudApi(unified.createService()),
            workshop = WorkshopApi(unified.createService()),
            sessions = SessionsApi(unified.createService(), localSteamId),
            remoteClients = RemoteClientsApi(unified.createService()) { clientInstanceId },
            screenshots = ScreenshotsApi(unified.createService(), localSteamId),
            manifests = ManifestApi(
                client = client,
                steamApps = steamApps,
                content = requireNotNull(client.getHandler<SteamContent>()),
                scope = scope,
            ),
            invites = InvitesApi(unified.createService(), localSteamId),
            store = StoreApi(unified.createService(), unified.createService()),
            web = webSession,
            leaderboards = LeaderboardsApi(userStats = userStats, profile = profile),
            family = FamilyApi(unified.createService(), profile, localSteamId),
            authTickets = AuthTicketApi(
                authTicket = requireNotNull(client.getHandler<SteamAuthTicket>()),
                scope = scope,
            ),
            notifications = NotificationsApi(
                requireNotNull(client.getHandler<SteamNotifications>()),
            ),
            wishlist = WishlistApi(unified.createService(), localSteamId),
            collections = CollectionsApi(unified.createService()),
        )
    }

    /** Creates the parts of the session that react to what Steam pushes. */
    private fun buildComponents(client: SteamClient, steamApis: SteamApis): List<SessionComponent> {
        val steamApps: SteamApps = requireNotNull(client.getHandler())

        // Without an account there is nothing to sync, and the sync classes must not hear
        // what Steam sends: an anonymous license list would replace the stored one.
        if (anonymous) {
            return listOf(AnonymousPicsFeed(steamApps))
        }

        val localSteamId = { client.steamID }

        // The two sync classes call each other: friends ask for the app info of an unknown
        // game, and are told its name once that info is stored.
        lateinit var friends: FriendsSync
        val licenses = LicenseSync(
            scope = scope,
            db = db,
            account = account,
            steamApps = steamApps,
            library = steamApis.library,
            family = steamApis.family,
            localSteamId = localSteamId,
            onAppsNamed = { names -> friends.nameGames(names) },
        )
        friends = FriendsSync(
            context = context,
            scope = scope,
            db = db,
            account = account,
            localSteamId = localSteamId,
            licenses = licenses,
        )
        friendsSync = friends

        val remoteRequests = RemoteRequests(
            scope = scope,
            handler = requireNotNull(client.getHandler<VapullaHandler>()),
            appDao = db.steamAppDao(),
            downloadDao = db.downloadDao(),
            licenseDao = db.steamLicenseDao(),
            downloads = downloads,
        )

        return listOf(
            friends,
            steamApis.chat,
            steamApis.chatRooms,
            steamApis.favorites,
            steamApis.friendPreferences,
            steamApis.notifications,
            steamApis.wishlist,
            steamApis.collections,
            licenses,
            remoteRequests,
        )
    }

    /** Starts connecting and runs the loop that hands Steam's messages to the callbacks. */
    private fun connectToSteam(callbacks: CallbackManager) {
        Timber.i("Connecting to steam...")

        steamClient?.connect()

        onStatus("Connecting")
        SteamDiagnostics.record("Connecting")

        callbackLoopJob = scope.launch {
            while (isActive) {
                // One callback failing must not end the loop, or nothing Steam sends after
                // it would be heard and the session would hang without a word.
                try {
                    callbacks.runWaitCallbackAsync()
                } catch (e: Exception) {
                    ensureActive()
                    Timber.e(e, "Error in a Steam callback")
                }
            }
        }
    }

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer {
        // Whatever happens next, the signed-in jobs have nothing to talk to.
        stopSession()
        SteamDiagnostics.signedOut()
        SteamAccount.signedOut()
        if (it.isUserInitiated) {
            SteamDiagnostics.record("Disconnected (by the app)")
        } else {
            val detail = SteamDiagnostics.disconnectDetail()
            Timber.w("Disconnected%s", detail)
            SteamDiagnostics.record("Disconnected$detail")
        }

        // Worth retrying when a signed-in session dropped, or when the very first connection
        // of a sign-in attempt has not come up yet. A drop in the middle of signing in is
        // Steam turning the attempt down, and retrying that would only repeat it.
        val stillConnecting = connectedSignal?.isCompleted == false
        val canRetry = !it.isUserInitiated &&
            (resumeOnReconnect || stillConnecting) &&
            retryCount < MAX_RETRIES

        if (canRetry) {
            retryCount++
            Timber.i("onDisconnected() - Lost connection, retry $retryCount of $MAX_RETRIES")

            onStatus("Reconnecting")
            SteamDiagnostics.record("Reconnecting, attempt $retryCount of $MAX_RETRIES")

            // The callback loop is still running, so only the socket has to come back.
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                // Each attempt waits a little longer than the one before.
                delay(duration = RETRY_DELAY_STEP * retryCount)
                steamClient?.connect()

                // Every attempt should end in a callback: signed in again, or disconnected
                // again. One that stays silent would otherwise leave the session frozen on
                // "Reconnecting" for good, so it ends here instead. Cancelled by either answer.
                delay(duration = RECONNECT_TIMEOUT)
                Timber.w("Reconnect attempt $retryCount got no answer, giving up")
                SteamDiagnostics.record("Reconnect attempt $retryCount got no answer, giving up")

                shutDown()
            }
        } else {
            Timber.i("onDisconnected() - Disconnected from steam")

            if (!it.isUserInitiated && !loginErrorReported) {
                scope.launch {
                    SteamState.loginResult.emit(LoginResult.Error("Disconnected from Steam"))
                }
            }

            shutDown()
        }
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        Timber.i("onConnected() - Connected to Steam!")

        onStatus("Connected")
        val endpoint = steamClient?.currentEndpoint
        SteamDiagnostics.record("Connected to ${endpoint?.hostString}:${endpoint?.port}")

        // First connection of a sign-in attempt: the waiting auth flow takes over from here.
        if (connectedSignal?.complete(Unit) == true) {
            return@Consumer
        }

        // Otherwise this is a reconnect, and the session has to be signed in again.
        if (resumeOnReconnect && anonymous) {
            Timber.i("Reconnected, signing in anonymously again")
            logOnAnonymous()
        } else if (resumeOnReconnect) {
            scope.launch {
                val username = account.username.first()
                val refreshToken = account.refreshToken.first()

                if (username.isNullOrBlank() || refreshToken.isNullOrBlank()) {
                    Timber.w("Reconnected without a saved sign-in, giving up")
                    resumeOnReconnect = false
                    steamClient?.disconnect()
                } else {
                    Timber.i("Reconnected, signing in again")
                    logOn(accountName = username, refreshToken = refreshToken)
                }
            }
        }
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        Timber.d("onLoggedOn() got result ${it.result}")

        // Steam answered, so a reconnect attempt is no longer waiting on it.
        reconnectJob?.cancel()
        reconnectJob = null

        // Steam disconnects after a refused logon; only an accepted one may be resumed.
        resumeOnReconnect = it.result == EResult.OK

        // Counted from here and not from the connection coming up: a connection that drops
        // again before it is signed in must still use up the retries.
        if (it.result == EResult.OK) {
            retryCount = 0
        }

        when (it.result) {
            EResult.OK if anonymous -> {
                // No account: nothing is stored or synced, and the app stays "signed out".
                SteamState.isAnonymous.value = true
                recordSignIn(it)
                startSession()
            }

            EResult.OK -> {
                // What is stored belongs to one account. Anything left by another, or by a
                // sign-out, is erased before this one's data arrives: on this thread, so that
                // nothing Steam sends next is handled before it is done.
                val steamId = it.clientSteamID?.convertToUInt64() ?: 0L
                runBlocking {
                    if (account.dataOwner() != steamId) {
                        eraseAccountData()
                        account.setDataOwner(steamId)
                    }
                }

                SteamState.isLoggedIn.value = true
                SteamState.signOutReason.value = null
                recordSignIn(it)

                // Messages that arrived while signed out are fetched by ChatApi.start(), run by
                // startSession() below. The older requestOfflineMessages path is not used: it
                // returns plain text that cannot be matched against the BB Code copy from the
                // FriendMessages service, so each message was stored twice. NotificationsApi
                // only reads how many there are.

                friendsSync?.resetPresence()

                apis?.friends?.setPersonaState(EPersonaState.Online)
                steamClient?.getHandler<VapullaHandler>()?.getEmoticonList()

                scope.launch {
                    SteamState.loginResult.emit(LoginResult.Success)

                    // A token saved as plain text by an older build is encrypted now that
                    // it has proved good.
                    account.encryptStoredRefreshToken()
                }

                startSession()
            }

            else -> {
                Timber.w("onLoggedOn() refused with ${it.result}")
                SteamDiagnostics.record("Sign-in refused: ${it.result}")

                // Tell the login screen which EResult Steam answered with. The saved token is
                // only forgotten when Steam says it is no good any more; the other refusals
                // (TryAnotherCM, ServiceUnavailable, ...) are Steam-side trouble, and the
                // token is still good for a later try.
                loginErrorReported = true
                scope.launch {
                    val error = LoginResult.Error("Sign-in failed: ${it.result.name}")
                    SteamState.loginResult.emit(error)
                }
                if (!anonymous && it.result in FRESH_SIGN_IN_RESULTS) {
                    forgetDeadToken(it.result)
                }
            }
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        Timber.d("onLoggedOff() result ${it.result}")
        SteamDiagnostics.record("Signed off: ${it.result}")
        resumeOnReconnect = false

        // Steam ended the session rather than the user signing out. Leave the
        // reason for the login screen, which is about to be shown.
        if (it.result != EResult.OK && !userSignedOut) {
            SteamState.signOutReason.value = "Signed out by Steam: ${it.result.name}"
        }

        // Steam ending the session for one of these reasons (access removed while signed in,
        // say) also ends the token, so do not offer it again at the next sign-in.
        if (!anonymous && it.result in FRESH_SIGN_IN_RESULTS) {
            forgetDeadToken(it.result)
        }

        steamClient?.disconnect()
    }

    /**
     * Steam sends the license list after every sign-in. The depot downloader needs it to tell
     * what the account may download, so this is the moment downloads can start or carry on.
     */
    private val onLicenseList: Consumer<LicenseListCallback> = Consumer {
        val client = steamClient
        // An anonymous session has a license list too, but it is not the account's.
        if (it.result == EResult.OK && client != null && !anonymous) {
            downloads.attach(client, it.licenseList)
        }
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        Timber.d("onEmoticonList()")

        scope.launch {
            db.emoticonDao().replaceAll(it.emoteList)
        }
    }

    // Steam sends the next four on its own after a sign-in, and again when one changes.

    private val onAccountInfo: Consumer<AccountInfoCallback> = Consumer { info ->
        SteamAccount.update {
            it.copy(country = info.country, authorizedComputers = info.countAuthedComputers)
        }
    }

    private val onWalletInfo: Consumer<WalletInfoCallback> = Consumer { wallet ->
        SteamAccount.update {
            it.copy(
                hasWallet = wallet.isHasWallet,
                walletCents = wallet.longBalance,
                walletPendingCents = wallet.longBalanceDelayed,
                walletCurrency = wallet.currency?.name.orEmpty(),
            )
        }
    }

    private val onEmailInfo: Consumer<EmailAddrInfoCallback> = Consumer { email ->
        SteamAccount.update {
            it.copy(email = email.emailAddress, emailValidated = email.isEmailValidated)
        }
    }

    private val onVanityUrlChanged: Consumer<VanityURLChangedCallback> = Consumer { change ->
        SteamAccount.update { it.copy(customUrl = change.vanityUrl) }
    }

    /**
     * Another device signed in to the account started or stopped a game. Steam lets an
     * account play on one device at a time and tells the others; this app only shows it.
     */
    private val onPlayingSessionState: Consumer<PlayingSessionStateCallback> = Consumer { state ->
        Timber.d("onPlayingSessionState() blocked=%b", state.isPlayingBlocked)

        SteamAccount.update {
            it.copy(
                playingElsewhere = state.isPlayingBlocked,
                playingElsewhereAppId = state.playingAppID,
            )
        }
    }

    /** Steam's answer to [in.dragonbra.vapulla.service.api.FriendsApi.setPersonaName]. */
    private val onPersonaChange: Consumer<PersonaChangeCallback> = Consumer { change ->
        Timber.d("onPersonaChange() result ${change.result}")

        if (change.result == EResult.OK) {
            scope.launch { account.setAccountName(change.name) }
        }
        SteamAccount.nameChanged(NameChange(change.result, change.name))
    }

    /**
     * Keeps the cell Steam placed this sign-in in, for [steamConfiguration]. When it is not
     * the one the stored server list was fetched for, the list is dropped, so the next
     * connection asks the directory for servers of this cell. The session under way stays
     * on the server it has.
     */
    private fun rememberCell(cellId: Int) {
        scope.launch {
            if (cellId == account.cellId()) return@launch

            Timber.i("Steam cell is $cellId; dropping the stored server list")
            account.setCellId(cellId)
            serverListFile.delete()
        }
    }

    /** Keeps what the logon answer says about the session, for the Under the Hood screens. */
    private fun recordSignIn(callback: LoggedOnCallback) {
        rememberCell(callback.cellID)
        clientInstanceId = callback.clientInstanceId
        val client = steamClient
        val now = System.currentTimeMillis()

        SteamDiagnostics.signedIn(
            ConnectionInfo(
                server = client?.currentEndpoint?.let { "${it.hostString}:${it.port}" }.orEmpty(),
                allowedProtocols = client?.configuration?.protocolTypes
                    ?.joinToString { it.name }.orEmpty(),
                steamId = callback.clientSteamID?.convertToUInt64() ?: 0L,
                sessionId = client?.sessionID,
                cellId = callback.cellID,
                publicIp = callback.publicIP?.hostAddress.orEmpty(),
                ipCountry = callback.ipCountryCode.orEmpty(),
                heartbeatSeconds = callback.outOfGameSecsPerHeartbeat,
                serverTimeOffsetMs = callback.serverTime?.let { it.time - now },
                accountFlags = callback.accountFlags.orEmpty().map { it.name }.toImmutableList(),
                emailDomain = callback.emailDomain.orEmpty(),
                vanityUrl = callback.vanityURL.orEmpty(),
                familyGroupId = callback.familyGroupId,
                signedInAt = now,
            ),
        )
        SteamDiagnostics.record("Signed in")
        SteamAccount.update { it.copy(customUrl = callback.vanityURL.orEmpty()) }
    }

    /** Stores the account's Steam level for the drawer header. */
    private fun CoroutineScope.syncAccountLevel() = launch {
        apis?.profile?.steamLevel()?.let { account.setLevel(it) }
    }

    private companion object {
        /** Reconnect attempts after a dropped connection before giving up. */
        const val MAX_RETRIES = 3

        /** The wait before the first reconnect attempt; the second waits twice as long, etc. */
        val RETRY_DELAY_STEP = 2.seconds

        /** How long one reconnect attempt may take to connect and sign in again. */
        val RECONNECT_TIMEOUT = 60.seconds

        /**
         * Results that mean the saved refresh token will never sign in again, so it is
         * deleted and the user signs in afresh. Add to this as more are found.
         */
        val FRESH_SIGN_IN_RESULTS: Set<EResult> = setOf(
            // The session was removed, e.g. with Remove Access under Steam Guard.
            EResult.AccessDenied,
            EResult.Revoked,
            // What Steam answers for a token it does not recognise.
            EResult.InvalidPassword,
            // The token has run out.
            EResult.Expired,
        )
    }
}
