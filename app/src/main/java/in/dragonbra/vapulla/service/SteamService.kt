package `in`.dragonbra.vapulla.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
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
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.VapullaBaseActivity
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.extension.vapulla
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.PersonaStateBuffer
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.Closeable
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class SteamService : Service() {

    companion object {
        private const val MAX_RETRY_COUNT = 5

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"

        const val SERVERS_FILE = "servers.bin"
    }

    // Coroutines
    private val scope = CoroutineScope(
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("SteamService")
    )

    // Koin DI Injection
    private val db: VapullaDatabase by inject()
    private val account: AccountManager by inject()
    private val serviceManager: ServiceManager by inject()

    // Steam-related properties
    private var steamClient: SteamClient? = null
    private var callbackMgr: CallbackManager? = null
    private var steamThreadJob: Job? = null
    private val subscriptions: MutableSet<Closeable> = mutableSetOf()
    private var connectedSignal: CompletableDeferred<Unit>? = null

    // State management
    private lateinit var stateBuffer: PersonaStateBuffer
    private lateinit var remoteInput: RemoteInput
    private var expectDisconnect: Boolean = false
    private var isRunning: Boolean = false
    private var isWaitingForQRAuth: Boolean = false
    private var retryCount = 0

    private var currentAuthJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        Timber.i("onCreate")

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())
        remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        scope.launch {
            serviceManager.commandChannel.collect { command ->
                when (command) {
                    is ServiceCommand.Login -> handleCredentialLogin(command)
                    is ServiceCommand.LoginQR -> handleQRLogin()
                    is ServiceCommand.LoginQRCancel -> handleQrCodeCancel()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

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

        serviceManager.setServiceRunning(true)

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

        val intent = Intent(VapullaBaseActivity.STOP_INTENT)
        sendBroadcast(intent)
        serviceManager.setServiceRunning(false)
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

        // Stop state buffer
        if (::stateBuffer.isInitialized) {
            stateBuffer.stop()
        }

        isRunning = false
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
                        mgr.subscribe(onFriendMsgEcho)
                    )
                )
            }

            // Configure handlers
            with(client) {
                addHandler<VapullaHandler>()
                // Remove unnecessary handlers to save memory
                removeHandler<SteamApps>()
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

    private fun handleCredentialLogin(command: ServiceCommand.Login) {
        Timber.d("handleCredentialLogin() - Starting and Logging into Steam")

        // Cancel any ongoing QR auth
        cancelCurrentAuthOperation()

        currentAuthJob = scope.launch {
            try {
                initializeSteamClient()
                connectToSteam()

                connectedSignal?.await()
                    ?: throw IllegalStateException("Connection signal not available")

                val authDetails = AuthSessionDetails().apply {
                    this.authenticator = command.authenticator
                    this.deviceFriendlyName = "Vapulla ${BuildConfig.VERSION_NAME}"
                    this.username = command.username.trim()
                    this.password = command.password?.trim()
                    this.persistentSession = true
                }

                serviceManager.emitLoginResult(LoginResult.Loading)

                val authSession = steamClient?.authentication
                    ?.beginAuthSessionViaCredentials(authDetails)
                    ?.await() ?: throw IllegalStateException("Steam client not available")

                val pollResult = authSession.pollingWaitForResult().await()

                if (pollResult.accountName.isBlank() || pollResult.refreshToken.isBlank()) {
                    val result = LoginResult.Error("Account Name or Refresh Token is blank")
                    serviceManager.emitLoginResult(result)
                    return@launch
                }

                account.username = pollResult.accountName
                account.loginKey = pollResult.refreshToken

                loginToSteam(
                    pollResult.accountName,
                    command.password?.trim(),
                    pollResult.refreshToken
                )
            } catch (e: CancellationException) {
                Timber.i(e, "Credential login cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Error during credential login")
                serviceManager.emitLoginResult(LoginResult.Error("Login failed: ${e.message}"))
            } finally {
                currentAuthJob = null
            }
        }
    }

    private fun handleQRLogin() {
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

                authSession.challengeUrlChanged = object : IChallengeUrlChanged {
                    override fun onChanged(qrAuthSession: QrAuthSession?) {
                        if (isWaitingForQRAuth) {
                            scope.launch {
                                val qrCode = qrAuthSession?.challengeUrl.orEmpty()
                                serviceManager.emitLoginResult(LoginResult.QRCode(qrCode))
                            }
                        }
                    }
                }

                // Send initial QR code
                serviceManager.emitLoginResult(LoginResult.QRCode(authSession.challengeUrl))

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
                    loginToSteam(
                        accountName = authPollResult.accountName,
                        password = null,
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
                serviceManager.emitLoginResult(LoginResult.Error("QR login failed: ${e.message}"))
            } finally {
                isWaitingForQRAuth = false
                withContext(NonCancellable) {
                    serviceManager.emitLoginResult(LoginResult.QRCodeEnded)
                }
                currentAuthJob = null
            }
        }
    }

    private fun handleQrCodeCancel() {
        Timber.i("Cancelling QR Login")
        cancelCurrentAuthOperation()
    }

    private fun loginToSteam(
        accountName: String,
        password: String? = null,
        refreshToken: String? = null
    ) {
        val loginDetails = LogOnDetails(
            username = accountName,
            password = password,
            shouldRememberPassword = true,
            accessToken = refreshToken,
            loginID = Utils.getUniqueId(account),
            machineName = "Vapulla ${BuildConfig.VERSION_NAME}",
            chatMode = ChatMode.NEW_STEAM_CHAT
        )

        steamClient?.getHandler<SteamUser>()?.logOn(loginDetails)
    }

    private fun connectToSteam() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0

            stateBuffer.start()

            Timber.i("Connecting to steam...")

            isRunning = true
            steamClient?.connect()

            NotificationHelper.updateServiceNotification(
                context = this,
                text = getString(R.string.notificationConnecting)
            )

            steamThreadJob = scope.launch(Dispatchers.IO) {
                Timber.i("Callback loop started on thread: ${Thread.currentThread().name}")

                try {
                    while (isRunning && callbackMgr != null) {
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

            isRunning = false

            stateBuffer.stop()
        } else {
            Timber.i("onDisconnected() - Failed to connect ${++retryCount} times, trying again...")

            NotificationHelper.updateServiceNotification(
                context = this,
                text = getString(R.string.notificationLostConnection)
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
            text = getString(R.string.notificationConnected)
        )

        connectedSignal?.complete(Unit)
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        Timber.d("onLoggedOn() got result ${it.result}")
        when (it.result) {
            EResult.OK -> {
                steamClient?.getHandler<SteamNotifications>()?.requestOfflineMessageCount()
                scope.launch {
                    serviceManager.emitLoginResult(LoginResult.Success)
                }
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
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        Timber.d("onFriendsList()")
    }

    private val onFriendMsgHistory: Consumer<FriendMsgHistoryCallback> = Consumer {
        Timber.d("onFriendMsgHistory()")
    }

    private val onFriendMsg: Consumer<FriendMsgCallback> = Consumer {
        Timber.d("onFriendMsg()")
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        Timber.d("onNicknameList()")
    }

    private val onOfflineMessages: Consumer<OfflineMessageNotificationCallback> = Consumer {
        Timber.d("onOfflineMessages()")
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        Timber.d("onEmoticonList()")
    }

    private val onFriendMsgEcho: Consumer<FriendMsgEchoCallback> = Consumer {
        Timber.d("onFriendMsgEcho()")
    }

//endregion
}
