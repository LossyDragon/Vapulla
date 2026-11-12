package `in`.dragonbra.vapulla.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.RemoteInput
import androidx.room.withTransaction
import `in`.dragonbra.javasteam.enums.EClientPersonaStateFlag
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.LicenseListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSChangesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamapps.callback.PICSProductInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgEchoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgHistoryCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStateCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
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
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.ProfileItems
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.db.entity.ChatMessage
import `in`.dragonbra.vapulla.db.entity.SteamApp
import `in`.dragonbra.vapulla.db.entity.SteamFriend
import `in`.dragonbra.vapulla.db.entity.SteamLicense
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.api.ChatRoomsApi
import `in`.dragonbra.vapulla.service.api.CloudApi
import `in`.dragonbra.vapulla.service.api.LibraryApi
import `in`.dragonbra.vapulla.service.api.ProfileApi
import `in`.dragonbra.vapulla.service.api.ServerBrowserApi
import `in`.dragonbra.vapulla.service.api.SessionsApi
import `in`.dragonbra.vapulla.service.api.SteamApis
import `in`.dragonbra.vapulla.service.api.WorkshopApi
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.service.handler.VapullaHandler
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.generateSteamApp
import `in`.dragonbra.vapulla.util.helpers.emptyEnumSet
import `in`.dragonbra.vapulla.util.helpers.toLong
import `in`.dragonbra.vapulla.util.helpers.toSteamID
import `in`.dragonbra.vapulla.util.timeChunked
import java.io.Closeable
import java.io.File
import java.util.EnumSet
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Foreground service that owns the JavaSteam client for the whole app.
 *
 * It signs in (credentials or QR), keeps the connection alive, and mirrors what Steam sends
 * (friends, presence, chat history, licenses and PICS app info) into the Room database, which
 * the UI observes. Screens reach it through [ServiceConnection]; notification actions reach it
 * through [actionIntent].
 */
class SteamService : Service() {

    companion object {
        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"

        /** Log off and stop the service. */
        const val ACTION_STOP = "stop"

        /** Send [EXTRA_MESSAGE] to the friend in [EXTRA_ID]. */
        const val ACTION_REPLY = "reply"

        /** Accept the friend request from [EXTRA_ID]. */
        const val ACTION_ACCEPT_REQUEST = "accept_request"

        /** Decline the friend request from [EXTRA_ID]. */
        const val ACTION_IGNORE_REQUEST = "ignore_request"

        /** Decline the friend request from [EXTRA_ID] and block them. */
        const val ACTION_BLOCK_REQUEST = "block_request"

        /** Builds the start intent for one of the `ACTION_*` commands. */
        fun actionIntent(
            context: Context,
            action: String,
            friendId: Long? = null,
            message: CharSequence? = null,
        ): Intent = Intent(context, SteamService::class.java).apply {
            putExtra(EXTRA_ACTION, action)
            friendId?.let { putExtra(EXTRA_ID, it) }
            message?.let { putExtra(EXTRA_MESSAGE, it) }
        }

        /** Max ids per `WHERE id IN (...)` query, kept well under SQLite's bound-parameter limit. */
        private const val SQL_IN_CHUNK_SIZE = 500

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isLoggedIn = MutableStateFlow(false)
        val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

        private val _loginResult = MutableSharedFlow<LoginResult>()
        val loginResult: SharedFlow<LoginResult> = _loginResult.asSharedFlow()
    }

    // Coroutines
    private val scope = CoroutineScope(
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("SteamService"),
    )

    // Koin DI Injection
    private val db: VapullaDatabase by inject()
    private val account: AccountManager by inject()

    // Steam-related properties
    private var steamClient: SteamClient? = null
    private var steamApps: SteamApps? = null
    private var steamFriends: SteamFriends? = null
    private var steamUser: SteamUser? = null
    private var unifiedMessages: SteamUnifiedMessages? = null
    private var callbackMgr: CallbackManager? = null
    private val subscriptions: MutableSet<Closeable> = mutableSetOf()

    private var player: Player? = null

    /** Showcase APIs; available while connected. */
    var apis: SteamApis? = null
        private set

    // State management
    private lateinit var remoteInput: RemoteInput
    private var expectDisconnect: Boolean = false
    private var isWaitingForQRAuth: Boolean = false
    private var retryCount = 0
    private val requestsToNotify = mutableSetOf<SteamID>()

    private var connectedSignal: CompletableDeferred<Unit>? = null
    private val picsRequestChannel = Channel<Int>(Channel.UNLIMITED)

    private var steamThreadJob: Job? = null
    private var currentAuthJob: Job? = null

    private val friendsListMutex = Mutex()

    private var binder: Binder? = ServiceBinder()

    inner class ServiceBinder : Binder() {
        val service: SteamService = this@SteamService
    }

    override fun onBind(intent: Intent): IBinder? = binder

    override fun onCreate() {
        super.onCreate()

        Timber.i("onCreate")

        remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("onStartCommand")

        val notification = NotificationHelper.createServiceNotification(
            context = this,
            text = "Starting...",
        )

        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        intent?.getStringExtra(EXTRA_ACTION)?.let { action -> handleAction(action, intent) }

        return START_STICKY
    }

    /** Runs a notification action sent through [actionIntent]. */
    private fun handleAction(action: String, intent: Intent) {
        val friendId = intent.getLongExtra(EXTRA_ID, 0L)
        Timber.d("handleAction($action) for $friendId")

        val friends = steamFriends ?: run {
            Timber.w("Ignoring action '$action': not connected")
            return
        }

        when (action) {
            ACTION_STOP -> logOut()
            ACTION_ACCEPT_REQUEST -> friends.addFriend(friendId.toSteamID())
            ACTION_IGNORE_REQUEST -> friends.removeFriend(friendId.toSteamID())
            ACTION_BLOCK_REQUEST -> friends.ignoreFriend(friendId.toSteamID(), setIgnore = true)
            ACTION_REPLY -> Timber.w("Reply action is not implemented yet")
            else -> Timber.w("Unknown action '$action'")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")

        // Cancel any ongoing auth operations
        cancelCurrentAuthOperation()

        // Clean shutdown
        cleanupSteamClient()

        // Cancel coroutines
        scope.cancel(CancellationException("Service Destroyed"))
    }

    private fun cancelCurrentAuthOperation() {
        isWaitingForQRAuth = false

        currentAuthJob?.cancel(CancellationException("Auth operation cancelled"))
        currentAuthJob = null
    }

    private fun cleanupSteamClient() {
        expectDisconnect = true

        subscriptions.forEach { it.close() }
        subscriptions.clear()

        steamThreadJob?.cancel(CancellationException("Service cleanup"))
        steamThreadJob = null

        steamApps = null
        steamFriends = null
        steamUser = null
        apis = null
        steamClient?.disconnect()
        steamClient = null
        callbackMgr = null

        connectedSignal?.cancel(CancellationException("Service cleanup"))
        connectedSignal = null

        _isRunning.value = false
    }

    private fun initializeSteamClient() {
        Timber.d("initializeSteamClient()")

        if (steamClient != null) {
            cleanupSteamClient()
        }

        connectedSignal = CompletableDeferred()

        val serverFile = File(filesDir, "servers.bin")
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
            callbackMgr = CallbackManager(client)

            // Subscribe to callbacks
            callbackMgr?.let { mgr ->
                subscriptions.addAll(
                    listOf(
                        mgr.subscribe(onDisconnected),
                        mgr.subscribe(onConnected),
                        mgr.subscribe(onLoggedOn),
                        mgr.subscribe(onLoggedOff),
                        mgr.subscribe(onPersonaState),
                        mgr.subscribe(onFriendsList),
                        mgr.subscribe(onFriendMsgHistory),
                        mgr.subscribe(onFriendMsg),
                        mgr.subscribe(onNicknameList),
                        mgr.subscribe(onOfflineMessages),
                        mgr.subscribe(onEmoticonList),
                        mgr.subscribe(onFriendMsgEcho),
                        mgr.subscribe(onPicsChanges),
                        mgr.subscribe(onPicsProduct),
                        mgr.subscribe(onLicenseList),
                    ),
                )

                steamApps = requireNotNull(client.getHandler())
                steamUser = requireNotNull(client.getHandler())
                steamFriends = requireNotNull(client.getHandler())
                unifiedMessages = requireNotNull(client.getHandler())

                player = unifiedMessages!!.createService()
            }

            // Configure handlers. Only the ones no screen uses are dropped.
            with(client) {
                addHandler<VapullaHandler>()

                removeHandler<SteamCloud>()
                removeHandler<SteamGameCoordinator>()
                removeHandler<SteamGameServer>()
                removeHandler<SteamScreenshots>()
                removeHandler<SteamWorkshop>()
            }

            val unified = requireNotNull(unifiedMessages)
            val localSteamId = { client.steamID }
            apis = SteamApis(
                library = LibraryApi(
                    steamApps = requireNotNull(client.getHandler()),
                    userStats = requireNotNull(client.getHandler<SteamUserStats>()),
                    player = requireNotNull(player),
                    localSteamId = localSteamId,
                ),
                profile = ProfileApi(requireNotNull(player)),
                chatRooms = ChatRoomsApi(
                    chatRoom = unified.createService(),
                    friends = requireNotNull(steamFriends),
                    callbacks = requireNotNull(callbackMgr),
                ),
                servers = ServerBrowserApi(requireNotNull(client.getHandler<SteamMasterServer>())),
                cloud = CloudApi(unified.createService()),
                workshop = WorkshopApi(unified.createService()),
                sessions = SessionsApi(unified.createService(), localSteamId),
            )

            // Feed the in-app packet log; see NetworkLog.
            client.setDebugNetworkListener(NetworkLog)
        }
    }

    fun setPersonaState(state: EPersonaState) {
        steamFriends!!.setPersonaState(state = state)
    }

    fun logOut() {
        steamUser!!.logOff()
    }

    fun handleCredentialLogin(
        username: String? = null,
        password: String? = null,
        authenticator: IAuthenticator,
    ) {
        Timber.d("handleCredentialLogin() - Starting and Logging into Steam")

        // Cancel any ongoing QR auth
        cancelCurrentAuthOperation()

        currentAuthJob = scope.launch {
            try {
                initializeSteamClient()
                connectToSteam()

                connectedSignal?.await()
                    ?: throw IllegalStateException("Connection signal not available")

                _loginResult.emit(LoginResult.Loading)

                var username = username?.trim()
                var loginKey = account.refreshToken.first()

                if (loginKey.isNullOrBlank() || username.isNullOrBlank()) {
                    // Normal sign in
                    val authDetails = AuthSessionDetails().apply {
                        this.authenticator = authenticator
                        this.deviceFriendlyName = "Vapulla ${BuildConfig.VERSION_NAME}"
                        this.username = username
                        this.password = password?.trim()
                        this.persistentSession = true
                    }

                    val authSession = steamClient?.authentication
                        ?.beginAuthSessionViaCredentials(authDetails)
                        ?.await() ?: throw IllegalStateException("Steam client not available")

                    val pollResult = authSession.pollingWaitForResult().await()

                    if (pollResult.accountName.isBlank() || pollResult.refreshToken.isBlank()) {
                        val result = LoginResult.Error("Account Name or Refresh Token is blank")
                        _loginResult.emit(result)
                        return@launch
                    }

                    username = pollResult.accountName
                    loginKey = pollResult.refreshToken

                    account.setUsername(username)
                    account.setRefreshToken(loginKey)
                } else {
                    // Auto sign in
                    username = account.username.first()!!
                    loginKey = account.refreshToken.first()
                }

                loginToSteam(
                    accountName = username,
                    refreshToken = loginKey,
                )
            } catch (e: CancellationException) {
                Timber.i(e, "Credential login cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Error during credential login")
                _loginResult.emit(LoginResult.Error("Login failed: ${e.message}"))
            } finally {
                currentAuthJob = null
            }
        }
    }

    fun handleQRLogin() {
        Timber.i("Logging in via QR.")

        if (isWaitingForQRAuth) {
            Timber.w("QR login already in progress")
            return
        }

        // Cancel any existing auth operation
        cancelCurrentAuthOperation()

        isWaitingForQRAuth = true

        currentAuthJob = scope.launch {
            try {
                initializeSteamClient()
                connectToSteam()

                // Wait for connection with timeout handling
                connectedSignal?.await()
                    ?: throw IllegalStateException("Connection signal not available")

                // Check if cancelled while connecting
                if (!isWaitingForQRAuth) {
                    Timber.i("QR login cancelled while connecting")
                    return@launch
                }

                val authDetails = AuthSessionDetails().apply {
                    this.deviceFriendlyName = "Vapulla ${BuildConfig.VERSION_NAME}"
                }

                val authSession = steamClient?.authentication
                    ?.beginAuthSessionViaQR(authDetails)
                    ?.await() ?: throw IllegalStateException("Steam client not available")

                // Check if cancelled while creating session
                if (!isWaitingForQRAuth) {
                    Timber.i("QR login cancelled while creating auth session")
                    return@launch
                }

                authSession.challengeUrlChanged = IChallengeUrlChanged { qrAuthSession ->
                    if (isWaitingForQRAuth) {
                        scope.launch {
                            val qrCode = qrAuthSession?.challengeUrl.orEmpty()
                            _loginResult.emit(LoginResult.QRCode(qrCode))
                        }
                    }
                }

                // Send initial QR code
                _loginResult.emit(LoginResult.QRCode(authSession.challengeUrl))

                var authPollResult: AuthPollResult? = null

                // Poll for authentication result
                while (isWaitingForQRAuth && authPollResult == null) {
                    try {
                        authPollResult = authSession.pollAuthSessionStatus().await()
                        if (authPollResult == null && isWaitingForQRAuth) {
                            ensureActive()
                            delay(authSession.pollingInterval.toLong().milliseconds)
                        }
                    } catch (e: Exception) {
                        if (isWaitingForQRAuth) {
                            Timber.e(e, "Error polling auth session")
                            break
                        }
                    }
                }

                // Handle completion
                if (authPollResult != null) {
                    account.setUsername(authPollResult.accountName)
                    account.setRefreshToken(authPollResult.refreshToken)
                    loginToSteam(
                        accountName = authPollResult.accountName,
                        refreshToken = authPollResult.refreshToken,
                    )
                } else {
                    Timber.w("QR login completed without auth result")
                    steamClient?.disconnect()
                }
            } catch (e: CancellationException) {
                Timber.i(e, "QR login cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Error during QR login")
                _loginResult.emit(LoginResult.Error("QR login failed: ${e.message}"))
            } finally {
                isWaitingForQRAuth = false
                withContext(NonCancellable) {
                    _loginResult.emit(LoginResult.QRCodeEnded)
                }
                currentAuthJob = null
            }
        }
    }

    // region [Region] Profile Stuff
    suspend fun getProfileItems(friendId: Long) = withContext(Dispatchers.IO) {
        val request = SteammessagesPlayerSteamclient.CPlayer_GetProfileItemsEquipped_Request
            .newBuilder().apply {
                this.steamid = friendId
            }
            .build()

        val response = player!!.getProfileItemsEquipped(request).await()

        if (response.result != EResult.OK) {
            Timber.e("Failed to get profile items for $friendId, result: ${response.result}")
            return@withContext
        }

        val body = response.body
        var dbFriend = db.steamFriendDao().find(friendId)

        if (dbFriend == null) {
            Timber.i("No friend found in db to update profile items")
            return@withContext
        }

        dbFriend = dbFriend.copy(
            profileBackground = ProfileItems.from(body.profileBackground),
            profileMiniBackground = ProfileItems.from(body.miniProfileBackground),
            profileAvatarFrame = ProfileItems.from(body.avatarFrame),
            profileAnimatedAvatar = ProfileItems.from(body.animatedAvatar),
            profileProfileModifier = ProfileItems.from(body.profileModifier),
        )

        db.steamFriendDao().update(dbFriend)
    }

    suspend fun ignoreFriend(friendId: Long) = withContext(Dispatchers.IO) {
        val dbFriend = db.steamFriendDao().find(friendId) ?: return@withContext
        val friend = friendId.toSteamID()

        val result = if (dbFriend.relation == EFriendRelationship.Blocked) {
            steamFriends!!.ignoreFriend(friend, false).await()
        } else {
            steamFriends!!.ignoreFriend(friend, true).await()
        }

        if (result.result == EResult.OK) {
            val relation = if (dbFriend.relation == EFriendRelationship.IgnoredFriend) {
                EFriendRelationship.Blocked
            } else {
                EFriendRelationship.None // TODO this should update?
            }
            db.steamFriendDao().update(dbFriend.copy(relation = relation))
        }
    }

    suspend fun removeFriend(friendId: Long) = withContext(Dispatchers.IO) {
        val friend = friendId.toSteamID()
        steamFriends!!.removeFriend(friend)
        db.steamFriendDao().remove(friendId)
    }

    suspend fun setNickname(friendId: Long, value: String) = withContext(Dispatchers.IO) {
        val friend = friendId.toSteamID()
        steamFriends!!.setFriendNickname(friend, value)
    }

    suspend fun requestAliasHistory(friendId: Long) = withContext(Dispatchers.IO) {
        val friend = friendId.toSteamID()
        val aliasList = steamFriends!!.requestAliasHistory(friend).await()
            .responses
            .flatMap { response -> response.names }
            .map { name -> name.name }
            .toPersistentList()

        with(db.steamFriendDao()) {
            find(friendId)?.copy(aliases = aliasList)?.also {
                update(it)
            }
        }
    }

    suspend fun getProfileInfo(friendID: SteamID): ProfileInfoCallback =
        withContext(Dispatchers.IO) {
            steamFriends!!.requestProfileInfo(friendID).await()
        }

    // endregion

    fun handleQrCodeCancel() {
        Timber.i("Cancelling QR Login")
        cancelCurrentAuthOperation()
    }

    private fun loginToSteam(accountName: String, refreshToken: String? = null) {
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

            steamUser!!.logOn(loginDetails)
        }
    }

    private fun connectToSteam() {
        if (!isRunning.value) {
            expectDisconnect = false
            retryCount = 0

            Timber.i("Connecting to steam...")

            _isRunning.value = true
            steamClient?.connect()

            NotificationHelper.updateServiceNotification(
                context = this,
                text = "Connecting",
            )

            steamThreadJob = scope.launch {
                try {
                    while (isRunning.value && callbackMgr != null) {
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

    //region Callback handlers

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer {
        expectDisconnect = it.isUserInitiated

        if (expectDisconnect || retryCount >= 5) {
            Timber.i("onDisconnected() - Disconnected from steam")

            stopForeground(STOP_FOREGROUND_REMOVE)

            _isRunning.value = false
            _isLoggedIn.value = false

            stopSelf()
        } else {
            Timber.i("onDisconnected() - Failed to connect ${++retryCount} times, trying again...")

            NotificationHelper.updateServiceNotification(
                context = this,
                text = "Lost Connection",
            )

            scope.launch {
                delay(duration = 1.seconds)
                connectToSteam()
            }
        }
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        Timber.i("onConnected() - Connected to Steam!")

        retryCount = 0

        NotificationHelper.updateServiceNotification(
            context = this,
            text = "Connected",
        )

        connectedSignal?.complete(Unit)
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        Timber.d("onLoggedOn() got result ${it.result}")
        when (it.result) {
            EResult.OK -> {
                _isLoggedIn.value = true

                steamClient?.getHandler<SteamNotifications>()?.requestOfflineMessageCount()

                // Presence stored from the previous session is stale, and Steam only pushes
                // state for friends who are online now, so everyone must start out offline.
                // This queues on friendsListMutex well before the first persona push, which
                // follows the friends list and a network round trip.
                scope.launch {
                    friendsListMutex.withLock {
                        val dao = db.steamFriendDao()
                        val reset = dao.findWithPresence().map { friend ->
                            val offline = SteamFriend(id = friend.id)
                            friend.copy(
                                state = offline.state,
                                gameAppID = offline.gameAppID,
                                gameID = offline.gameID,
                                gameDataBlob = offline.gameDataBlob,
                                gameName = offline.gameName,
                                stateFlags = offline.stateFlags,
                            )
                        }
                        Timber.d("onLoggedOn: resetting stored presence of %d friends", reset.size)
                        dao.update(reset)
                    }
                }

                steamFriends!!.setPersonaState(EPersonaState.Online)
                steamClient?.getHandler<VapullaHandler>()?.getEmoticonList()

                scope.launch {
                    _loginResult.emit(LoginResult.Success)
                }

                continuousPicsChanges()
                continuousPicsChecker()
                continuousFriendChecker()
                syncPlaytime()
                syncAccountLevel()
            }

            EResult.InvalidPassword,
            EResult.AccessDenied,
            -> scope.launch {
                _loginResult.emit(LoginResult.Error(it.result.name))
                account.clearPreferences()
            }

            else -> Timber.w("onLoggedOn() got unknown result ${it.result}")
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        Timber.d("onLoggedOff()")
        steamClient?.disconnect()
    }

    private val onPersonaState: Consumer<PersonaStateCallback> = Consumer {
        // After the friends list arrives, JavaSteam sends ClientRequestFriendData for every
        // friend. Steam answers with profile-only messages in which persona_state and the game
        // fields are absent on the wire. Real presence for online friends arrives in separate
        // pushes, in no fixed order relative to those replies, so a reply must never overwrite
        // a stored state.
        val profileOnly = !it.hasPersonaState

        Timber.d(
            "onPersonaState() id=%d name='%s' state=%s game=%d profileOnly=%b",
            it.friendId.toLong(),
            it.playerName,
            it.personaState,
            it.gamePlayedAppId,
            profileOnly,
        )

        if (!it.friendId.isIndividualAccount) {
            return@Consumer
        }

        if (it.friendId == steamClient!!.steamID) {
            Timber.d("Updating local user info")
            scope.launch {
                account.saveLocalUser(it)
            }
            return@Consumer
        }

        scope.launch {
            friendsListMutex.withLock {
                val dao = db.steamFriendDao()
                val dbFriend = dao.find(it.friendId.toLong())

                if (dbFriend == null) {
                    Timber.d("Persona ${it.playerName} not in db to update persona state.")
                    return@launch
                }

                val withProfile = dbFriend.copy(
                    name = it.playerName,
                    avatar = it.avatarHash.toHexString(),
                    lastLogOn = it.lastLogon,
                    lastLogOff = it.lastLogoff,
                    statusFlags = it.statusFlags,
                )

                val updatedFriend = if (profileOnly) {
                    withProfile
                } else {
                    withProfile.copy(
                        state = it.personaState,
                        gameAppID = it.gamePlayedAppId,
                        gameID = it.gameId,
                        gameDataBlob = it.gameDataBlob,
                        richPresence = it.richPresence.associate { kv -> kv.key to kv.value },
                        gameName = it.gameName.ifEmpty {
                            if (it.gamePlayedAppId > 0) {
                                db.steamAppDao().findAppName(it.gamePlayedAppId) ?: ""
                            } else {
                                ""
                            }
                        },
                        stateFlags = it.personaStateFlags,
                    )
                }

                // Skip the write when nothing changed, otherwise every persona tick
                // invalidates the friends flow and re-runs the whole home pipeline.
                if (updatedFriend != dbFriend) {
                    if (updatedFriend.state != dbFriend.state) {
                        Timber.d(
                            "onPersonaState: %d '%s' %s -> %s",
                            dbFriend.id,
                            updatedFriend.name,
                            dbFriend.state,
                            updatedFriend.state,
                        )
                    }
                    dao.update(updatedFriend)
                }

                if (requestsToNotify.contains(it.friendId)) {
                    NotificationHelper.sendFriendRequestNotification(
                        context = applicationContext,
                        friendId = it.friendId.toLong(),
                        friendName = it.playerName,
                        avatarUrl = Utils.getAvatarURL(it.avatarHash.toHexString()),
                    )
                    requestsToNotify.remove(it.friendId)
                }
            }
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        Timber.d("onFriendsList()")
        val inc = it.isIncremental

        scope.launch {
            friendsListMutex.withLock {
                val dao = db.steamFriendDao()
                val friendsToAdd = mutableListOf<SteamFriend>()
                val friendsToUpdate = mutableListOf<SteamFriend>()
                val friendsToRemove = mutableListOf<SteamFriend>()

                it.friendList.forEach { friendItem ->
                    if (!friendItem.steamID.isIndividualAccount) {
                        return@forEach
                    }

                    var friend = dao.find(friendItem.steamID.toLong())

                    if (friend == null) {
                        if (friendItem.relationship == EFriendRelationship.Friend ||
                            friendItem.relationship == EFriendRelationship.RequestRecipient
                        ) {
                            val newFriend = SteamFriend(
                                id = friendItem.steamID.toLong(),
                                relation = friendItem.relationship,
                            )
                            friendsToAdd.add(newFriend)
                        }
                    } else {
                        if (friendItem.relationship == EFriendRelationship.Friend ||
                            friendItem.relationship == EFriendRelationship.RequestRecipient
                        ) {
                            val updatedFriend = friend.copy(relation = friendItem.relationship)
                            friendsToUpdate.add(updatedFriend)
                        } else {
                            friendsToRemove.add(friend)
                        }
                    }

                    if (inc && friendItem.relationship == EFriendRelationship.RequestRecipient) {
                        requestsToNotify.add(friendItem.steamID)
                    }
                }

                Timber.d(
                    "onFriendsList: incremental=%b received=%d add=%d update=%d remove=%d",
                    inc,
                    it.friendList.size,
                    friendsToAdd.size,
                    friendsToUpdate.size,
                    friendsToRemove.size,
                )
                dao.insert(friendsToAdd)
                dao.update(friendsToUpdate)
                dao.remove(friendsToRemove)
            }
        }
    }

    private val onFriendMsgHistory: Consumer<FriendMsgHistoryCallback> = Consumer {
        Timber.d("onFriendMsgHistory()")
        val dao = db.chatMessageDao()
        val friendId = it.steamID.toLong()

        it.messages.forEach { message ->
            val isFromLocal = it.steamID != message.steamID
            val timestamp = message.timestamp.time

            // Skip if we already have this confirmed message
            if (dao.find(message.message, timestamp, friendId, isFromLocal, true) != null) {
                return@forEach
            }

            // Try to find and update an unconfirmed message
            val unconfirmedMessage = dao.find(message.message, friendId, isFromLocal, false)
                .minByOrNull { abs(timestamp - it.timestamp) }

            if (unconfirmedMessage != null) {
                // Update existing unconfirmed message
                val updatedMessage = unconfirmedMessage.copy(
                    timestamp = timestamp,
                    timestampConfirmed = true,
                )
                dao.update(updatedMessage)
            } else {
                // Insert new confirmed message
                dao.insert(
                    ChatMessage(
                        message = message.message,
                        timestamp = timestamp,
                        friendId = friendId,
                        fromLocal = isFromLocal,
                        unread = message.unread,
                        timestampConfirmed = true,
                    ),
                )
            }
        }
    }

    private val onFriendMsg: Consumer<FriendMsgCallback> = Consumer {
        Timber.d("onFriendMsg()")
        // TODO ?
    }

    private val onFriendMsgEcho: Consumer<FriendMsgEchoCallback> = Consumer {
        Timber.d("onFriendMsgEcho()")
        // TODO ?
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        Timber.d("onNicknameList()")
        scope.launch {
            val dao = db.steamFriendDao()
            val nicknames = it.nicknames.associate { info ->
                info.steamID.toLong() to info.nickname
            }

            // Only clear rows losing their nickname; rows keeping one are set below.
            dao.clearNicknamesExcept(nicknames.keys.toList())

            val friendsToUpdate = nicknames.mapNotNull { (id, nickname) ->
                dao.find(id)
                    ?.takeIf { friend -> friend.nickname != nickname }
                    ?.copy(nickname = nickname)
            }

            dao.update(friendsToUpdate)
        }
    }

    private val onOfflineMessages: Consumer<OfflineMessageNotificationCallback> = Consumer {
        Timber.d("onOfflineMessages()")
        if (it.messageCount > 0) {
            steamFriends!!.requestOfflineMessages()
        }
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        Timber.d("onEmoticonList()")

        scope.launch {
            db.emoticonDao().replaceAll(it.emoteList)
        }
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer {
        scope.launch {
            if (account.lastChangeNumber.first() == it.currentChangeNumber) {
                Timber.w("Change number was the same as last change number, skipping")
                return@launch
            }

            Timber.d("onPicsChanges()")

            account.setLastChangeNumber(it.currentChangeNumber)

            // Only queue PICS requests for apps existing in the db that have changed.
            // Lookups are batched and select only the columns needed for the comparison.
            it.appChanges.values
                .chunked(SQL_IN_CHUNK_SIZE)
                .flatMap { chunk ->
                    val known = db.steamAppDao()
                        .findChangeInfo(chunk.map { change -> change.id })
                        .associateBy { info -> info.id }

                    chunk.filter { change ->
                        val app = known[change.id] ?: return@filter false
                        change.changeNumber != app.lastChangeNumber
                    }
                }
                .map { value -> value.id }
                .also { value -> Timber.d("onPicsChanges: Queueing ${value.size} app requests") }
                .forEach { value -> picsRequestChannel.send(value) }

            // Only queue PICS requests for pkgs existing in the db that have changed.
            val pkgsWithChanges = it.packageChanges.values
                .chunked(SQL_IN_CHUNK_SIZE)
                .flatMap { chunk ->
                    val known = db.steamLicenseDao()
                        .findLicenseInfo(chunk.map { change -> change.id })
                        .associateBy { info -> info.packageID }

                    chunk.filter { change ->
                        val pkg = known[change.id] ?: return@filter false
                        change.changeNumber != pkg.lastChangeNumber
                    }
                }

            if (pkgsWithChanges.isEmpty()) {
                return@launch
            }

            val pkgsForAccessTokens = pkgsWithChanges
                .filter { value -> value.isNeedsToken }
                .map { value -> value.id }

            val accessTokens = if (pkgsForAccessTokens.isNotEmpty()) {
                steamApps!!
                    .picsGetAccessTokens(appIds = emptyList(), packageIds = pkgsForAccessTokens)
                    .await()
                    .packageTokens
            } else {
                emptyMap()
            }

            val picsRequest = pkgsWithChanges.map { value ->
                PICSRequest(value.id, accessTokens[value.id] ?: 0)
            }

            Timber.d("onPicsChanges: Queueing ${picsRequest.size} package requests")
            steamApps!!.picsGetProductInfo(apps = emptyList(), packages = picsRequest)
        }
    }

    private val onPicsProduct: Consumer<PICSProductInfoCallback> = Consumer { callback ->
        Timber.d("onPicsProduct()")

        scope.launch {
            val appDao = db.steamAppDao()
            val licenseDao = db.steamLicenseDao()

            if (callback.packages.isNotEmpty()) {
                Timber.i("onPicsProduct: Received PICS of ${callback.packages.size} package(s)")

                val appIdsToRequest = mutableListOf<Int>()

                // One transaction for the whole batch: Room's invalidation tracker then notifies
                // observers (e.g. the paged games list) once, instead of once per statement.
                db.withTransaction {
                    callback.packages.values.forEach { pkg ->
                        val appIds = pkg.keyValues["appids"].children.map { it.asInteger() }
                        licenseDao.updateApps(pkg.id, appIds)

                        val depotIds = pkg.keyValues["depotids"].children.map { it.asInteger() }
                        licenseDao.updateDepots(pkg.id, depotIds)

                        // Insert a stub row for apps we don't know yet, then point every app
                        // in this package at it. This replaces a per-app SELECT * + full-row
                        // UPDATE that re-serialized every JSON column just to set packageId.
                        appDao.insertIfAbsent(appIds.map { SteamApp(id = it, packageId = pkg.id) })
                        appDao.updatePackageId(pkg.id, appIds)

                        appIdsToRequest += appIds
                    }
                }

                appIdsToRequest.forEach { picsRequestChannel.send(it) }
            }

            if (callback.apps.isNotEmpty()) {
                Timber.i("onPicsProduct: Received PICS of ${callback.apps.size} apps(s)")

                // Batch the lookups and select only the columns needed to decide what changed,
                // instead of a SELECT * (with JSON decoding of every column) per app.
                val changeInfoById = callback.apps.keys
                    .chunked(SQL_IN_CHUNK_SIZE)
                    .flatMap { ids -> appDao.findChangeInfo(ids) }
                    .associateBy { info -> info.id }

                val licenseByPackageId = changeInfoById.values
                    .map { info -> info.packageId }
                    .filter { packageId -> packageId != Int.MAX_VALUE }
                    .distinct()
                    .chunked(SQL_IN_CHUNK_SIZE)
                    .flatMap { ids -> licenseDao.findLicenseInfo(ids) }
                    .associateBy { license -> license.packageID }

                val apps = callback.apps.values.mapNotNull { app ->
                    val changeInfo = changeInfoById[app.id]
                    if (app.changeNumber == changeInfo?.lastChangeNumber) {
                        return@mapNotNull null
                    }

                    val packageId = changeInfo?.packageId ?: Int.MAX_VALUE
                    val license = licenseByPackageId[packageId]

                    app.keyValues.generateSteamApp().copy(
                        packageId = packageId,
                        ownerAccountId = license?.ownerAccountID ?: emptyList(),
                        receivedPICS = true,
                        lastChangeNumber = app.changeNumber,
                        licenseFlags = license?.licenseFlags ?: emptyEnumSet(),
                    )
                }

                if (apps.isNotEmpty()) {
                    Timber.d("Inserting ${apps.size} PICS apps to database")
                    appDao.insert(apps)
                }
            }
        }
    }

    private val onLicenseList: Consumer<LicenseListCallback> = Consumer {
        Timber.d("onLicenseList()")

        val result = it.result

        if (result != EResult.OK) {
            Timber.w("Failed to get license list: $result")
            return@Consumer
        }

        scope.launch {
            delay(5.seconds)

            val incomingLicenses = it.licenseList
            val existingLicenses = db.steamLicenseDao().getAllLicenses()
            val myAccountId = steamUser?.steamID?.accountID?.toInt()

            // Create lookup maps for efficient comparison
            val existingLicenseMap =
                existingLicenses.associateBy { license -> license.packageID }
            val incomingPackageIds =
                incomingLicenses.map { license -> license.packageID }.toSet()

            // Find licenses that need to be added or updated. A package can be licensed to
            // several accounts (family sharing); prefer our own copy for the details.
            val licensesToProcess = it.licenseList
                .groupBy { license -> license.packageID }
                .mapNotNull { entry ->
                    val preferredAccount = entry.value
                        .firstOrNull { value -> value.ownerAccountID == myAccountId }
                        ?: entry.value.first()

                    val existingLicense = existingLicenseMap[entry.key]

                    // Only process if:
                    // 1. License doesn't exist in DB, OR
                    // 2. License has been updated (different lastChangeNumber)
                    if (existingLicense == null ||
                        existingLicense.lastChangeNumber != preferredAccount.lastChangeNumber
                    ) {

                        SteamLicense(
                            packageID = entry.key,
                            lastChangeNumber = preferredAccount.lastChangeNumber,
                            timeCreated = preferredAccount.timeCreated,
                            timeNextProcess = preferredAccount.timeNextProcess,
                            minuteLimit = preferredAccount.minuteLimit,
                            minutesUsed = preferredAccount.minutesUsed,
                            paymentMethod = preferredAccount.paymentMethod,
                            licenseFlags = entry.value.fold(emptyEnumSet()) { flags, value ->
                                flags.apply { addAll(value.licenseFlags) }
                            },
                            purchaseCode = preferredAccount.purchaseCode,
                            licenseType = preferredAccount.licenseType,
                            territoryCode = preferredAccount.territoryCode,
                            accessToken = preferredAccount.accessToken,
                            ownerAccountID = entry.value.map { value -> value.ownerAccountID },
                            masterPackageID = preferredAccount.masterPackageID,
                            // Preserve existing app/depot data if updating
                            appIds = existingLicense?.appIds ?: emptyList(),
                            depotIds = existingLicense?.depotIds ?: emptyList(),
                        )
                    } else {
                        null // Skip unchanged licenses
                    }
                }

            if (licensesToProcess.isNotEmpty()) {
                Timber.i("Adding/updating ${licensesToProcess.size} licenses")
                db.steamLicenseDao().insert(licensesToProcess)
            }

            // Find licenses that were removed (exist in DB but not in incoming list)
            val licensesToRemove = existingLicenses.filter { license ->
                license.packageID !in incomingPackageIds
            }

            if (licensesToRemove.isNotEmpty()) {
                Timber.i("Removing ${licensesToRemove.size} stale licenses")

                // Clean up associated SteamApps
                licensesToRemove.forEach { license ->
                    // Remove apps that were only associated with this package
                    license.appIds.forEach { appId ->
                        val app = db.steamAppDao().findApp(appId)
                        if (app != null && app.packageId == license.packageID) {
                            // Reset to stub state or remove entirely
                            db.steamAppDao().update(
                                app.copy(
                                    packageId = Int.MAX_VALUE,
                                    ownerAccountId = emptyList(),
                                    licenseFlags = emptyEnumSet(),
                                ),
                            )
                        }
                    }
                }

                val packageIds = licensesToRemove.map { it.packageID }
                db.steamLicenseDao().deleteStaleLicenses(packageIds)
            }

            // Only request PICS for NEW or UPDATED licenses
            if (licensesToProcess.isNotEmpty()) {
                val picsRequests = licensesToProcess.map { license ->
                    PICSRequest(license.packageID, license.accessToken)
                }

                Timber.d("Requesting PICS for ${picsRequests.size} licenses")
                steamApps!!.picsGetProductInfo(apps = emptyList(), packages = picsRequests)
            }

            // Resume an interrupted ingest. Package info creates stub app rows and queues their
            // app info; if the process died before the replies arrived, nothing else would
            // ever ask again because the licenses themselves are unchanged.
            val awaitingPics = db.steamAppDao().findAppsAwaitingPics()
            if (awaitingPics.isNotEmpty()) {
                Timber.i("Resuming PICS for ${awaitingPics.size} apps still without app info")
                awaitingPics.forEach { picsRequestChannel.send(it) }
            }
        }
    }

    // endregion

    private fun continuousPicsChanges() = scope.launch {
        while (isActive && _isLoggedIn.value) {
            delay(1.minutes)

            val lastChangeNumber = account.lastChangeNumber.first() ?: 0

            Timber.d("picsGetChangesSince($lastChangeNumber)")

            steamApps!!.picsGetChangesSince(
                lastChangeNumber = lastChangeNumber,
                sendAppChangeList = true,
                sendPackageChangelist = true,
            )
        }
    }

    private fun continuousPicsChecker() = scope.launch {
        picsRequestChannel.receiveAsFlow()
            .timeChunked(500, 10.seconds)
            .collect { ids ->
                Timber.d("Collected ${ids.size} app(s) to query PICS")
                steamApps!!.picsGetProductInfo(
                    apps = ids.map { PICSRequest(id = it, accessToken = 0) },
                    packages = emptyList(),
                )
            }
    }

    /** Stores the account's Steam level for the drawer header. */
    private fun syncAccountLevel() = scope.launch {
        runCatching { requireNotNull(apis).profile.badgeLevels() }
            .onFailure { Timber.w(it, "Steam level sync failed") }
            .getOrNull()
            ?.let { account.setLevel(it.playerLevel) }
    }

    /** Copies playtime and last-played from `Player.GetOwnedGames` onto the stored apps. */
    private fun syncPlaytime() = scope.launch {
        val games = runCatching { requireNotNull(apis).library.ownedGames() }
            .onFailure { Timber.w(it, "Playtime sync failed") }
            .getOrNull() ?: return@launch

        val appDao = db.steamAppDao()
        db.withTransaction {
            games.forEach { game ->
                appDao.updatePlaytime(
                    game.appId,
                    game.playtimeForever,
                    game.playtimeTwoWeeks,
                    game.lastPlayed,
                )
            }
        }
        Timber.d("Synced playtime for ${games.size} games")
    }

    private fun continuousFriendChecker() = scope.launch {
        val appDao = db.steamAppDao()
        val friendDao = db.steamFriendDao()
        while (isActive && _isLoggedIn.value) {
            delay(15.seconds)
            friendDao.findFriendsInGame()
                .also { friends -> Timber.d("Found ${friends.size} friends in game") }
                .forEach { friend ->
                    if (friend.gameAppID <= 0) {
                        return@forEach
                    }

                    appDao.findAppName(friend.gameAppID)?.let { name ->
                        if (friend.gameName != name) {
                            friendDao.update(friend.copy(gameName = name))
                        }
                    } ?: picsRequestChannel.send(friend.gameAppID)
                }
        }
    }
}
