package `in`.dragonbra.vapulla.service

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
import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.activity.VapullaBaseActivity
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.broadcastreceiver.ReplyReceiver.Companion.KEY_TEXT_REPLY
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.data.entity.ChatMessage
import `in`.dragonbra.vapulla.data.entity.Emoticon
import `in`.dragonbra.vapulla.data.entity.SteamFriend
import `in`.dragonbra.vapulla.extension.vapulla
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.VapullaHandler
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceMethodCallback
import `in`.dragonbra.vapulla.steam.callback.ServiceServiceMethodCallback
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.*
import `in`.dragonbra.vapulla.util.Utils.avatarOptions
import `in`.dragonbra.vapulla.util.Utils.isAtLeastN
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.*
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import org.spongycastle.util.encoders.Hex
import java.io.Closeable
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class SteamService : Service(), VapullaLogger {

    companion object {
        private const val ONGOING_NOTIFICATION_ID = 100
        private const val MAX_RETRY_COUNT = 5

        /**
         * Time to back off when we receive an echo message because it means that the user is
         * chatting on another device.
         */
        private const val ECHO_BACKOFF = 5L * DateUtils.MINUTE_IN_MILLIS

        /**
         * Time to back off when we receive a new message to prevent spam
         */
        private const val NEW_MESSAGE_BACKOFF = DateUtils.MINUTE_IN_MILLIS

        const val EXTRA_ACTION = "action"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_ID = "id"

        const val SERVERS_FILE = "servers.bin"
    }

    private val binder: SteamBinder = SteamBinder()

    lateinit var steamClient: SteamClient

    lateinit var callbackMgr: CallbackManager

    private val subscriptions: MutableSet<Closeable?> = mutableSetOf()

    private val newMessages: MutableMap<SteamID,
            MutableList<NotificationCompat.MessagingStyle.Message>> = mutableMapOf()

    val disconnectedSubs: MutableSet<(DisconnectedCallback) -> Unit> = mutableSetOf()

    private val requestsToNotify: MutableSet<SteamID> = mutableSetOf()

    private val handlerThread = HandlerThread("SteamService Handler")

    private lateinit var handler: Handler

    private lateinit var prefs: SharedPreferences

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var account: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private lateinit var stateBuffer: PersonaStateBuffer

    @Volatile
    var isRunning: Boolean = false

    @Volatile
    var isLoggedIn: Boolean = false

    @Volatile
    var isActivityRunning: Boolean = false

    @Volatile
    private var expectDisconnect = false

    private var retryCount = 0

    /**
     * id of the friend whose chat is currently open, null if no chat open
     */
    @Volatile
    private var chatFriendId: Long? = null

    private lateinit var remoteInput: RemoteInput

    /**
     * Time of the last echo used for notification back off
     */
    private var lastEcho = 0L

    override fun onCreate() {
        vapulla().graph.inject(this)
        super.onCreate()

        info("onCreate")

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())

        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(File(filesDir, SERVERS_FILE)))
        }

        steamClient = SteamClient(config).apply {
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

        subscriptions.apply {
            add(callbackMgr.subscribe(DisconnectedCallback::class.java, onDisconnected))
            add(callbackMgr.subscribe(ConnectedCallback::class.java, onConnected))
            add(callbackMgr.subscribe(LoggedOnCallback::class.java, onLoggedOn))
            add(callbackMgr.subscribe(LoggedOffCallback::class.java, onLoggedOff))
            add(callbackMgr.subscribe(LoginKeyCallback::class.java, onNewLoginKey))
            add(callbackMgr.subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth))
            add(callbackMgr.subscribe(PersonaStatesCallback::class.java, onPersonaState))
            add(callbackMgr.subscribe(FriendsListCallback::class.java, onFriendsList))
            add(callbackMgr.subscribe(FriendMsgHistoryCallback::class.java, onFriendMsgHistory))
            // add(callbackMgr.subscribe(FriendMsgCallback::class.java, onFriendMsg))
            add(callbackMgr.subscribe(NicknameListCallback::class.java, onNicknameList))
            add(callbackMgr.subscribe(
                    OfflineMessageNotificationCallback::class.java, onOfflineMessageNotification))
            add(callbackMgr.subscribe(EmoticonListCallback::class.java, onEmoticonList))
            add(callbackMgr.subscribe(FriendMsgEchoCallback::class.java, onFriendMsgEcho))
            add(callbackMgr.subscribe(ServiceMethodCallback::class.java, onServiceMethod))
            add(callbackMgr.subscribe(
                    ServiceServiceMethodCallback::class.java, onServiceMethodResponse))
        }

        remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
                .setLabel("Reply")
                .build()

        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    override fun onBind(intent: Intent): IBinder? {
        info("onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        info("onStartCommand")

        if (isRunning && intent != null && intent.hasExtra(EXTRA_ACTION)) {
            val id = SteamID(intent.getLongExtra(EXTRA_ID, 0L))
            val action = intent.getStringExtra(EXTRA_ACTION)

            info("Received $action action message")

            when (intent.getStringExtra(EXTRA_ACTION)) {
                "reply" -> {
                    val message = intent.getStringExtra(EXTRA_MESSAGE)!!
                    // TODO: Make reply intents proper with messaging style
                    runOnBackgroundThread {
                        val emotes = db.emoticonDao().find()
                        val emoteSet = emotes.map { it.name }.toSet()
                        sendMessage(id, message, emoteSet)
                    }

                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "stop" -> {
                    sendBroadcast(Intent(VapullaBaseActivity.STOP_INTENT))
                    stopSelf()
                }
                "accept_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.addFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "ignore_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.removeFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
                "block_request" -> {
                    runOnBackgroundThread {
                        getHandler<SteamFriends>()?.ignoreFriend(id)
                    }
                    notificationManager.cancel(id.convertToUInt64().toInt())
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        info("onDestroy")
        disconnect()
        handlerThread.quit()
        sendBroadcast(Intent(VapullaBaseActivity.STOP_INTENT))
    }

    private fun setNotification(text: String) {
        val logOutIntent = Intent(this, LogOutReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, logOutIntent, 0)

        val builder = NotificationCompat.Builder(this, "vapulla-service")
                .setDefaults(0)
                .setShowWhen(false)
                .setContentTitle("Vapulla")
                .setContentText(text)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                Intent(this, HomeActivity::class.java),
                                0
                        )
                )
                .setSmallIcon(R.drawable.ic_vapulla)
                .setVibrate(longArrayOf(-1L))
                .setSound(null)
                .addAction(
                        R.drawable.ic_exit_to_app,
                        getString(R.string.notificationActionLogOut),
                        pendingIntent
                )

        if (isAtLeastN) {
            builder.priority = NotificationManager.IMPORTANCE_LOW
        } else {
            @Suppress("DEPRECATION")
            builder.priority = Notification.PRIORITY_LOW
        }

        startForeground(ONGOING_NOTIFICATION_ID, builder.build())
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            stateBuffer.start()
            Thread(steamThread, "Steam Thread").start()
            setNotification(getString(R.string.notificationConnecting))
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
        if (account.hasSentryFile()) {
            details.sentryFileHash = account.readSentryFile()
        }
        getHandler<SteamUser>()?.logOn(details)
    }

    private fun postMessageNotification(friendId: SteamID, message: String) {
        if (System.currentTimeMillis() <= lastEcho + ECHO_BACKOFF) {
            // User is still chatting on another device
            return
        }

        val friend = db.steamFriendDao().find(friendId.convertToUInt64()) ?: return

        val messages: MutableList<NotificationCompat.MessagingStyle.Message> =
                if (!newMessages.containsKey(friendId)) {
                    val list = LinkedList<NotificationCompat.MessagingStyle.Message>()
                    newMessages[friendId] = list
                    list
                } else {
                    newMessages[friendId]!!
                }

        val currentTs = System.currentTimeMillis()
        val backoff = messages.isNotEmpty() &&
                currentTs < messages[messages.size - 1].timestamp + NEW_MESSAGE_BACKOFF

        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(Utils.getAvatarUrl(friend.avatar))
                    .apply(avatarOptions)
                    .submit()
                    .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }

        val steamUser = Person
                .Builder()
                .setName(friend.name ?: "")
                .setIcon(IconCompat.createWithBitmap(bitmap))
                .build()

        val newMessage = NotificationCompat.MessagingStyle.Message(message, currentTs, steamUser)
        messages.add(newMessage)

        val style = NotificationCompat.MessagingStyle(steamUser)

        messages.forEach {
            style.addMessage(it)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                friendId.convertToUInt64().toInt(),
                getMessageReplyIntent(friendId.convertToUInt64()),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
                R.drawable.ic_send,
                getString(R.string.notificationActionReply),
                replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.INTENT_STEAM_ID, friendId.convertToUInt64())
        }
        val pendingIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(
                        friendId.convertToUInt64().toInt(),
                        PendingIntent.FLAG_UPDATE_CURRENT
                )

        val notification = NotificationCompat.Builder(this, "vapulla-message")
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setStyle(style)
                .setSmallIcon(R.drawable.ic_message)
                .setLargeIcon(bitmap)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(backoff)
                .addAction(replyAction)
                .build()

        notificationManager.notify(friendId.convertToUInt64().toInt(), notification)

        if (isActivityRunning && prefs.getBoolean("pref_clear_notifications", false)) {
            removeNotifications()
        }
    }

    private fun removeNotifications() {
        notificationManager.cancelAll()
    }

    private fun postFriendRequestNotification(state: PersonaState) {
        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                    .asBitmap()
                    .load(Utils.getAvatarUrl(Hex.toHexString(state.avatarHash)))
                    .apply(avatarOptions)
                    .submit()
                    .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                Intent(this, AcceptRequestReceiver::class.java).apply {
                    putExtra(AcceptRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
                },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ignorePendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                Intent(this, IgnoreRequestReceiver::class.java).apply {
                    putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
                },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                state.friendID.convertToUInt64().toInt(),
                Intent(this, BlockRequestReceiver::class.java).apply {
                    putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
                },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "vapulla-friend-request")
                .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_add_friend)
                .setLargeIcon(bitmap)
                .setContentText(getString(R.string.notificationMessageFriendRequest, state.name))
                .setContentTitle(getString(R.string.notificationTitleFriendRequest))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(
                        PendingIntent.getActivity(
                                this,
                                0,
                                Intent(this, HomeActivity::class.java), 0)
                )
                .addAction(
                        R.drawable.ic_check,
                        getString(R.string.notificationActionAccept),
                        acceptPendingIntent
                )
                .addAction(
                        R.drawable.ic_close,
                        getString(R.string.notificationActionIgnore),
                        ignorePendingIntent
                )
                .addAction(
                        R.drawable.ic_block,
                        getString(R.string.notificationActionBlock),
                        blockPendingIntent
                )
                .build()

        notificationManager.notify(state.friendID.convertToUInt64().toInt(), notification)
    }

    private fun getMessageReplyIntent(id: Long): Intent =
            Intent(this, ReplyReceiver::class.java).apply {
                putExtra(ReplyReceiver.EXTRA_ID, id)
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
        getHandler<VapullaHandler>()?.getRecentMessages(account.steamId, steamID2)
    }

    fun sendMessage(id: SteamID, message: String, emoteSet: Set<String>) {
        val trimmed = message.trim()

        if (Strings.isNullOrEmpty(trimmed)) {
            return
        }

        val emoteMessage = Utils.findEmotes(trimmed.replace('\u02D0', ':'), emoteSet)

        // getHandler<SteamFriends>()?.sendChatMessage(id, EChatEntryType.ChatMsg, message)
        getHandler<VapullaHandler>()?.sendMessage(id, message)

        db.chatMessageDao().insert(ChatMessage(
                message = emoteMessage,
                timestamp = System.currentTimeMillis(),
                friendId = id.convertToUInt64(),
                fromLocal = true,
                unread = false,
                timestampConfirmed = false
        ))

        clearMessageNotifications(id)
    }

    inline fun <reified T : ICallbackMsg>
            subscribe(noinline callbackFunc: (T) -> Unit): Closeable? =

            when (T::class) {
                DisconnectedCallback::class -> {
                    @Suppress("UNCHECKED_CAST")
                    disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
                    Closeable {
                        disconnectedSubs.remove(callbackFunc)
                    }
                }
                else -> callbackMgr.subscribe(T::class.java) { callbackFunc(it) }
            }

    private val steamThread: Runnable = Runnable {
        info("Connecting to steam...")
        isRunning = true
        steamClient.connect()

        while (isRunning) {
            callbackMgr.runWaitCallbacks(1000)
        }

        info("Steam thread stopped")
    }

    inner class SteamBinder : Binder() {
        fun getService(): SteamService = this@SteamService
    }

    inline fun <reified T : ClientMsgHandler> getHandler(): T? =
            this.steamClient.getHandler(T::class.java)

    //region Callback handlers

    private val onDisconnected: Consumer<DisconnectedCallback> = Consumer { cb ->
        if (expectDisconnect || retryCount >= MAX_RETRY_COUNT) {
            info("disconnected from steam")
            stopForeground(true)
            isRunning = false
            isLoggedIn = false
            expectDisconnect = false
            stateBuffer.stop()
            disconnectedSubs.forEach { it.invoke(cb) }
        } else {
            info("failed to connect to steam ${++retryCount} times, trying again...")
            handler.postDelayed({ steamClient.connect() }, 1000L)
            setNotification(getString(R.string.notificationLostConnection))
        }
    }

    private val onConnected: Consumer<ConnectedCallback> = Consumer {
        info("connected to steam")
        retryCount = 0
        setNotification(getString(R.string.notificationConnected))

        if (isLoggedIn) {
            val details = LogOnDetails()
            details.username = account.username
            details.loginKey = account.loginKey
            details.isShouldRememberPassword = true
            if (account.hasSentryFile()) {
                details.sentryFileHash = account.readSentryFile()
            }
            getHandler<SteamUser>()?.logOn(details)
        }
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        when (it.result) {
            EResult.OK -> {
                isLoggedIn = true
                getHandler<SteamNotifications>()?.requestOfflineMessageCount()

                // Set the client's "UI" mode to receive new callbacks.
                if (steamClient.isConnected)
                    getHandler<VapullaHandler>()?.setClientUIMode()
            }
            EResult.InvalidPassword -> account.loginKey = null
            else -> {
            }
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        steamClient.disconnect()
    }

    private val onNewLoginKey: Consumer<LoginKeyCallback> = Consumer {
        info("received login key")
        account.loginKey = it.loginKey
        account.uniqueId = it.uniqueID

        getHandler<SteamUser>()?.acceptNewLoginKey(it)
    }

    private val onUpdateMachineAuth: Consumer<UpdateMachineAuthCallback> = Consumer {
        info("received sentry file called ${it.fileName}")
        account.updateSentryFile(it)

        val otp = OTPDetails()
        otp.identifier = it.oneTimePassword.identifier
        otp.type = it.oneTimePassword.type

        val details = MachineAuthDetails()
        details.jobID = it.jobID
        details.fileName = it.fileName
        details.bytesWritten = it.bytesToWrite
        details.fileSize = account.sentrySize.toInt()
        details.offset = it.offset
        details.seteResult(EResult.OK)
        details.lastError = 0
        details.oneTimePassword = otp
        details.sentryFileHash = account.readSentryFile()

        getHandler<SteamUser>()?.sendMachineAuthResponse(details)
    }

    private val onPersonaState: Consumer<PersonaStatesCallback> = Consumer {
        it.personaStates.forEach { state ->
            if (!state.friendID.isIndividualAccount) {
                return@forEach
            }

            if (state.friendID == steamClient.steamID) {
                account.saveLocalUser(state)
                return@forEach
            }

            info("${state.state} - " +
                    "${state.name} - " +
                    "${state.lastLogOff.time} - " +
                    "${state.lastLogOn.time}"
            )

            stateBuffer.push(state)

            if (requestsToNotify.contains(state.friendID)) {
                postFriendRequestNotification(state)
                requestsToNotify.remove(state.friendID)
            }
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
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
                        currentFriend.relationship == EFriendRelationship.RequestRecipient) {
                    friend = SteamFriend(currentFriend.steamID.convertToUInt64())
                    friend.relation = currentFriend.relationship.code()
                    friendsToAdd.add(friend)
                }
            } else {
                if (currentFriend.relationship == EFriendRelationship.Friend ||
                        currentFriend.relationship == EFriendRelationship.RequestRecipient) {
                    friend.relation = currentFriend.relationship.code()
                    friendsToUpdate.add(friend)
                } else {
                    friendsToRemove.add(friend)
                }
            }

            if (inc && friend!!.relation == EFriendRelationship.RequestRecipient.code()) {
                requestsToNotify.add(currentFriend.steamID)
            }
        }

        dao.insert(*friendsToAdd.toTypedArray())
        dao.update(*friendsToUpdate.toTypedArray())
        dao.remove(*friendsToRemove.toTypedArray())
    }

    private val onFriendMsgHistory: Consumer<FriendMsgHistoryCallback> = Consumer { cb ->
        cb.messages.forEach {
            val fromLocal = cb.steamID != it.steamID
            val friendId = cb.steamID.convertToUInt64()
            val timestamp = it.timestamp.time
            val confirmedMessage =
                    db.chatMessageDao()
                            .find(it.message, timestamp, friendId, fromLocal, true)

            if (confirmedMessage != null) {
                return@forEach
            }

            val unconfirmedMessages =
                    db.chatMessageDao().find(it.message, friendId, fromLocal, false)
                            .sortedWith(kotlin.Comparator { o1, o2 ->
                                (abs(timestamp - o1.timestamp) -
                                        abs(timestamp - o2.timestamp)).toInt()
                            })

            if (unconfirmedMessages.isNotEmpty()) {
                unconfirmedMessages[0].timestamp = timestamp
                unconfirmedMessages[0].timestampConfirmed = true

                db.chatMessageDao().update(unconfirmedMessages[0])
            } else {
                db.chatMessageDao().insert(ChatMessage(
                        message = it.message,
                        timestamp = timestamp,
                        friendId = friendId,
                        fromLocal = fromLocal,
                        unread = it.isUnread,
                        timestampConfirmed = true
                ))
            }
        }
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        db.steamFriendDao().clearNicknames()

        it.nicknames.forEach { playerName ->
            val friend = db.steamFriendDao().find(playerName.steamID.convertToUInt64())

            if (friend != null) {
                friend.nickname = playerName.nickname
                db.steamFriendDao().update(friend)
            }
        }
    }

    private val onOfflineMessageNotification:
            Consumer<OfflineMessageNotificationCallback> = Consumer {
        if (it.messageCount > 0) {
            getHandler<SteamFriends>()?.requestOfflineMessages()
        }
    }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer { emoticon ->
        debug("onEmoticonList")

        val emoticons = emoticon.getEmoteList().map {
            if (it.isSticker) {
                Emoticon(it.name, it.isSticker)
            } else {
                Emoticon(it.name.substring(1, it.name.length - 1), it.isSticker)
            }
        }.toTypedArray()

        db.emoticonDao().delete()
        db.emoticonDao().insert(*emoticons)
    }

    private val onFriendMsgEcho: Consumer<FriendMsgEchoCallback> = Consumer {
        lastEcho = System.currentTimeMillis()

        db.chatMessageDao().insert(ChatMessage(
                message = it.message,
                timestamp = System.currentTimeMillis(),
                friendId = it.sender.convertToUInt64(),
                fromLocal = true,
                unread = false,
                timestampConfirmed = false
        ))
        db.chatMessageDao().markRead(it.sender.convertToUInt64())

        clearMessageNotifications(it.sender)
    }

    private val onServiceMethodResponse: Consumer<ServiceServiceMethodCallback> = Consumer { it ->
        info("onServiceMethodResponse")

        // TODO: Resume implementing this.
        if (it.jobName == "FriendMessages.SendMessage#1") {
            debug(it.modifiedMessage!!)
            debug(it.jobName!!)
            debug(it.timestamp.toString())
        }

        // TODO: Resume implementing this.
        if (it.jobName == "FriendMessages.GetRecentMessages#1") {
            debug(it.jobName!!)
            it.messageHistory!!.forEach {
                debug(it.toString())
            }
        }
    }

    // TODO
    private val onServiceMethod: Consumer<ServiceMethodCallback> = Consumer {
        info("onServiceMethod")

        // Incoming Message! [Chat Message or Typing]
        if (it.jobName == "FriendMessagesClient.IncomingMessage#1") {
            // Friend is typing
            if (it.entryType == EChatEntryType.Typing) {
                val friend = db.steamFriendDao().find(it.steamID.convertToUInt64())

                if (friend != null) {
                    friend.typingTs = System.currentTimeMillis()
                    db.steamFriendDao().update(friend)
                }
            }

            if (it.entryType == EChatEntryType.ChatMsg && it.message.isNotEmpty()) {
                db.chatMessageDao().insert(ChatMessage(
                        it.message,
                        System.currentTimeMillis(),
                        it.steamID.convertToUInt64(),
                        false,
                        chatFriendId != it.steamID.convertToUInt64(),
                        false
                ))

                if (it.steamID.convertToUInt64() != chatFriendId) {
                    postMessageNotification(it.steamID, it.message)
                }
            }
        }
    }

    //endregion
}
