package `in`.dragonbra.vapulla.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.core.app.*
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
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
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.MachineAuthDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.OTPDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.ICallbackMsg
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.UnifiedChatHandler
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.RecentMessagesResponseCallback
import `in`.dragonbra.vapulla.steam.callback.SendMessageResponseCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceMethodCallback
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import `in`.dragonbra.vapulla.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class SteamService : Service() {

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 100
        private const val MAX_RETRY_COUNT = 5

        /**
         * Time to back off when we receive an echo message because it means that the user is
         * chatting on another device.
         */
        private const val ECHO_BACKOFF = 5L * DateUtils.MINUTE_IN_MILLIS

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"

        const val SERVERS_FILE = "servers.bin"
    }

    lateinit var callbackMgr: CallbackManager

    lateinit var steamClient: SteamClient

    private lateinit var handler: Handler

    private lateinit var prefs: SharedPreferences

    private lateinit var stateBuffer: PersonaStateBuffer

    private val binder: SteamBinder = SteamBinder()

    private val handlerThread = HandlerThread("SteamService Handler")

    private val newMessages = mutableMapOf<SteamID, MutableList<MessagingStyle.Message>>()

    private val requestsToNotify = mutableSetOf<SteamID>()

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val subscriptions = mutableSetOf<Closeable?>()

    val disconnectedSubs = mutableSetOf<(DisconnectedCallback) -> Unit>()

    private var retryCount = 0

    /**
     * Time of the last echo used for notification back off
     */
    private var lastEcho = 0L

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var account: AccountManager

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

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())

        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(File(filesDir, SERVERS_FILE)))
        }

        steamClient = SteamClient(config).apply {
            addHandler(VapullaHandler())
            addHandler(UnifiedChatHandler())

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

        subscriptions.apply {
            // add(callbackMgr.subscribe(FriendMsgCallback::class.java, onFriendMsg))
            add(callbackMgr.subscribe(ConnectedCallback::class.java, onConnected))
            add(callbackMgr.subscribe(DisconnectedCallback::class.java, onDisconnected))
            add(callbackMgr.subscribe(EmoticonListCallback::class.java, onEmoticonList))
            add(callbackMgr.subscribe(FriendMsgEchoCallback::class.java, onFriendMsgEcho))
            add(callbackMgr.subscribe(FriendMsgHistoryCallback::class.java, onFriendMsgHistory))
            add(callbackMgr.subscribe(FriendsListCallback::class.java, onFriendsList))
            add(callbackMgr.subscribe(LoggedOffCallback::class.java, onLoggedOff))
            add(callbackMgr.subscribe(LoggedOnCallback::class.java, onLoggedOn))
            add(callbackMgr.subscribe(LoginKeyCallback::class.java, onNewLoginKey))
            add(callbackMgr.subscribe(NicknameListCallback::class.java, onNicknameList))
            add(
                callbackMgr.subscribe(
                    OfflineMessageNotificationCallback::class.java,
                    onOfflineMessageNotification
                )
            )
            add(callbackMgr.subscribe(PersonaStatesCallback::class.java, onPersonaState))
            add(
                callbackMgr.subscribe(
                    RecentMessagesResponseCallback::class.java,
                    onRecentMessagesCallback
                )
            )
            add(
                callbackMgr.subscribe(
                    SendMessageResponseCallback::class.java,
                    onSendMessageCallback
                )
            )
            add(callbackMgr.subscribe(ServiceMethodCallback::class.java, onServiceMethodCallback))
            add(callbackMgr.subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth))
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.i("onBind")
        return binder
    }

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
                    scope.executeAsyncTask {
                        val emotes = db.emoticonDao().find()
                        val emoteSet = emotes.map { it.name }.toSet()
                        sendMessage(id, message, emoteSet)
                    }
                    notificationManager.cancel(steamId)
                }

                "stop" -> {
                    val stopIntent = Intent(VapullaBaseActivity.STOP_INTENT)
                    sendBroadcast(stopIntent)
                    stopSelf()
                }

                "accept_request" -> {
                    scope.executeAsyncTask {
                        getHandler<SteamFriends>().addFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "ignore_request" -> {
                    scope.executeAsyncTask {
                        getHandler<SteamFriends>().removeFriend(id)
                    }
                    notificationManager.cancel(steamId)
                }

                "block_request" -> {
                    scope.executeAsyncTask {
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
        handlerThread.quit()
        val stopIntent = Intent(VapullaBaseActivity.STOP_INTENT)
        sendBroadcast(stopIntent)
    }

    private fun setNotification(@StringRes string: Int) {
        val text = getString(string)
        serviceNotification(text) { builder ->
            startForeground(ONGOING_NOTIFICATION_ID, builder.build())
        }
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
        if (isLoggedIn) {
            return
        }

        details.isShouldRememberPassword = true

        if (account.hasSentryFile) {
            details.sentryFileHash = account.readSentryFile()
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
                val steamId = friendId.convertToUInt64().toInt()
                notificationManager.notify(steamId, builder.build())
            }
        }

        if (isActivityRunning && prefs.getBoolean("pref_clear_notifications", false)) {
            notificationManager.cancelAll()
        }
    }

    private fun postFriendRequestNotification(state: PersonaState) {
        scope.launch {
            serviceRequestNotification(state) { builder ->
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
    }

    fun removeChatFriendId() {
        chatFriendId = null
    }

    fun getMessageHistory(steamID2: SteamID) {
        getHandler<UnifiedChatHandler>().getRecentMessages(account.steamId, steamID2)
    }

    fun sendMessage(id: SteamID, message: String, emoteSet: Set<String>) {
        val trimmedMessage = message.trim()

        if (trimmedMessage.isEmpty()) {
            return
        }

        val formattedMessage = trimmedMessage.replace('\u02D0', ':')
        val emoteMessage = Utils.findEmotes(formattedMessage, emoteSet)

        getHandler<UnifiedChatHandler>().sendMessage(id, message)

        val msg = ChatMessage(
            message = emoteMessage,
            timestamp = System.currentTimeMillis(),
            friendId = id.convertToUInt64(),
            fromLocal = true,
            unread = false,
            timestampConfirmed = false
        )
        db.chatMessageDao().insert(msg)

        clearMessageNotifications(id)
    }

    inline fun <reified T : ICallbackMsg>
    subscribe(noinline callbackFunc: (T) -> Unit): Closeable? {
        return when (T::class) {
            DisconnectedCallback::class -> {
                @Suppress("UNCHECKED_CAST")
                disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
                Closeable {
                    disconnectedSubs.remove(callbackFunc)
                }
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

    inner class SteamBinder : Binder() {
        fun getService(): SteamService = this@SteamService
    }

    inline fun <reified T : ClientMsgHandler> getHandler(): T {
        return steamClient.getHandler(T::class.java)
    }

    //region Callback handlers

    private val onDisconnected = Consumer<DisconnectedCallback> { cb ->
        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            Timber.i("disconnected from steam")
            stopForeground(true)
            isRunning = false
            isLoggedIn = false
            expectDisconnect = false
            stateBuffer.stop()
            disconnectedSubs.forEach { it.invoke(cb) }
        } else {
            Timber.i("failed to connect to steam ${++retryCount} times, trying again...")
            handler.postDelayed({ steamClient.connect() }, 1000L)
            setNotification(R.string.notificationLostConnection)
        }
    }

    private val onConnected = Consumer<ConnectedCallback> {
        Timber.i("connected to steam")
        retryCount = 0
        setNotification(R.string.notificationConnected)

        if (isLoggedIn) {
            val details = LogOnDetails().apply {
                username = account.username
                loginKey = account.loginKey
                isShouldRememberPassword = true

                if (account.hasSentryFile) {
                    sentryFileHash = account.readSentryFile()
                }
            }

            getHandler<SteamUser>().logOn(details)
        }
    }

    private val onLoggedOn = Consumer<LoggedOnCallback> {
        when (it.result) {
            EResult.OK -> {
                isLoggedIn = true
                getHandler<SteamNotifications>().requestOfflineMessageCount()

                // Set the client's "UI" mode to receive new callbacks.
                getHandler<VapullaHandler>().setClientUIMode()
            }

            EResult.InvalidPassword -> account.loginKey = null
            else -> Unit /* no-op */
        }
    }

    private val onLoggedOff = Consumer<LoggedOffCallback> {
        steamClient.disconnect()
    }

    private val onNewLoginKey = Consumer<LoginKeyCallback> {
        Timber.i("received login key")
        account.loginKey = it.loginKey
        account.uniqueId = it.uniqueID

        getHandler<SteamUser>().acceptNewLoginKey(it)
    }

    private val onUpdateMachineAuth = Consumer<UpdateMachineAuthCallback> {
        Timber.i("received sentry file called ${it.fileName}")
        account.updateSentryFile(it)

        val otp = OTPDetails().apply {
            identifier = it.oneTimePassword.identifier
            type = it.oneTimePassword.type
        }

        val details = MachineAuthDetails().apply {
            bytesWritten = it.bytesToWrite
            fileName = it.fileName
            fileSize = account.sentrySize.toInt()
            jobID = it.jobID
            lastError = 0
            offset = it.offset
            oneTimePassword = otp
            sentryFileHash = account.readSentryFile()
            seteResult(EResult.OK)
        }

        getHandler<SteamUser>().sendMachineAuthResponse(details)
    }

    private val onPersonaState = Consumer<PersonaStatesCallback> {
        Timber.d("onPersonaState")
        it.personaStates.forEach { state ->
            if (!state.friendID.isIndividualAccount) {
                return@forEach
            }

            if (state.friendID == steamClient.steamID) {
                account.saveLocalUser(state)
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
        val dao = db.steamFriendDao()
        val inc = it.isIncremental

        val friendsToAdd: MutableList<SteamFriend> = LinkedList()
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()
        val friendsToRemove: MutableList<SteamFriend> = LinkedList()

        it.friendList.forEach { currentFriend ->
            if (!currentFriend.steamID.isIndividualAccount) {
                return@forEach
            }

            var friend = dao.find(currentFriend.steamID.convertToUInt64())

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
                }
            }

            // TODO: this crashes
            if (inc && friend!!.relation == EFriendRelationship.RequestRecipient.code()) {
                requestsToNotify.add(currentFriend.steamID)
            }
        }

        dao.insert(*friendsToAdd.toTypedArray())
        dao.update(*friendsToUpdate.toTypedArray())
        dao.remove(*friendsToRemove.toTypedArray())
    }

    private val onFriendMsgHistory = Consumer<FriendMsgHistoryCallback> { cb ->
        cb.messages.forEach {
            val fromLocal = cb.steamID != it.steamID
            val friendId = cb.steamID.convertToUInt64()
            val timestamp = it.timestamp.time

            db.chatMessageDao()
                .find(it.message, timestamp, friendId, fromLocal, true)
                .also { msg ->
                    if (msg != null) {
                        return@forEach
                    }
                }

            val findMessages = db.chatMessageDao().find(it.message, friendId, fromLocal, false)
            val unconfirmedMessages =
                findMessages.sortedWith { o1, o2 ->
                    (abs(timestamp - o1.timestamp) - abs(timestamp - o2.timestamp)).toInt()
                }

            if (unconfirmedMessages.isNotEmpty()) {
                unconfirmedMessages[0].timestamp = timestamp
                unconfirmedMessages[0].timestampConfirmed = true

                db.chatMessageDao().update(unconfirmedMessages[0])
            } else {
                ChatMessage(
                    message = it.message,
                    timestamp = timestamp,
                    friendId = friendId,
                    fromLocal = fromLocal,
                    unread = it.isUnread,
                    timestampConfirmed = true
                ).also { msg ->
                    db.chatMessageDao().insert(msg)
                }
            }
        }
    }

    private val onNicknameList = Consumer<NicknameListCallback> {
        db.steamFriendDao().clearNicknames()

        it.nicknames.forEach { playerName ->
            val steamId = playerName.steamID.convertToUInt64()
            db.steamFriendDao().find(steamId)?.let { friend ->
                friend.nickname = playerName.nickname
                db.steamFriendDao().update(friend)
            }
        }
    }

    private val onOfflineMessageNotification = Consumer<OfflineMessageNotificationCallback> {
        if (it.messageCount <= 0) {
            return@Consumer
        }

        getHandler<SteamFriends>().requestOfflineMessages()
    }

    private val onEmoticonList = Consumer<EmoticonListCallback> { emoticon ->
        Timber.d("onEmoticonList")

        val emoticons = emoticon.getEmoteList().map {
            if (it.isSticker) {
                Emoticon(it.name, true, it.appId)
            } else {
                Emoticon(it.name.substring(1, it.name.length - 1), false, it.appId)
            }
        }.toTypedArray()

        db.emoticonDao().delete()
        db.emoticonDao().insert(*emoticons)
    }

    private val onFriendMsgEcho = Consumer<FriendMsgEchoCallback> {
        lastEcho = System.currentTimeMillis()

        val msg = ChatMessage(
            message = it.message,
            timestamp = System.currentTimeMillis(),
            friendId = it.sender.convertToUInt64(),
            fromLocal = true,
            unread = false,
            timestampConfirmed = false
        )
        db.chatMessageDao().insert(msg)
        db.chatMessageDao().markRead(it.sender.convertToUInt64())

        clearMessageNotifications(it.sender)
    }

    private val onServiceMethodCallback = Consumer<ServiceMethodCallback> {
        Timber.i("onServiceMethodCallback: ${it.jobName}")

        // Friend is typing
        if (it.entryType == EChatEntryType.Typing) {
            // Add Typing timeout???
            val friend = db.steamFriendDao().find(it.steamID.convertToUInt64())

            if (friend != null) {
                friend.typingTs = System.currentTimeMillis()
                db.steamFriendDao().update(friend)
            }
        }

        // Friend sent a message
        if (it.entryType == EChatEntryType.ChatMsg && it.message.isNotEmpty()) {
            val msg = ChatMessage(
                it.message,
                System.currentTimeMillis(),
                it.steamID.convertToUInt64(),
                false,
                chatFriendId != it.steamID.convertToUInt64(),
                false
            )
            db.chatMessageDao().insert(msg)

            if (it.steamID.convertToUInt64() != chatFriendId) {
                postMessageNotification(it.steamID, it.message)
            }
        }
    }

    // Callback on messages you sent. Trasformed to Unified/BBCode
    private val onSendMessageCallback = Consumer<SendMessageResponseCallback> {
        Timber.i("onSendMessageCallback: ${it.jobName}")

        Timber.i("onSendMessageCallback: ${it.modifiedMessage}")
        Timber.i("onSendMessageCallback: ${it.timestamp}")
    }

    // Callback to receive recent messages on a chat. Unified/BBCode enabled
    private val onRecentMessagesCallback = Consumer<RecentMessagesResponseCallback> {
        Timber.i("onRecentMessagesCallback: ${it.jobName}")

        Timber.i("onRecentMessagesCallback: ${it.messageHistory!!.toList()}")
    }

//endregion
}
