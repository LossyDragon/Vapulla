package `in`.dragonbra.vapulla.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.*
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.CChat_RequestFriendPersonaStates_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_AckMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_GetRecentMessages_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_IncomingMessage_Notification
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.CFriendMessages_SendMessage_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_CreateFriendInviteToken_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_GetFriendInviteTokens_Request
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.CUserAccount_RevokeFriendInviteToken_Request
import `in`.dragonbra.javasteam.rpc.service.Chat
import `in`.dragonbra.javasteam.rpc.service.FriendMessages
import `in`.dragonbra.javasteam.rpc.service.FriendMessagesClient
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.rpc.service.UserAccount
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendMsgEchoCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.FriendsListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.NicknameListCallback
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.PersonaStatesCallback
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.javasteam.util.log.LogListener
import `in`.dragonbra.javasteam.util.log.LogManager
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.util.findEmotes
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.core.isFriend
import `in`.dragonbra.vapulla.core.isRequest
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.model.AuthResponse
import `in`.dragonbra.vapulla.model.InviteTokenItem
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.Closeable
import javax.inject.Inject

@AndroidEntryPoint
class SteamService : Service() {

    companion object {
        private const val MAX_RETRY_COUNT = 5

        const val BROADCAST_INVITES_LIST = "in.dragonbra.vapulla.service.INVITES_LIST"

        /**
         * Time to back off when we receive an echo message because it means that the user is
         * chatting on another device.
         */
        private const val ECHO_BACKOFF = 5L * DateUtils.MINUTE_IN_MILLIS

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"
    }

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var isLoggedIn: Boolean = false
        private set

    @Volatile
    var isActivityRunning: Boolean = false

    @Volatile
    private var expectDisconnect = false

    /**
     * id of the friend whose chat is currently open, null if no chat open
     */
    @Volatile
    private var chatFriendId: Long? = null

    private lateinit var stateBuffer: PersonaStateBuffer

    private lateinit var steamClient: SteamClient

    private lateinit var unifiedMessages: SteamUnifiedMessages

    private val binder: SteamServiceBinder = SteamServiceBinder(this)

    private val newMessages = mutableMapOf<SteamID, MutableList<MessagingStyle.Message>>()

    private val requestsToNotify = mutableSetOf<SteamID>()

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val steamJob = Job()

    private val steamScope = CoroutineScope(Dispatchers.IO + steamJob)

    private var lastEcho = 0L // Time of the last echo used for notification back off

    private var retryCount = 0

    private var unifiedChat: Chat? = null

    private var unifiedFriendMessages: FriendMessages? = null

    private var unifiedPlayer: Player? = null

    private var userAccount: UserAccount? = null

    lateinit var callbackMgr: CallbackManager

    val disconnectedSubs = mutableSetOf<(DisconnectedCallback) -> Unit>()

    override fun onCreate() {
        super.onCreate()

        Timber.i("onCreate")

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())

        steamClient = SteamClient().apply {
            addHandler(VapullaHandler())

            removeHandler(SteamApps::class.java)
            removeHandler(SteamCloud::class.java)
            removeHandler(SteamGameCoordinator::class.java)
            removeHandler(SteamGameServer::class.java)
            removeHandler(SteamMasterServer::class.java)
            removeHandler(SteamScreenshots::class.java)
            removeHandler(SteamUserStats::class.java)
            removeHandler(SteamWorkshop::class.java)
        }

        callbackMgr = CallbackManager(steamClient)
        unifiedMessages = steamClient.getHandler(SteamUnifiedMessages::class.java)!!

        val logger = object : LogListener {
            override fun onLog(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.tag(clazz?.simpleName ?: "Unknown Class").d(message)
            }

            override fun onError(clazz: Class<*>?, message: String?, throwable: Throwable?) {
                Timber.tag(clazz?.simpleName ?: "Unknown Class").e(message)
            }
        }
        LogManager.addListener(logger)

        callbackMgr.subscribe(ConnectedCallback::class.java, onConnected)
        callbackMgr.subscribe(DisconnectedCallback::class.java, onDisconnected)
        callbackMgr.subscribe(EmoticonListCallback::class.java, onEmoticonList)
        callbackMgr.subscribe(FriendMsgEchoCallback::class.java, onFriendMsgEcho)
        callbackMgr.subscribe(FriendsListCallback::class.java, onFriendsList)
        callbackMgr.subscribe(LoggedOffCallback::class.java, onLoggedOff)
        callbackMgr.subscribe(LoggedOnCallback::class.java, onLoggedOn)
        callbackMgr.subscribe(NicknameListCallback::class.java, onNicknameList)
        callbackMgr.subscribe(PersonaStatesCallback::class.java, onPersonaState)
        callbackMgr.subscribe(
            OfflineMessageNotificationCallback::class.java,
            onOfflineMessageNotification
        )
        callbackMgr.subscribeServiceNotification<
            FriendMessages,
            CFriendMessages_AckMessage_Notification.Builder> {
            Timber.i("onAckMessage")
            steamScope.launch {
                db.chatMessageDao().markRead(it.body.steamidPartner)
            }
        }
        callbackMgr.subscribeServiceNotification<
            FriendMessagesClient,
            CFriendMessages_IncomingMessage_Notification.Builder> {
            Timber.i("onIncomingMessage")
            scope.launch(Dispatchers.IO) {
                val steamID = SteamID(it.body.steamidFriend)
                val steamIdLong = steamID.convertToUInt64()

                when (it.body.chatEntryType) {
                    EChatEntryType.Typing.code() -> {
                        db.withTransaction {
                            db.steamFriendDao().find(steamIdLong)?.let { friend ->
                                db.steamFriendDao().update(
                                    friend.copy(typingTs = System.currentTimeMillis())
                                )
                            }
                        }
                    }

                    EChatEntryType.ChatMsg.code() -> {
                        Timber.d("Message: ${it.body.message}")

                        val msg = ChatMessage(
                            accountId = steamIdLong,
                            fromLocal = it.body.localEcho,
                            message = it.body.message,
                            timestamp = it.body.rtime32ServerTimestamp.toLong(),
                            isUnread = chatFriendId != steamIdLong
                        )

                        db.chatMessageDao().insert(msg)

                        if (steamIdLong != chatFriendId) {
                            postMessageNotification(steamID, it.body.message)
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand")

        if (isRunning && intent != null && intent.hasExtra(EXTRA_ACTION)) {
            val id = SteamID(intent.getLongExtra(EXTRA_ID, 0L))
            val action = intent.getStringExtra(EXTRA_ACTION)

            Timber.i("Received $action action message")
            val steamId = id.convertToUInt64().toInt()

            when (intent.getStringExtra(EXTRA_ACTION)) {
                "reply" -> {
                    scope.launch(Dispatchers.IO) {
                        // TODO emotes?
                        val message = intent.getStringExtra(EXTRA_MESSAGE)!!
                        val emotes = db.emoticonDao().find()
                        val emoteSet = emotes.map { it.name }.toSet()
                        sendMessage(id, message, emoteSet)
                    }
                    notificationManager.cancel(steamId)
                }

                "stop" -> {
                    Intent(VapullaBaseActivity.STOP_INTENT).also(::sendBroadcast)
                    stopSelf()
                }

                "accept_request" -> {
                    scope.launch(Dispatchers.IO) {
                        getHandler<SteamFriends>()?.addFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "ignore_request" -> {
                    scope.launch(Dispatchers.IO) {
                        getHandler<SteamFriends>()?.removeFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "block_request" -> {
                    scope.launch(Dispatchers.IO) {
                        getHandler<SteamFriends>()?.ignoreFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        disconnect()
        Intent(VapullaBaseActivity.STOP_INTENT).also(::sendBroadcast)
    }

    private fun checkNotificationPermission(onGranted: () -> Unit) {
        if (Constants.isAtLeastT) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val checkPermission = ActivityCompat.checkSelfPermission(this, permission)
            if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                Timber.d("Notification perms granted")
                onGranted()
                return
            }
        }

        Timber.d("Notification perms granted")
        onGranted()
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            stateBuffer.start()
            steamThread()
            setNotification(R.string.notificationConnecting)
        }
    }

    fun disconnect() {
        expectDisconnect = true
        steamClient.disconnect()
    }

    fun logOn(details: LogOnDetails) {
        if (isLoggedIn) return
        getHandler<SteamUser>()?.logOn(details)
    }

    private suspend fun postMessageNotification(friendId: SteamID, message: String) {
        if (System.currentTimeMillis() > lastEcho + ECHO_BACKOFF) {
            // User is still chatting on another device
            Timber.d("Skipping chat notification: user recently active on another device")
            return
        }

        Timber.d("Posting chat notification")

        // Get friend details from DB
        val friend = withContext(Dispatchers.IO) {
            db.steamFriendDao().find(friendId.convertToUInt64())
        } ?: run {
            Timber.d("Friend not found in database: ${friendId.convertToUInt64()}")
            return
        }

        // Get or create message list for this friend
        val messages = newMessages.getOrPut(friendId) { mutableListOf() }

        // Build and post notification
        serviceMessageNotification(
            friendId = friendId,
            friend = friend,
            message = message,
            messages = messages
        ) { builder ->
            checkNotificationPermission {
                val steamId = friendId.convertToUInt64().toInt()
                notificationManager.notify(steamId, builder.build())
            }
        }

        // Clear notifications if needed
        if (isActivityRunning && accountManager.prefClearNotifications) {
            notificationManager.cancelAll()
        }
    }

    private fun postFriendRequestNotification(state: PersonaStatesCallback) {
        serviceRequestNotification(state) { builder ->
            checkNotificationPermission {
                val steamId = state.friendID.convertToUInt64().toInt()
                notificationManager.notify(steamId, builder.build())
            }
        }
    }

    private fun clearMessageNotifications(id: SteamID) {
        newMessages[id]?.clear()
        newMessages.remove(id)

        notificationManager.cancel(id.convertToUInt64().toInt())
    }

    fun setChatFriendId(id: SteamID) {
        chatFriendId = id.convertToUInt64()
        clearMessageNotifications(id)
        ackMessage(id)
    }

    fun removeChatFriendId() {
        chatFriendId = null
    }

    /**
     * Request a fresh state of Friend's PersonaStates
     */
    fun getFriendPersonaStates() {
        Timber.d("getFriendPersonaStates")
        val request = CChat_RequestFriendPersonaStates_Request.newBuilder().build()
        unifiedChat?.requestFriendPersonaStates(request)
    }

    /**
     * Acknowledge a message that was unread
     */
    private fun ackMessage(steamID: SteamID) {
        Timber.d("ackMessage($steamID)")
        scope.launch {
            val request = CFriendMessages_AckMessage_Notification.newBuilder().apply {
                steamidPartner = steamID.convertToUInt64()
                timestamp = System.currentTimeMillis().div(1000).toInt()
            }.build()
            unifiedFriendMessages?.ackMessage(request)
        }
    }

    /**
     * Get the last 50 recent messages from a friend conversation.
     */
    suspend fun getMessageHistory(steamID2: SteamID) {
        Timber.d("getMessageHistory($steamID2)")

        val chatFriendId = this.chatFriendId ?: run {
            Timber.e("chatFriendId is null")
            return
        }

        try {
            val request = CFriendMessages_GetRecentMessages_Request.newBuilder().apply {
                steamid1 = accountManager.steamId
                steamid2 = steamID2.convertToUInt64()
                count = 50
                rtime32StartTime = 0
                bbcodeFormat = true
                startOrdinal = 0
                timeLast = Int.MAX_VALUE // More explicit than magic number
                ordinalLast = 0
            }.build()

            val response = withContext(Dispatchers.IO) {
                unifiedFriendMessages?.getRecentMessages(request)?.toDeferred()?.await()
            } ?: run {
                Timber.e("unifiedFriendMessages is null")
                return
            }

            if (response.result != EResult.OK) {
                Timber.d("Failed to get message history for ${steamID2.convertToUInt64()}: ${response.result}")
                return
            }

            // Process all messages in a single transaction
            db.withTransaction {
                // Get existing messages for efficient lookup
                val existingMessages = db.chatMessageDao()
                    .findMessagesInTimeRange(
                        accountId = chatFriendId,
                        timestamps = response.body.messagesList.map { it.timestamp.toLong() }
                    ).associateBy {
                        "${it.message}_${it.timestamp}_${it.fromLocal}"
                    }

                // Process and filter messages that don't exist yet
                val newMessages = response.body.messagesList.mapNotNull { friendMessage ->
                    val steamID = SteamID().apply {
                        set(
                            friendMessage.accountid.toLong(),
                            EUniverse.Public,
                            EAccountType.Individual
                        )
                    }

                    val fromLocal = accountManager.steamId == steamID.convertToUInt64()
                    val timestamp = friendMessage.timestamp.toLong()

                    // Check if message already exists
                    val key = "${friendMessage.message}_${timestamp}_$fromLocal"
                    if (existingMessages.containsKey(key)) {
                        return@mapNotNull null
                    }

                    ChatMessage(
                        accountId = chatFriendId,
                        fromLocal = fromLocal,
                        message = friendMessage.message,
                        timestamp = timestamp,
                        isUnread = false
                    )
                }

                // Batch insert new messages
                if (newMessages.isNotEmpty()) {
                    db.chatMessageDao().insertAll(newMessages)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting message history for ${steamID2.convertToUInt64()}")
        }
    }

    fun setTyping(steamID: SteamID) {
        Timber.d("setTyping($steamID)")
        val request = CFriendMessages_SendMessage_Request.newBuilder().apply {
            chatEntryType = EChatEntryType.Typing.code()
            message = ""
            steamid = steamID.convertToUInt64()
        }.build()
        unifiedFriendMessages?.sendMessage(request)
    }

    suspend fun sendMessage(id: SteamID, msg: String, emoteSet: Set<String>) {
        val trimmedMessage = msg.trim()

        if (trimmedMessage.isEmpty()) {
            return
        }

        // Send the message to steam
        val request = CFriendMessages_SendMessage_Request.newBuilder().apply {
            chatEntryType = EChatEntryType.ChatMsg.code()
            message = msg
            steamid = id.convertToUInt64()
            containsBbcode = true
            echoToSender = false
            lowPriority = false
        }.build()
        unifiedFriendMessages?.sendMessage(request)

        // Then, save the message to our database.
        val formattedMessage = trimmedMessage.replace('\u02D0', ':')
        val emoteMessage = findEmotes(formattedMessage, emoteSet)
        val chatMessage = ChatMessage(
            accountId = id.convertToUInt64(),
            fromLocal = true,
            isUnread = false,
            message = emoteMessage,
            timestamp = System.currentTimeMillis().div(1000)
        )
        db.chatMessageDao().insert(chatMessage)

        clearMessageNotifications(id)
    }

    suspend fun signInViaCredentials(
        iAuthenticator: IAuthenticator,
        accountName: String,
        accountPassword: String
    ): AuthResponse? {
        val authSessionDetails = AuthSessionDetails().apply {
            username = accountName.trim()
            password = accountPassword
            persistentSession = true
            authenticator = iAuthenticator
        }

        return try {
            val authSession = steamClient.authentication
                .beginAuthSessionViaCredentials(authSessionDetails)

            val authPollResult = authSession.pollingWaitForResult()

            // Save our results (username and refresh token) to account manager.
            with(authPollResult) {
                AuthResponse(this.accountName, this.refreshToken)
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun signInViaQR(
        onDrawQRCode: (QrAuthSession) -> Unit
    ): AuthResponse {
        val authSessionDetails = AuthSessionDetails().apply {
            deviceFriendlyName = "Vapulla - Android"
            persistentSession = true
        }

        val authSession = steamClient.authentication
            .beginAuthSessionViaQR(authSessionDetails)

        authSession.challengeUrlChanged = IChallengeUrlChanged { onDrawQRCode(it!!) }

        onDrawQRCode(authSession)

        val pollResponse = authSession.pollingWaitForResult()

        Timber.i("Connected to Steam! Logging in as ${pollResponse.accountName}...")

        return AuthResponse(pollResponse.accountName, pollResponse.refreshToken)
    }

    fun createFriendInviteToken() {
        scope.launch {
            val request = CUserAccount_CreateFriendInviteToken_Request.newBuilder().build()
            val response = userAccount!!.createFriendInviteToken(request).toDeferred().await()

            if (response.result != EResult.OK) {
                Timber.d("Failed to create invite tokens.")
                return@launch
            }

            // We do get a Token, so we'll just refresh the list since it will be the latest.
            getFriendInviteTokens()
        }
    }

    fun revokeFriendInviteToken(token: String) {
        scope.launch {
            val request = CUserAccount_RevokeFriendInviteToken_Request.newBuilder()
                .setInviteToken(token)
                .build()
            val response = userAccount!!.revokeFriendInviteToken(request).toDeferred().await()

            if (response.result != EResult.OK) {
                Timber.d("Failed to revoke invite tokens.")
                return@launch
            }

            // There is no data in the response, so we'll just refresh the list.
            getFriendInviteTokens()
        }
    }

    fun getFriendInviteTokens() {
        steamScope.launch {
            val request = CUserAccount_GetFriendInviteTokens_Request.newBuilder().build()
            val response = userAccount!!.getFriendInviteTokens(request).toDeferred().await()

            if (response.result != EResult.OK) {
                Timber.d("Failed to get invite tokens.")
                return@launch
            }

            val inviteTokens = response.body.tokensList.map { token ->
                InviteTokenItem(
                    token.inviteToken,
                    token.inviteLimit,
                    token.inviteDuration,
                    token.timeCreated.toLong(),
                    token.valid
                )
            }

            Intent(BROADCAST_INVITES_LIST).apply {
                putExtra("invites_list", ArrayList(inviteTokens))
            }.also {
                LocalBroadcastManager.getInstance(this@SteamService).sendBroadcast(it)
            }
        }
    }

    private fun steamThread() {
        steamScope.launch {
            Timber.i("Connecting to steam...")
            isRunning = true
            steamClient.connect()

            while (isRunning && isActive) {
                callbackMgr.runWaitCallbacks(1000)
            }

            Timber.i("Steam thread stopped")
        }
    }

    inline fun <reified T : CallbackMsg> subscribe(
        noinline callbackFunc: (T) -> Unit
    ): Closeable = when (T::class) {
        DisconnectedCallback::class -> {
            @Suppress("UNCHECKED_CAST")
            disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
            Closeable { disconnectedSubs.remove(callbackFunc) }
        }

        else -> callbackMgr.subscribe(T::class.java) { callbackFunc(it) }
    }

    internal inline fun <reified T : ClientMsgHandler> getHandler(): T? =
        steamClient.getHandler(T::class.java)

    //region Callback handlers
    private val onDisconnected = Consumer<DisconnectedCallback> { cb ->
        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            Timber.i("disconnected from steam")
            stopForeground(STOP_FOREGROUND_REMOVE)
            isRunning = false
            steamJob.cancel()
            isLoggedIn = false
            expectDisconnect = false
            stateBuffer.stop()
            disconnectedSubs.forEach { it.invoke(cb) }
        } else {
            Timber.i("failed to connect to steam ${++retryCount} times, trying again...")
            Thread.sleep(2000L)
            steamClient.connect()
            setNotification(R.string.notificationLostConnection)
        }
    }

    private val onConnected = Consumer<ConnectedCallback> {
        Timber.i("(onConnected) connected to steam")
        retryCount = 0
        setNotification(R.string.notificationConnected)
    }

    private val onLoggedOn = Consumer<LoggedOnCallback> {
        Timber.d("onLoggedOn")
        when (it.result) {
            EResult.OK -> {
                isLoggedIn = true
                getHandler<SteamNotifications>()?.requestOfflineMessageCount()
                userAccount = unifiedMessages.createService(UserAccount::class.java)
                unifiedChat = unifiedMessages.createService(Chat::class.java)
                unifiedPlayer = unifiedMessages.createService(Player::class.java)
                unifiedFriendMessages = unifiedMessages.createService(FriendMessages::class.java)
            }

            EResult.InvalidPassword -> accountManager.loginKey = null
            else -> Unit /* no-op */
        }
    }

    private val onLoggedOff = Consumer<LoggedOffCallback> {
        steamClient.disconnect()
    }

    private val onPersonaState = Consumer<PersonaStatesCallback> {
        Timber.d("onPersonaState: ${it.name} | ${it.state}")
        if (!it.friendID.isIndividualAccount) {
            return@Consumer
        }

        if (it.friendID == steamClient.steamID) {
            accountManager.saveLocalUser(it)
            return@Consumer
        }

        stateBuffer.push(it)

        if (requestsToNotify.contains(it.friendID)) {
            postFriendRequestNotification(it)
            requestsToNotify.remove(it.friendID)
        }
    }

    private val onFriendsList = Consumer<FriendsListCallback> { callback ->
        Timber.d("onFriendsList")

        // Launch in the steamScope since we're doing DB operations
        steamScope.launch {
            val isIncremental = callback.isIncremental

            // Process friends and partition them into appropriate lists
            val (friendsToProcess, _) = callback.friendList.partition {
                it.steamID.isIndividualAccount
            }

            val existingFriends = db.steamFriendDao().findAll(
                friendsToProcess.map { it.steamID.convertToUInt64() }
            ).associateBy { it.id }

            val (toAdd, toUpdateOrRemove) = friendsToProcess.partition {
                existingFriends[it.steamID.convertToUInt64()] == null
            }

            // Create new friends list
            val friendsToAdd = toAdd
                .filter { it.relationship.isFriend() || it.relationship.isRequest() }
                .map { friend ->
                    SteamFriend(
                        id = friend.steamID.convertToUInt64(),
                        relation = friend.relationship.code()
                    )
                }

            // Process existing friends
            val (friendsToUpdate, friendsToRemove) = toUpdateOrRemove.partition {
                it.relationship.isFriend() || it.relationship.isRequest()
            }

            // Create update list
            val updatedFriends = friendsToUpdate.mapNotNull { friend ->
                existingFriends[friend.steamID.convertToUInt64()]?.copy(
                    relation = friend.relationship.code()
                )
            }

            // Create remove list
            val removeFriends = friendsToRemove.mapNotNull { friend ->
                existingFriends[friend.steamID.convertToUInt64()]
            }

            // Check for friend requests to notify if incremental
            if (isIncremental) {
                requestsToNotify.addAll(
                    friendsToAdd
                        .filter { it.relation == EFriendRelationship.RequestRecipient.code() }
                        .map { SteamID(it.id) }
                )
            }

            // Perform all database operations in a single transaction
            db.withTransaction {
                // Remove chat messages for removed friends first
                if (removeFriends.isNotEmpty()) {
                    db.chatMessageDao().deleteMessagesForFriends(removeFriends.map { it.id })
                }

                db.steamFriendDao().run {
                    // Perform friend updates
                    if (friendsToAdd.isNotEmpty()) {
                        insertAll(friendsToAdd)
                    }
                    if (updatedFriends.isNotEmpty()) {
                        updateAll(updatedFriends)
                    }
                    if (removeFriends.isNotEmpty()) {
                        removeAll(removeFriends)
                    }
                }
            }
        }
    }

    private val onNicknameList = Consumer<NicknameListCallback> {
        Timber.d("onNicknameList")
        steamScope.launch {
            db.steamFriendDao().clearNicknames()

            val nicknamesToUpdate = it.nicknames.mapNotNull { playerName ->
                val steamId = playerName.steamID.convertToUInt64()
                db.steamFriendDao().find(steamId)?.copy(nickname = playerName.nickname)
            }

            if (nicknamesToUpdate.isNotEmpty()) {
                db.steamFriendDao().updateAll(nicknamesToUpdate)
            }
        }
    }

    private val onOfflineMessageNotification = Consumer<OfflineMessageNotificationCallback> {
        Timber.d("onOfflineMessageNotification")
        if (it.messageCount <= 0) {
            return@Consumer
        }

        getHandler<SteamFriends>()?.requestOfflineMessages()
    }

    private val onEmoticonList = Consumer<EmoticonListCallback> { emoticon ->
        Timber.d("onEmoticonList, size: ${emoticon.emoteList.size}")
        steamScope.launch {
            val emoticons = emoticon.emoteList.map {
                if (it.isSticker) {
                    Emoticon(it.name, true, it.appId)
                } else {
                    Emoticon(it.name.substring(1, it.name.length - 1), false, it.appId)
                }
            }

            db.emoticonDao().replaceAll(emoticons)
        }
    }

    private val onFriendMsgEcho = Consumer<FriendMsgEchoCallback> {
        Timber.d("onFriendMsgEcho")
        steamScope.launch {
            lastEcho = System.currentTimeMillis()

            val msg = ChatMessage(
                accountId = it.recipient.convertToUInt64(),
                fromLocal = true,
                isUnread = false,
                message = it.message.orEmpty(),
                timestamp = it.rTime32ServerTimestamp.toLong()
            )

            db.chatMessageDao().insert(msg)
            db.chatMessageDao().markRead(it.recipient.convertToUInt64())
            clearMessageNotifications(it.recipient)
        }
    }
//endregion
}
