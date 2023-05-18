package `in`.dragonbra.vapulla.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.*
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.base.ClientMsgProtobuf
import `in`.dragonbra.javasteam.enums.EAccountType
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EMsg
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesChatSteamclient.*
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientserver2.CMsgClientUIMode
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesFriendmessagesSteamclient.*
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesPlayerSteamclient.*
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesUseraccountSteamclient.*
import `in`.dragonbra.javasteam.rpc.service.Chat
import `in`.dragonbra.javasteam.rpc.service.FriendMessages
import `in`.dragonbra.javasteam.rpc.service.Player
import `in`.dragonbra.javasteam.rpc.service.UserAccount
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.OnChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.authentication.SteamAuthentication
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.SteamUnifiedMessages
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodNotification
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodResponse
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.MachineAuthDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.OTPDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.ICallbackMsg
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.compose.util.findEmotes
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.model.InviteTokenItem
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import java.io.Closeable
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO friend requests don't get a name or avatar, PersonaStateBuffer doesn't allow it.

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

    lateinit var callbackMgr: CallbackManager

    lateinit var steamClient: SteamClient

    private lateinit var stateBuffer: PersonaStateBuffer

    private val binder: SteamServiceBinder = SteamServiceBinder(this)

    private val newMessages = mutableMapOf<SteamID, MutableList<MessagingStyle.Message>>()

    private val requestsToNotify = mutableSetOf<SteamID>()

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    val disconnectedSubs = mutableSetOf<(DisconnectedCallback) -> Unit>()

    private lateinit var unifiedMessages: SteamUnifiedMessages

    private var retryCount = 0

    private var unifiedChat: Chat? = null

    private var unifiedPlayer: Player? = null

    private var userAccount: UserAccount? = null

    private var unifiedFriendMessages: FriendMessages? = null

    /**
     * Time of the last echo used for notification back off
     */
    private var lastEcho = 0L

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var isLoggedIn: Boolean = false

    @Volatile
    var isActivityRunning: Boolean = false

    @Volatile
    private var expectDisconnect = false

    /**
     * id of the friend whose chat is currently open, null if no chat open
     */
    @Volatile
    private var chatFriendId: Long? = null

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
        unifiedMessages = steamClient.getHandler(SteamUnifiedMessages::class.java)

        callbackMgr.subscribe(ConnectedCallback::class.java, onConnected)
        callbackMgr.subscribe(DisconnectedCallback::class.java, onDisconnected)
        callbackMgr.subscribe(EmoticonListCallback::class.java, onEmoticonList)
        callbackMgr.subscribe(FriendMsgEchoCallback::class.java, onFriendMsgEcho)
        callbackMgr.subscribe(FriendsListCallback::class.java, onFriendsList)
        callbackMgr.subscribe(LoggedOffCallback::class.java, onLoggedOff)
        callbackMgr.subscribe(LoggedOnCallback::class.java, onLoggedOn)
        callbackMgr.subscribe(NicknameListCallback::class.java, onNicknameList)
        callbackMgr.subscribe(PersonaStatesCallback::class.java, onPersonaState)
        callbackMgr.subscribe(ServiceMethodResponse::class.java, onMethodResponse)
        callbackMgr.subscribe(ServiceMethodNotification::class.java, onMethodNotification)
        callbackMgr.subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth)
        callbackMgr.subscribe(
            OfflineMessageNotificationCallback::class.java,
            onOfflineMessageNotification
        )
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("onStartCommand")

        if (isRunning && intent != null && intent.hasExtra(EXTRA_ACTION)) {
            val id = SteamID(intent.getLongExtra(EXTRA_ID, 0L))
            val action = intent.getStringExtra(EXTRA_ACTION)

            Timber.i("Received $action action message")
            val steamId = id.convertToUInt64().toInt()

            when (intent.getStringExtra(EXTRA_ACTION)) {
                "reply" -> {
                    // TODO: Make reply intents proper with messaging style
                    val message = intent.getStringExtra(EXTRA_MESSAGE)!!
                    scope.launch(Dispatchers.IO) {
                        // TODO emotes?
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
                        getHandler<SteamFriends>().addFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "ignore_request" -> {
                    scope.launch(Dispatchers.IO) {
                        getHandler<SteamFriends>().removeFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "block_request" -> {
                    scope.launch(Dispatchers.IO) {
                        getHandler<SteamFriends>().ignoreFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")
        disconnect()
        val stopIntent = Intent(VapullaBaseActivity.STOP_INTENT)
        sendBroadcast(stopIntent)
    }

    private fun checkNotificationPermission(onGranted: () -> Unit) {
        if (Constants.isAtLeastT) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val checkPermission = ActivityCompat.checkSelfPermission(this, permission)
            if (checkPermission == PackageManager.PERMISSION_GRANTED) {
                onGranted()
            }
        }

        onGranted()
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            stateBuffer.start()
            Thread(steamThread, "Steam Thread").start()
            setNotification(R.string.notificationConnecting)
        }
    }

    fun disconnect() {
        expectDisconnect = true
        steamClient.disconnect()
    }

    fun logOn(details: LogOnDetails) {
        if (isLoggedIn) return

        details.isShouldRememberPassword = true

        if (accountManager.hasSentryFile) {
            details.sentryFileHash = accountManager.readSentryFile()
        }

        getHandler<SteamUser>().logOn(details)
    }

    private fun postMessageNotification(friendId: SteamID, message: String) {
        if (System.currentTimeMillis() <= lastEcho + ECHO_BACKOFF) {
            // User is still chatting on another device
            return
        }

        val friend = db.steamFriendDao().find(friendId.convertToUInt64()) ?: return

        val messages: MutableList<MessagingStyle.Message> =
            if (!newMessages.containsKey(friendId)) {
                val list = LinkedList<MessagingStyle.Message>()
                newMessages[friendId] = list
                list
            } else {
                newMessages[friendId]!!
            }

        scope.launch {
            serviceMessageNotification(friendId, friend, message, messages) { builder ->
                checkNotificationPermission {
                    val steamId = friendId.convertToUInt64().toInt()
                    notificationManager.notify(steamId, builder.build())
                }
            }
        }

        if (isActivityRunning && accountManager.prefClearNotifications) {
            notificationManager.cancelAll()
        }
    }

    private fun postFriendRequestNotification(state: PersonaState) {
        scope.launch {
            serviceRequestNotification(state) { builder ->
                checkNotificationPermission {
                    val steamId = state.friendID.convertToUInt64().toInt()
                    notificationManager.notify(steamId, builder.build())
                }
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
        unifiedChat?.RequestFriendPersonaStates(request)
    }

    /**
     * Acknowledge a message that was unread
     */
    private fun ackMessage(steamID: SteamID) {
        Timber.d("ackMessage($steamID)")
        scope.launch {
            val msgNotification = CFriendMessages_AckMessage_Notification.newBuilder().apply {
                steamidPartner = steamID.convertToUInt64()
                timestamp = System.currentTimeMillis().div(1000).toInt()
            }.build()

            unifiedFriendMessages?.AckMessage(msgNotification)
        }
    }

    /**
     * Get the last 50 recent messages from a friend conversation.
     */
    fun getMessageHistory(steamID2: SteamID) {
        Timber.d("getMessageHistory($steamID2)")
        val msgHistory = CFriendMessages_GetRecentMessages_Request.newBuilder().apply {
            steamid1 = accountManager.steamId
            steamid2 = steamID2.convertToUInt64()
            count = 50
            rtime32StartTime = 0
            bbcodeFormat = true
            startOrdinal = 0
            timeLast = 2147483647 // ???
            ordinalLast = 0
        }.build()

        unifiedFriendMessages?.GetRecentMessages(msgHistory)
    }

    fun setTyping(steamID: SteamID) {
        Timber.d("setTyping($steamID)")
        val message = CFriendMessages_SendMessage_Request.newBuilder().apply {
            chatEntryType = EChatEntryType.Typing.code()
            message = ""
            steamid = steamID.convertToUInt64()
        }.build()
        unifiedFriendMessages?.SendMessage(message)
    }

    fun sendMessage(id: SteamID, msg: String, emoteSet: Set<String>) {
        val trimmedMessage = msg.trim()

        if (trimmedMessage.isEmpty()) {
            return
        }

        // Send the message to steam
        val message = CFriendMessages_SendMessage_Request.newBuilder().apply {
            chatEntryType = EChatEntryType.ChatMsg.code()
            message = msg
            steamid = id.convertToUInt64()
            containsBbcode = true
            echoToSender = false
            lowPriority = false
        }.build()
        unifiedFriendMessages?.SendMessage(message)

        // Then, save the message to our database.
        val formattedMessage = trimmedMessage.replace('\u02D0', ':')
        val emoteMessage = findEmotes(formattedMessage, emoteSet)
        val chatMessage = ChatMessage(
            accountid = id.convertToUInt64(),
            fromLocal = true,
            isUnread = false,
            message = emoteMessage,
            timestamp = System.currentTimeMillis().div(1000)
        )
        db.chatMessageDao().insert(chatMessage)

        clearMessageNotifications(id)
    }

    suspend fun signInViaCredentials(
        coroutineScope: CoroutineScope,
        iAuthenticator: IAuthenticator,
        accountName: String,
        accountPassword: String
    ): Pair<String, String>? {
        val authSessionDetails = AuthSessionDetails().apply {
            username = accountName.trim()
            password = accountPassword
            persistentSession = true
            authenticator = iAuthenticator
        }

        val auth = SteamAuthentication(steamClient, unifiedMessages)
        return try {
            val authSession = auth.beginAuthSessionViaCredentials(authSessionDetails)

            val authPollResult = authSession.pollingWaitForResult(coroutineScope)

            // Save our results (username and refresh token) to account manager.
            Pair(authPollResult.accountName, authPollResult.refreshToken)
        } catch (e: IllegalArgumentException) {
            coroutineScope.cancel(CancellationException(e.message))
            null
        }
    }

    suspend fun signInViaQR(
        coroutineScope: CoroutineScope,
        onDrawQRCode: (QrAuthSession) -> Unit
    ): Pair<String, String> {
        val auth = SteamAuthentication(steamClient, unifiedMessages)

        val authSessionDetails = AuthSessionDetails().apply {
            deviceFriendlyName = "Vapulla - Android"
            persistentSession = true
        }

        val authSession: QrAuthSession = auth.beginAuthSessionViaQR(authSessionDetails)

        authSession.challengeUrlChanged = object : OnChallengeUrlChanged {
            override fun onChanged(qrAuthSession: QrAuthSession) {
                onDrawQRCode(qrAuthSession)
            }
        }

        onDrawQRCode(authSession)

        val pollResponse = authSession.pollingWaitForResult(coroutineScope)

        Timber.i("Connected to Steam! Logging in as ${pollResponse.accountName}...")

        return Pair(pollResponse.accountName, pollResponse.refreshToken)
    }

    fun createFriendInviteToken() {
        val request = CUserAccount_CreateFriendInviteToken_Request.newBuilder()
        userAccount?.CreateFriendInviteToken(request.build())
    }

    fun getFriendInviteTokens() {
        val request = CUserAccount_GetFriendInviteTokens_Request.newBuilder()
        userAccount?.GetFriendInviteTokens(request.build())
    }

    fun revokeFriendInviteToken(token: String) {
        val request = CUserAccount_RevokeFriendInviteToken_Request.newBuilder()
        request.inviteToken = token
        userAccount?.RevokeFriendInviteToken(request.build())
    }

    inline fun <reified T : ICallbackMsg> subscribe(
        noinline callbackFunc: (T) -> Unit
    ): Closeable? {
        return when (T::class) {
            DisconnectedCallback::class -> {
                @Suppress("UNCHECKED_CAST")
                disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
                Closeable { disconnectedSubs.remove(callbackFunc) }
            }

            else -> callbackMgr.subscribe(T::class.java) { callbackFunc(it) }
        }
    }

    private val steamThread: Runnable = Runnable {
        Timber.i("Connecting to steam...")
        isRunning = true
        steamClient.connect()

        while (isRunning) {
            callbackMgr.runWaitCallbacks(1000)
        }

        Timber.i("Steam thread stopped")
    }

    inline fun <reified T : ClientMsgHandler> getHandler(): T {
        return steamClient.getHandler(T::class.java)
    }

    //region Callback handlers
    private val onDisconnected = Consumer<DisconnectedCallback> { cb ->
        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            Timber.i("disconnected from steam")
            stopForeground(STOP_FOREGROUND_REMOVE)
            isRunning = false
            isLoggedIn = false
            expectDisconnect = false
            stateBuffer.stop()
            disconnectedSubs.forEach { it.invoke(cb) }
        } else {
            Timber.i("failed to connect to steam ${++retryCount} times, trying again...")
            scope.launch {
                delay(1.seconds)
                steamClient.connect()
            }
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
                getHandler<SteamNotifications>().requestOfflineMessageCount()

                // Set the client's "UI" mode to receive new callbacks.
                val uiMode = ClientMsgProtobuf<CMsgClientUIMode.Builder>(
                    CMsgClientUIMode::class.java,
                    EMsg.ClientCurrentUIMode
                ).apply {
                    body.uimode = 0
                    body.chatMode = 2
                }
                steamClient.send(uiMode)

                userAccount = UserAccount(getHandler())
                unifiedChat = Chat(getHandler())
                unifiedPlayer = Player(getHandler())
                unifiedFriendMessages = FriendMessages(getHandler())
            }

            EResult.InvalidPassword -> accountManager.loginKey = null
            else -> Unit /* no-op */
        }
    }

    private val onLoggedOff = Consumer<LoggedOffCallback> {
        steamClient.disconnect()
    }

    private val onUpdateMachineAuth = Consumer<UpdateMachineAuthCallback> {
        Timber.i("received sentry file called ${it.fileName}")
        accountManager.updateSentryFile(it)

        val otp = OTPDetails().apply {
            identifier = it.oneTimePassword.identifier
            type = it.oneTimePassword.type
        }

        val details = MachineAuthDetails().apply {
            bytesWritten = it.bytesToWrite
            eResult = EResult.OK
            fileName = it.fileName
            fileSize = accountManager.sentrySize.toInt()
            jobID = it.jobID
            lastError = 0
            offset = it.offset
            oneTimePassword = otp
            sentryFileHash = accountManager.readSentryFile()
        }

        getHandler<SteamUser>().sendMachineAuthResponse(details)
    }

    private val onPersonaState = Consumer<PersonaStatesCallback> {
        Timber.d("onPersonaState: ${it.personaStates.size}")
        it.personaStates.forEach { state ->
            if (!state.friendID.isIndividualAccount) {
                return@forEach
            }

            if (state.friendID == steamClient.steamID) {
                accountManager.saveLocalUser(state)
                return@forEach
            }

            stateBuffer.push(state)

            if (requestsToNotify.contains(state.friendID)) {
                postFriendRequestNotification(state)
                requestsToNotify.remove(state.friendID)
            }
        }
    }

    private val onFriendsList = Consumer<FriendsListCallback> {
        Timber.d("onFriendsList")

        val inc = it.isIncremental

        val friendsToAdd: MutableList<SteamFriend> = LinkedList()
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()
        val friendsToRemove: MutableList<SteamFriend> = LinkedList()

        it.friendList.forEach { currentFriend ->
            if (!currentFriend.steamID.isIndividualAccount) {
                return@forEach
            }

            var friend = db.steamFriendDao().find(currentFriend.steamID.convertToUInt64())
            if (friend == null) {
                if (currentFriend.relationship == EFriendRelationship.Friend ||
                    currentFriend.relationship == EFriendRelationship.RequestRecipient
                ) {
                    friend = SteamFriend(currentFriend.steamID.convertToUInt64())
                    friend.relation = currentFriend.relationship.code()
                    friendsToAdd.add(friend)
                }
            } else {
                if (currentFriend.relationship == EFriendRelationship.Friend ||
                    currentFriend.relationship == EFriendRelationship.RequestRecipient
                ) {
                    friend.relation = currentFriend.relationship.code()
                    friendsToUpdate.add(friend)
                } else {
                    friendsToRemove.add(friend)
                    db.chatMessageDao().remove(friend.id)
                }
            }

            if (inc && friend?.relation == EFriendRelationship.RequestRecipient.code()) {
                requestsToNotify.add(currentFriend.steamID)
            }
        }

        db.steamFriendDao().run {
            insert(*friendsToAdd.toTypedArray())
            update(*friendsToUpdate.toTypedArray())
            remove(*friendsToRemove.toTypedArray())
        }
    }

    private val onNicknameList = Consumer<NicknameListCallback> {
        Timber.d("onNicknameList")

        val steamFriendDao = db.steamFriendDao()
        steamFriendDao.clearNicknames()

        it.nicknames.forEach { playerName ->
            val steamId = playerName.steamID.convertToUInt64()
            steamFriendDao.find(steamId)?.let { friend ->
                friend.nickname = playerName.nickname
                steamFriendDao.update(friend)
            }
        }
    }

    private val onOfflineMessageNotification = Consumer<OfflineMessageNotificationCallback> {
        Timber.d("onOfflineMessageNotification")
        if (it.messageCount <= 0) {
            return@Consumer
        }

        getHandler<SteamFriends>().requestOfflineMessages()
    }

    private val onEmoticonList = Consumer<EmoticonListCallback> { emoticon ->
        val emoticons = emoticon.getEmoteList().map {
            if (it.isSticker) {
                Emoticon(it.name, true, it.appId)
            } else {
                Emoticon(it.name.substring(1, it.name.length - 1), false, it.appId)
            }
        }.toTypedArray()

        db.emoticonDao().run {
            delete()
            insert(*emoticons)
        }
    }

    private val onFriendMsgEcho = Consumer<FriendMsgEchoCallback> {
        Timber.d("onFriendMsgEcho")
        lastEcho = System.currentTimeMillis()

        val msg = ChatMessage(
            accountid = it.sender.convertToUInt64(),
            fromLocal = true,
            isUnread = false,
            message = it.message,
            timestamp = it.rTime32ServerTimestamp.toLong()
        )

        db.chatMessageDao().insert(msg)
        db.chatMessageDao().markRead(it.sender.convertToUInt64())
        clearMessageNotifications(it.sender)
    }

    private val onMethodResponse = Consumer<ServiceMethodResponse> { resp ->
        Timber.d("onMethodResponse: ${resp.rpcName}")

        if (resp.result != EResult.OK) {
            Timber.w("Unified service request failed with " + resp.result)
            return@Consumer
        }

        if (resp.serviceName == UserAccount::class.simpleName) {
            if (resp.rpcName == "GetFriendInviteTokens") {
                resp.getDeserializedResponse<CUserAccount_GetFriendInviteTokens_Response.Builder>(
                    CUserAccount_GetFriendInviteTokens_Response::class.java
                ).also { cb ->
                    val inviteTokens = cb.tokensList.map { token ->
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
                        LocalBroadcastManager.getInstance(this).sendBroadcast(it)
                    }
                }
            }
            if (resp.rpcName == "RevokeFriendInviteToken") {
                // There is no data in the response, so we'll just refresh the list.
                getFriendInviteTokens()
            }
            if (resp.rpcName == "CreateFriendInviteToken") {
                // We do get a Token, so we'll just refresh the list since it will be the latest.
                getFriendInviteTokens()
            }
        }

        if (resp.serviceName == FriendMessages::class.simpleName) {
            if (resp.rpcName == "GetRecentMessages") {
                resp.getDeserializedResponse<CFriendMessages_GetRecentMessages_Response.Builder>(
                    CFriendMessages_GetRecentMessages_Response::class.java
                ).also { cb ->
                    cb.messagesList.forEachIndexed { index, friendMessage ->
                        if (chatFriendId == null) {
                            throw NullPointerException("chatFriendId null in onMethodResponse")
                        }

                        val steamID = SteamID()
                        steamID.set(
                            friendMessage.accountid.toLong(),
                            EUniverse.Public,
                            EAccountType.Individual
                        ) // Also sus for TO-DO below

                        val fromLocal = accountManager.steamId == steamID.convertToUInt64()
                        val timestamp = friendMessage.timestamp.toLong()

                        // TODO we're still duping messages when getting history

                        // Msg found, skip
                        db.chatMessageDao().find(
                            message = friendMessage.message,
                            timestamp = timestamp,
                            accountid = chatFriendId!!,
                            fromLocal = fromLocal // Most likely the culprit
                        ).also { msg ->
                            if (msg != null) {
                                Timber.d("Skipping Msg History: ${friendMessage.message}")
                                return@forEachIndexed
                            }
                        }

                        Timber.d("Item: $index")
                        Timber.d("accountid -> ${friendMessage.accountid}")
                        Timber.d("timestamp -> ${friendMessage.timestamp}")
                        Timber.d("message -> ${friendMessage.message}")
                        Timber.d("----\n")

                        val chatMsg = ChatMessage(
                            accountid = chatFriendId!!,
                            fromLocal = fromLocal,
                            message = friendMessage.message,
                            timestamp = timestamp,
                            isUnread = false
                        )

                        db.chatMessageDao().insert(chatMsg)
                    }
                }
            }
        }
    }

    private val onMethodNotification = Consumer<ServiceMethodNotification> {
        Timber.d("onMethodNotification: ${it.rpcName}")

        when (val callbackObject = it.body) {
            is CFriendMessages_IncomingMessage_Notification -> {
                when (callbackObject.chatEntryType) {
                    EChatEntryType.Typing.code() -> {
                        val steamID = SteamID(callbackObject.steamidFriend)
                        db.steamFriendDao().run {
                            find(steamID.convertToUInt64())?.apply {
                                typingTs = System.currentTimeMillis()
                            }?.also { friend ->
                                update(friend)
                            }
                        }
                    }
                    EChatEntryType.ChatMsg.code() -> {
                        Timber.d("Message: ${callbackObject.message}")
                        val steamID = SteamID(callbackObject.steamidFriend)

                        val msg = ChatMessage(
                            accountid = steamID.convertToUInt64(),
                            fromLocal = callbackObject.localEcho,
                            message = callbackObject.message,
                            timestamp = callbackObject.rtime32ServerTimestamp.toLong(),
                            isUnread = chatFriendId != steamID.convertToUInt64()
                        )
                        db.chatMessageDao().insert(msg)

                        if (steamID.convertToUInt64() != chatFriendId) {
                            postMessageNotification(steamID, callbackObject.message)
                        }
                    }
                }
            }
            is CFriendMessages_AckMessage_Notification -> {
                db.chatMessageDao().markRead(callbackObject.steamidPartner)
            }
            else -> Timber.w("Could not process ${it.rpcName}")
        }
    }

//endregion
}
