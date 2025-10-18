package `in`.dragonbra.vapulla.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.RemoteInput
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.PICSRequest
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
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
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
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
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.SteamApp
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.service.handler.VapullaHandler
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.util.timeChunked
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SteamService : Service() {

    companion object {
        private const val MAX_RETRY_COUNT = 5

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"

        const val SERVERS_FILE = "servers.bin"

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
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("SteamService")
    )

    // Koin DI Injection
    private val db: VapullaDatabase by inject()
    private val account: AccountManager by inject()

    // Steam-related properties
    private var steamClient: SteamClient? = null
    private var callbackMgr: CallbackManager? = null
    private var steamThreadJob: Job? = null
    private val subscriptions: MutableSet<Closeable> = mutableSetOf()
    private var connectedSignal: CompletableDeferred<Unit>? = null

    // State management
    private lateinit var remoteInput: RemoteInput
    private var expectDisconnect: Boolean = false
    private var isWaitingForQRAuth: Boolean = false
    private var retryCount = 0
    private val requestsToNotify = mutableSetOf<SteamID>()

    private val picsRequestChannel = Channel<Int>(Channel.UNLIMITED)

    private var currentAuthJob: Job? = null

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

        val data = intent?.getStringExtra("extra_data")
        Timber.d("Received data: $data") // TODO re-add notification intents.

        val notification = NotificationHelper.createServiceNotification(
            context = this,
            text = "Starting..."
        )
        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        return START_STICKY
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

        val intent = Intent("stop")
        sendBroadcast(intent)
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

        val serverFile = File(filesDir, SERVERS_FILE)
        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(serverFile))
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
                    )
                )
            }

            // Configure handlers
            with(client) {
                addHandler<VapullaHandler>()

                removeHandler<SteamCloud>()
                removeHandler<SteamGameCoordinator>()
                removeHandler<SteamGameServer>()
                removeHandler<SteamMasterServer>()
                removeHandler<SteamScreenshots>()
                removeHandler<SteamUserStats>()
                removeHandler<SteamWorkshop>()
            }
        }
    }

    fun handleCredentialLogin(
        username: String? = null,
        password: String? = null,
        refreshToken: String? = null,
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
                var loginKey = account.loginKey

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

                    account.username = username
                    account.loginKey = loginKey
                } else {
                    // Auto sign in
                    username = account.username!!
                    loginKey = account.loginKey!!
                }

                loginToSteam(
                    accountName = username,
                    refreshToken = loginKey
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
                            delay(authSession.pollingInterval.toLong())
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
                    account.username = authPollResult.accountName
                    account.loginKey = authPollResult.refreshToken
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

    fun handleQrCodeCancel() {
        Timber.i("Cancelling QR Login")
        cancelCurrentAuthOperation()
    }

    private fun loginToSteam(
        accountName: String,
        refreshToken: String? = null
    ) {
        val loginDetails = LogOnDetails(
            username = accountName,
            shouldRememberPassword = true,
            accessToken = refreshToken,
            loginID = Utils.getUniqueId(account),
            machineName = "Vapulla ${BuildConfig.VERSION_NAME}",
            chatMode = ChatMode.NEW_STEAM_CHAT
        )

        steamClient?.getHandler<SteamUser>()?.logOn(loginDetails)
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
                text = "Connecting"
            )

            steamThreadJob = scope.launch(Dispatchers.IO) {
                Timber.i("Callback loop started on thread: ${Thread.currentThread().name}")

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

        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            Timber.i("onDisconnected() - Disconnected from steam")

            stopForeground(STOP_FOREGROUND_REMOVE)

            _isRunning.value = false
            _isLoggedIn.value = false
        } else {
            Timber.i("onDisconnected() - Failed to connect ${++retryCount} times, trying again...")

            NotificationHelper.updateServiceNotification(
                context = this,
                text = "Lost Connection"
            )

            scope.launch {
                delay(duration = 1.seconds)
                steamClient?.connect()
            }
        }
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        Timber.i("onConnected() - Connected to Steam!")

        retryCount = 0

        NotificationHelper.updateServiceNotification(
            context = this,
            text = "Connected"
        )

        connectedSignal?.complete(Unit)
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        Timber.d("onLoggedOn() got result ${it.result}")
        when (it.result) {
            EResult.OK -> {
                _isLoggedIn.value = true

                steamClient?.getHandler<SteamNotifications>()?.requestOfflineMessageCount()
                steamClient?.getHandler<SteamFriends>()?.setPersonaState(EPersonaState.Online)
                steamClient?.getHandler<VapullaHandler>()?.getEmoticonList()

                scope.launch {
                    _loginResult.emit(LoginResult.Success)
                }

                continuousPicsChanges()
                continuousPicsChecker()
                continuousFriendChecker()
            }

            EResult.InvalidPassword -> account.loginKey = null

            else -> Timber.w("onLoggedOn() got unknown result ${it.result}")
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        Timber.d("onLoggedOff()")
        steamClient?.disconnect()
    }

    private val onPersonaState: Consumer<PersonaStateCallback> = Consumer {
        Timber.d("onPersonaState()")
        scope.launch {
            if (!it.friendID.isIndividualAccount) {
                return@launch
            }

            if (it.friendID == steamClient!!.steamID) {
                account.saveLocalUser(it)
                return@launch
            }

            val id = it.friendID.convertToUInt64()
            val friend = db.steamFriendDao().find(id) ?: return@launch

            Timber.d("${it.name} is ${it.state} - ${it.lastLogOff.time} - ${it.lastLogOn.time}")

            if (it.state != EPersonaState.Offline || it.lastLogOff.time > friend.lastLogOff) {
                db.steamFriendDao().update(
                    friend.copy(
                        avatar = it.avatarHash.toHexString(),
                        gameAppID = it.gameAppID,
                        gameDataBlob = it.gameDataBlob,
                        gameID = it.gameID,
                        gameName = db.steamAppDao().find(it.gameAppID)?.name ?: it.gameName,
                        lastLogOff = it.lastLogOff.time,
                        lastLogOn = it.lastLogOn.time,
                        name = it.name,
                        state = it.state,
                        stateFlags = it.stateFlags,
                        statusFlags = it.statusFlags,
                    )
                )
            }

            if (requestsToNotify.contains(it.friendID)) {
                scope.launch {
                    NotificationHelper.sendFriendRequestNotification(
                        context = applicationContext,
                        friendId = it.friendID.convertToUInt64(),
                        friendName = it.name,
                        avatarUrl = Utils.getAvatarURL(it.avatarHash.toHexString())
                    )
                }
                requestsToNotify.remove(it.friendID)
            }
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        Timber.d("onFriendsList()")

        scope.launch {
            val dao = db.steamFriendDao()
            val isIncremental = it.isIncremental

            val friendsToAdd = mutableListOf<SteamFriend>()
            val friendsToUpdate = mutableListOf<SteamFriend>()
            val friendsToRemove = mutableListOf<SteamFriend>()

            it.friendList
                .filter { item -> item.steamID.isIndividualAccount }
                .forEach { friendInfo ->
                    val steamId = friendInfo.steamID.convertToUInt64()
                    val existingFriend = dao.find(steamId)
                    val relationCode = friendInfo.relationship

                    val isValidRelationship = friendInfo.relationship in listOf(
                        EFriendRelationship.Friend,
                        EFriendRelationship.RequestRecipient
                    )

                    when {
                        existingFriend == null && isValidRelationship -> {
                            friendsToAdd.add(
                                SteamFriend(steamId).apply {
                                    relation = relationCode
                                }
                            )

                            // Track new friend requests for notifications
                            if (isIncremental && friendInfo.relationship == EFriendRelationship.RequestRecipient) {
                                requestsToNotify.add(friendInfo.steamID)
                            }
                        }

                        existingFriend != null && isValidRelationship -> {
                            friendsToUpdate.add(
                                existingFriend.apply {
                                    relation = relationCode
                                }
                            )
                        }

                        existingFriend != null && !isValidRelationship -> {
                            friendsToRemove.add(existingFriend)
                        }
                    }
                }

            // Batch database operations
            dao.insert(friendsToAdd)
            dao.update(friendsToUpdate)
            dao.remove(friendsToRemove)
        }
    }

    private val onFriendMsgHistory: Consumer<FriendMsgHistoryCallback> = Consumer {
        Timber.d("onFriendMsgHistory()")
        val dao = db.chatMessageDao()
        val friendId = it.steamID.convertToUInt64()

        it.messages.forEach { message ->
            val isFromLocal = it.steamID != message.steamID
            val timestamp = message.timestamp.time

            // Skip if we already have this confirmed message
            if (dao.find(message.message, timestamp, friendId, isFromLocal, true) != null) {
                return@forEach
            }

            // Try to find and update an unconfirmed message
            val unconfirmedMessage = dao.find(message.message, friendId, isFromLocal, false)
                .minByOrNull { kotlin.math.abs(timestamp - it.timestamp) }

            if (unconfirmedMessage != null) {
                // Update existing unconfirmed message
                unconfirmedMessage.apply {
                    this.timestamp = timestamp
                    this.timestampConfirmed = true
                }
                dao.update(unconfirmedMessage)
            } else {
                // Insert new confirmed message
                dao.insert(
                    ChatMessage(
                        message = message.message,
                        timestamp = timestamp,
                        friendId = friendId,
                        fromLocal = isFromLocal,
                        unread = message.unread,
                        timestampConfirmed = true
                    )
                )
            }
        }
    }

    private val onFriendMsg: Consumer<FriendMsgCallback> = Consumer {
        Timber.d("onFriendMsg()")
        // TODO ?
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        Timber.d("onNicknameList()")
        scope.launch {
            val dao = db.steamFriendDao()
            dao.clearNicknames()

            val friendsToUpdate = it.nicknames.mapNotNull { nicknameInfo ->
                dao.find(nicknameInfo.steamID.convertToUInt64())?.apply {
                    nickname = nicknameInfo.nickname
                }
            }

            dao.update(friendsToUpdate)
        }
    }

    private val onOfflineMessages: Consumer<OfflineMessageNotificationCallback> = Consumer {
        Timber.d("onOfflineMessages()")
        if (it.messageCount > 0) {
            steamClient?.getHandler<SteamFriends>()?.requestOfflineMessages()
        }
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        Timber.d("onEmoticonList()")

        scope.launch {
            db.emoticonDao().replaceAll(it.emoteList)
        }
    }

    private val onFriendMsgEcho: Consumer<FriendMsgEchoCallback> = Consumer {
        Timber.d("onFriendMsgEcho()")
        // TODO ?
    }

    private val onPicsChanges: Consumer<PICSChangesCallback> = Consumer {
        if (account.lastChangeNumber == it.currentChangeNumber) {
            Timber.w("Change number was the same as last change number, skipping")
            return@Consumer
        }

        Timber.d("onPicsChanges()")

        account.lastChangeNumber = it.currentChangeNumber
    }

    private val onPicsProduct: Consumer<PICSProductInfoCallback> = Consumer {
        Timber.d("onPicsProduct()")

        scope.launch {
            val steamApps = it.apps.values.mapNotNull { app ->
                val dbApp = db.steamAppDao().find(app.id)

                if (app.changeNumber == dbApp?.lastChangeNumber) {
                    return@mapNotNull null
                }

                SteamApp(
                    id = app.id,
                    name = app.keyValues["common"]["name"].value.orEmpty(),
                    lastChangeNumber = app.changeNumber
                )
            }

            db.steamAppDao().insert(steamApps)
        }
    }

// endregion

    private fun continuousPicsChanges() = scope.launch {
        while (isActive && _isLoggedIn.value) {
            val lastChangeNumber = account.lastChangeNumber

            Timber.d("picsGetChangesSince($lastChangeNumber)")

            steamClient?.getHandler<SteamApps>()?.picsGetChangesSince(
                lastChangeNumber = lastChangeNumber,
                sendAppChangeList = true,
                sendPackageChangelist = true,
            )

            delay(1.minutes)
        }
    }

    private fun continuousPicsChecker() = scope.launch {
        picsRequestChannel.receiveAsFlow()
            .timeChunked(50, 1.seconds)
            .collect { ids ->
                Timber.d("Collected ${ids.size} app(s) to query PICS")
                steamClient?.getHandler<SteamApps>()?.picsGetProductInfo(
                    apps = ids.map { PICSRequest(id = it, accessToken = 0) },
                    packages = emptyList()
                )
            }
    }

    private fun continuousFriendChecker() = scope.launch {
        while (isActive && _isLoggedIn.value) {
            delay(10.seconds)
            db.steamFriendDao().findFriendsInGame()
                .also { friends -> Timber.d("Found ${friends.size} friends in game") }
                .forEach { friend ->
                    db.steamAppDao().find(friend.gameAppID)?.let { app ->
                        if (friend.gameName != app.name) {
                            Timber.d("Updating ${friend.name} with game ${app.name}")
                            db.steamFriendDao().update(friend.copy(gameName = app.name))
                        }
                    } ?: picsRequestChannel.send(friend.gameAppID)
                }
        }
    }
}
