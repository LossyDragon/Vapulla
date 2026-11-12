package `in`.dragonbra.vapulla.service

import android.content.Context
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.ChatMode
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
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
import `in`.dragonbra.vapulla.service.api.ChatApi
import `in`.dragonbra.vapulla.service.api.ChatMediaApi
import `in`.dragonbra.vapulla.service.api.ChatRoomsApi
import `in`.dragonbra.vapulla.service.api.CloudApi
import `in`.dragonbra.vapulla.service.api.FriendsApi
import `in`.dragonbra.vapulla.service.api.LibraryApi
import `in`.dragonbra.vapulla.service.api.ProfileApi
import `in`.dragonbra.vapulla.service.api.ServerBrowserApi
import `in`.dragonbra.vapulla.service.api.SessionsApi
import `in`.dragonbra.vapulla.service.api.SteamApis
import `in`.dragonbra.vapulla.service.api.WorkshopApi
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.service.handler.VapullaHandler
import `in`.dragonbra.vapulla.service.sync.FriendsSync
import `in`.dragonbra.vapulla.service.sync.LicenseSync
import `in`.dragonbra.vapulla.util.Utils
import java.io.Closeable
import java.io.File
import java.util.EnumSet
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Owns the JavaSteam client: builds it, connects, logs on, reconnects after a dropped
 * connection, and tears it down. Each client gets its own set of [SteamApis] and
 * [SessionComponent]s, which do the feature work.
 *
 * JavaSteam never reconnects by itself, so that is done here in [onDisconnected].
 *
 * @param scope the service's scope; everything launched here ends with the service.
 * @param onStatus told the connection state as short text for the foreground notification.
 * @param onShutDown told when the connection has ended for good and the service should stop.
 */
class SteamConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val db: VapullaDatabase,
    private val account: AccountManager,
    private val onStatus: (String) -> Unit,
    private val onShutDown: () -> Unit,
) {

    /** The feature APIs; available while a client exists. */
    var apis: SteamApis? = null
        private set

    /** Steam's authentication service, for obtaining a refresh token before logging on. */
    val authentication get() = steamClient?.authentication

    private var steamClient: SteamClient? = null
    private var steamUser: SteamUser? = null
    private var callbackMgr: CallbackManager? = null
    private val subscriptions: MutableSet<Closeable> = mutableSetOf()

    private var components: List<SessionComponent> = emptyList()
    private var friendsSync: FriendsSync? = null

    private var retryCount = 0

    /** True once signed in: a dropped connection is then re-established and signed in again. */
    @Volatile
    private var resumeOnReconnect = false

    /** True when the current sign-in attempt already told the UI why it failed. */
    @Volatile
    private var loginErrorReported = false

    /** True from the user asking to sign out until the next sign-in attempt. */
    @Volatile
    private var userSignedOut = false

    private var connectedSignal: CompletableDeferred<Unit>? = null

    private var steamThreadJob: Job? = null
    private var reconnectJob: Job? = null

    /** Parent of the background jobs that only make sense while signed in. */
    private var sessionJob: Job? = null

    /** Starts a sign-in attempt: replaces any current client with a new one and connects it. */
    fun open() {
        loginErrorReported = false
        userSignedOut = false
        initializeSteamClient()
        connectToSteam()
    }

    /** Suspends until the client opened by [open] has connected. */
    suspend fun awaitConnected() {
        connectedSignal?.await() ?: throw IllegalStateException("Connection signal not available")
    }

    /** Logs on with a refresh token; the answer arrives in [onLoggedOn]. */
    fun logOn(accountName: String, refreshToken: String? = null) {
        scope.launch {
            val uuid = Utils.getUniqueId(account)

            val loginDetails = LogOnDetails(
                username = accountName,
                shouldRememberPassword = true,
                accessToken = refreshToken,
                loginID = uuid,
                machineName = "Vapulla ${BuildConfig.VERSION_NAME}",
                chatMode = ChatMode.NEW_STEAM_CHAT,
            )

            steamUser?.logOn(loginDetails)
        }
    }

    fun disconnect() {
        steamClient?.disconnect()
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
            }

            val user = steamUser
            if (user != null && steamClient?.isConnected == true) {
                // Steam answers with LoggedOffCallback, then the disconnect ends the service.
                user.logOff()
            } else {
                close()
                shutDown()
            }
        }
    }

    /**
     * Drops the current client. Its callbacks are unsubscribed first, so no
     * [DisconnectedCallback] follows and the state it would reset is reset here.
     */
    fun close() {
        resumeOnReconnect = false

        reconnectJob?.cancel()
        reconnectJob = null

        stopSession()

        subscriptions.forEach { it.close() }
        subscriptions.clear()

        steamThreadJob?.cancel(CancellationException("Service cleanup"))
        steamThreadJob = null

        components = emptyList()
        friendsSync = null
        steamUser = null
        apis = null
        steamClient?.disconnect()
        steamClient = null
        callbackMgr = null

        connectedSignal?.cancel(CancellationException("Service cleanup"))
        connectedSignal = null

        SteamState.isRunning.value = false
        SteamState.isLoggedIn.value = false
    }

    /** Ends the connection for good: nothing reconnects and the UI returns to sign-in. */
    private fun shutDown() {
        resumeOnReconnect = false

        reconnectJob?.cancel()
        reconnectJob = null

        stopSession()

        SteamState.isRunning.value = false
        SteamState.isLoggedIn.value = false

        onShutDown()
    }

    /** Starts the jobs that run for as long as this sign-in lasts. */
    private fun startSession() {
        stopSession()

        val job = SupervisorJob(scope.coroutineContext[Job])
        sessionJob = job

        val session = CoroutineScope(scope.coroutineContext + job)
        components.forEach { it.start(session) }
        session.syncAccountLevel()
    }

    private fun stopSession() {
        sessionJob?.cancel(CancellationException("Session ended"))
        sessionJob = null
    }

    private fun initializeSteamClient() {
        Timber.d("initializeSteamClient()")

        if (steamClient != null) {
            close()
        }

        connectedSignal = CompletableDeferred()

        val serverFile = File(context.filesDir, "servers.bin")
        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(serverFile))
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

        steamClient = SteamClient(config).also { client ->
            val callbacks = CallbackManager(client).also { callbackMgr = it }

            val steamApps: SteamApps = requireNotNull(client.getHandler())
            val steamFriends: SteamFriends = requireNotNull(client.getHandler())
            val unified: SteamUnifiedMessages = requireNotNull(client.getHandler())
            val player: Player = unified.createService()
            val localSteamId = { client.steamID }

            steamUser = requireNotNull(client.getHandler())

            val chat = ChatApi(
                context = context,
                scope = scope,
                db = db,
                friendMessages = unified.createService(),
                localSteamId = localSteamId,
            )

            val chatRooms = ChatRoomsApi(
                chatRoom = unified.createService(),
                friends = steamFriends,
                callbacks = callbacks,
                scope = scope,
                localSteamId = localSteamId,
            )

            val steamApis = SteamApis(
                library = LibraryApi(
                    steamApps = steamApps,
                    userStats = requireNotNull(client.getHandler<SteamUserStats>()),
                    player = player,
                    localSteamId = localSteamId,
                ),
                profile = ProfileApi(player),
                chat = chat,
                friends = FriendsApi(db = db, steamFriends = steamFriends, player = player),
                chatRooms = chatRooms,
                media = ChatMediaApi(
                    steamCloud = requireNotNull(client.getHandler<SteamCloud>()),
                    authentication = client.authentication,
                    account = account,
                    localSteamId = localSteamId,
                ),
                servers = ServerBrowserApi(requireNotNull(client.getHandler<SteamMasterServer>())),
                cloud = CloudApi(unified.createService()),
                workshop = WorkshopApi(unified.createService()),
                sessions = SessionsApi(unified.createService(), localSteamId),
            )
            apis = steamApis

            // The two sync classes call each other: friends ask for the app info of an
            // unknown game, and are told its name once that info is stored.
            lateinit var friends: FriendsSync
            val licenses = LicenseSync(
                scope = scope,
                db = db,
                account = account,
                steamApps = steamApps,
                library = steamApis.library,
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
            components = listOf(friends, chat, chatRooms, licenses)

            subscriptions.addAll(
                listOf(
                    callbacks.subscribe(onDisconnected),
                    callbacks.subscribe(onConnected),
                    callbacks.subscribe(onLoggedOn),
                    callbacks.subscribe(onLoggedOff),
                    callbacks.subscribe(onEmoticonList),
                ),
            )
            components.forEach { subscriptions.addAll(it.subscribe(callbacks)) }

            // Configure handlers. Only the ones no screen uses are dropped.
            with(client) {
                addHandler<VapullaHandler>()

                removeHandler<SteamGameCoordinator>()
                removeHandler<SteamGameServer>()
                removeHandler<SteamScreenshots>()
                removeHandler<SteamWorkshop>()
            }

            // Feed the in-app packet log; see NetworkLog.
            client.setDebugNetworkListener(NetworkLog)
        }
    }

    private fun connectToSteam() {
        if (!SteamState.isRunning.value) {
            retryCount = 0

            Timber.i("Connecting to steam...")

            SteamState.isRunning.value = true
            steamClient?.connect()

            onStatus("Connecting")

            steamThreadJob = scope.launch {
                try {
                    while (SteamState.isRunning.value && callbackMgr != null) {
                        callbackMgr?.runWaitCallbackAsync()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in callback loop")
                } finally {
                    Timber.i("Steam thread stopped")
                }
            }
        }
    }

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer {
        // Whatever happens next, the signed-in jobs have nothing to talk to.
        stopSession()

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

            // The callback loop is still running, so only the socket has to come back.
            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(duration = (retryCount * 2).seconds)
                steamClient?.connect()
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

        retryCount = 0

        onStatus("Connected")

        // First connection of a sign-in attempt: the waiting auth flow takes over from here.
        if (connectedSignal?.complete(Unit) == true) {
            return@Consumer
        }

        // Otherwise this is a reconnect, and the session has to be signed in again.
        if (resumeOnReconnect) {
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

        // Steam disconnects after a refused logon; only an accepted one may be resumed.
        resumeOnReconnect = it.result == EResult.OK

        when (it.result) {
            EResult.OK -> {
                SteamState.isLoggedIn.value = true
                SteamState.signOutReason.value = null

                // NOTE: the legacy offline-message path used to start here and is no longer
                // exercised anywhere in the app:
                //   SteamNotifications.requestOfflineMessageCount()
                //     -> OfflineMessageNotificationCallback
                //     -> SteamFriends.requestOfflineMessages()
                //     -> FriendMsgHistoryCallback
                // It was removed because it returns each message as plain text (emoticons as
                // ːnameː), which cannot be matched against the BB Code copy the FriendMessages
                // service delivers, so every message was stored twice. ChatApi.start(), run
                // by startSession() below, fetches what arrived while signed out.
                // If this is re-added to showcase those JavaSteam calls, request the count
                // here again, and log the history rather than storing it.

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

                // Tell the login screen which EResult Steam answered with. The saved token is
                // only forgotten when Steam says it is no good any more; the other refusals
                // (TryAnotherCM, ServiceUnavailable, ...) are Steam-side trouble, and the
                // token is still good for a later try.
                val tokenIsDead = it.result in FRESH_SIGN_IN_RESULTS

                loginErrorReported = true
                scope.launch {
                    val error = LoginResult.Error("Sign-in failed: ${it.result.name}")
                    SteamState.loginResult.emit(error)
                    if (tokenIsDead) {
                        Timber.i("Forgetting the saved sign-in token after ${it.result}")
                        account.setRefreshToken(null)
                    }
                }
            }
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        Timber.d("onLoggedOff() result ${it.result}")
        resumeOnReconnect = false

        // Steam ended the session rather than the user signing out. Leave the
        // reason for the login screen, which is about to be shown.
        if (it.result != EResult.OK && !userSignedOut) {
            SteamState.signOutReason.value = "Signed out by Steam: ${it.result.name}"
        }

        // Steam ending the session for one of these reasons (access removed while signed in,
        // say) also ends the token, so do not offer it again at the next sign-in.
        if (it.result in FRESH_SIGN_IN_RESULTS) {
            Timber.i("Forgetting the saved sign-in token after ${it.result}")
            scope.launch { account.setRefreshToken(null) }
        }

        steamClient?.disconnect()
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        Timber.d("onEmoticonList()")

        scope.launch {
            db.emoticonDao().replaceAll(it.emoteList)
        }
    }

    /** Stores the account's Steam level for the drawer header. */
    private fun CoroutineScope.syncAccountLevel() = launch {
        runCatching { requireNotNull(apis).profile.badgeLevels() }
            .onFailure { Timber.w(it, "Steam level sync failed") }
            .getOrNull()
            ?.let { account.setLevel(it.playerLevel) }
    }

    private companion object {
        /** Reconnect attempts after a dropped connection before giving up. */
        const val MAX_RETRIES = 5

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
        )
    }
}
