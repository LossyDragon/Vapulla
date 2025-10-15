package `in`.dragonbra.vapulla.service

import `in`.dragonbra.javasteam.enums.EChatEntryType
import `in`.dragonbra.javasteam.enums.EFriendRelationship
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.SteamNotifications
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
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
import `in`.dragonbra.vapulla.threading.runOnBackgroundThread
import `in`.dragonbra.vapulla.util.PersonaStateBuffer
import `in`.dragonbra.vapulla.util.Utils
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.Glide
import `in`.dragonbra.javasteam.steam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackMsg
import `in`.dragonbra.vapulla.util.AnkoLogger
import `in`.dragonbra.vapulla.util.NotificationHelper
import `in`.dragonbra.vapulla.util.info
import `in`.dragonbra.vapulla.util.intentFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.scope.serviceScope
import org.spongycastle.util.encoders.Hex
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SteamService : Service(), AnkoLogger {

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

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var account: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private val serviceManager: ServiceManager by inject()

    private val scope = CoroutineScope(
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("SteamService")
    )

    // region JavaSteam
    lateinit var steamClient: SteamClient
    lateinit var callbackMgr: CallbackManager
    private val subscriptions: MutableSet<Closeable?> = mutableSetOf()
    val disconnectedSubs: MutableSet<(DisconnectedCallback) -> Unit> = mutableSetOf()

    // Android Service
    private val binder: SteamBinder = SteamBinder()

    private val newMessages: MutableMap<SteamID, MutableList<NotificationCompat.MessagingStyle.Message>> =
        mutableMapOf()

    private val requestsToNotify: MutableSet<SteamID> = mutableSetOf()

    private val handlerThread = HandlerThread("SteamService Handler")

    private lateinit var handler: Handler

    private lateinit var stateBuffer: PersonaStateBuffer

    private lateinit var steamThreadJob: Job

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
        vapulla().graph.inject(this) // TODO remove
        super.onCreate()

        Timber.i("onCreate")

        handlerThread.start()
        handler = Handler(handlerThread.looper)

        stateBuffer = PersonaStateBuffer(db.steamFriendDao())

        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(File(filesDir, SERVERS_FILE)))
        }
        steamClient = SteamClient(config)
        steamClient.addHandler<VapullaHandler>()

        steamClient.removeHandler<SteamApps>()
        steamClient.removeHandler<SteamCloud>()
        steamClient.removeHandler<SteamGameCoordinator>()
        steamClient.removeHandler<SteamGameServer>()
        steamClient.removeHandler<SteamMasterServer>()
        steamClient.removeHandler<SteamScreenshots>()
        steamClient.removeHandler<SteamUserStats>()
        steamClient.removeHandler<SteamWorkshop>()

        callbackMgr = CallbackManager(steamClient)

        with(subscriptions) {
            with(callbackMgr) {
                add(subscribe<DisconnectedCallback> { onDisconnected })
                add(subscribe<ConnectedCallback> { onConnected })
                add(subscribe<LoggedOnCallback> { onLoggedOn })
                add(subscribe<LoggedOffCallback> { onLoggedOff })
                add(subscribe<PersonaStateCallback> { onPersonaState })
                add(subscribe<FriendsListCallback> { onFriendsList })
                add(subscribe<FriendMsgHistoryCallback> { onFriendMsgHistory })
                add(subscribe<FriendMsgCallback> { onFriendMsg })
                add(subscribe<NicknameListCallback> { onNicknameList })
                add(subscribe<OfflineMessageNotificationCallback> { onOfflineMessages })
                add(subscribe<EmoticonListCallback> { onEmoticonList })
                add(subscribe<FriendMsgEchoCallback> { onFriendMsgEcho })
            }
        }

        remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        scope.launch {
            serviceManager.commandChannel.collect { command ->
                when (command) {
                    is ServiceCommand.Start -> handleStart()
                    is ServiceCommand.Stop -> handleStop()
                    is ServiceCommand.Login -> handleStop()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        info("onBind")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("onStartCommand")

        val notification = NotificationHelper.createServiceNotification(
            this,
            this.getString(R.string.app_name),
            "Starting..."
        )
        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        serviceManager.setServiceRunning(true)

        val data = intent?.getStringExtra("extra_data")
        Timber.d("Received data: $data") // TODO re-add notification intents.

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")

        disconnect()

        handlerThread.quit()

        steamThreadJob.cancel(CancellationException("Service Destroyed"))

        sendBroadcast(Intent(VapullaBaseActivity.STOP_INTENT))

        serviceManager.setServiceRunning(false)
        serviceManager.setLoading(false)

        scope.cancel(CancellationException("Service Destroyed"))
    }

    private fun handleStart() {
        Timber.d("handleStart() - Starting work")
        serviceManager.setLoading(true)

        // Do your work here, the service will live indefinitely
    }

    private fun handleStop() {
        Timber.d("handleStop() - Stopping service")
        serviceManager.setLoading(false)
        stopSelf()
    }

    // -----------------

    private fun setNotification(text: String) {
        val logOutIntent = intentFor<LogOutReceiver>()
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, logOutIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "vapulla-service")
            .setPriority(NotificationManager.IMPORTANCE_LOW)
            .setDefaults(0)
            .setShowWhen(false)
            .setContentTitle("Vapulla")
            .setContentText(text)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intentFor<HomeActivity>(),
                    PendingIntent.FLAG_IMMUTABLE
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

        startForeground(ONGOING_NOTIFICATION_ID, builder.build())
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            stateBuffer.start()

            steamThreadJob = scope.launch(CoroutineName("Steam Thread")) {
                info("Connecting to steam...")

                isRunning = true
                steamClient.connect()

                while (isRunning) {
                    callbackMgr.runWaitCallbackAsync()
                }

                info("Steam thread stopped")
            }

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
        details.shouldRememberPassword = true
        steamClient.getHandler<SteamUser>()?.logOn(details)
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
        val backoff =
            !messages.isEmpty() && currentTs < messages[messages.size - 1].timestamp + NEW_MESSAGE_BACKOFF

        val newMessage = NotificationCompat.MessagingStyle.Message(message, currentTs, friend.name)
        messages.add(newMessage)

        val style = NotificationCompat.MessagingStyle(friend.name ?: "")

        messages.forEach {
            style.addMessage(it)
        }

        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                .asBitmap()
                .load(Utils.getAvatarUrl(friend.avatar))
                .apply(Utils.avatarOptions)
                .submit()
                .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
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

        val intent =
            intentFor<ChatActivity>(ChatActivity.INTENT_STEAM_ID to friendId.convertToUInt64())
        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(friendId.convertToUInt64().toInt(), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, "vapulla-message")
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setStyle(style)
            .setSmallIcon(R.drawable.ic_message)
            .setLargeIcon(bitmap)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setOnlyAlertOnce(backoff)
            .addAction(replyAction)
            .build()

        notificationManager.notify(friendId.convertToUInt64().toInt(), notification)

        if (isActivityRunning) {
            // TODO delayed notification remove?
            //removeNotifications()
        }
    }

    private fun postFriendRequestNotification(state: PersonaStateCallback) {
        var bitmap: Bitmap? = null

        try {
            bitmap = Glide.with(applicationContext)
                .asBitmap()
                .load(Utils.getAvatarUrl(Hex.toHexString(state.avatarHash)))
                .apply(Utils.avatarOptions)
                .submit()
                .get(5, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            state.friendID.convertToUInt64().toInt(),
            intentFor<AcceptRequestReceiver>(
                AcceptRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
            ),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val ignorePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            state.friendID.convertToUInt64().toInt(),
            intentFor<IgnoreRequestReceiver>(
                IgnoreRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
            ),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            state.friendID.convertToUInt64().toInt(),
            intentFor<BlockRequestReceiver>(
                IgnoreRequestReceiver.EXTRA_ID to state.friendID.convertToUInt64()
            ),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "vapulla-friend-request")
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_add_friend)
            .setLargeIcon(bitmap)
            .setContentText(getString(R.string.notificationMessageFriendRequest, state.name))
            .setContentTitle(getString(R.string.notificationTitleFriendRequest))
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, intentFor<HomeActivity>(),
                    PendingIntent.FLAG_IMMUTABLE
                )
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
        intentFor<ReplyReceiver>(ReplyReceiver.EXTRA_ID to id)

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

    fun sendMessage(id: SteamID, message: String?, emoteSet: Set<String>) {
        val trimmed = message?.trim()

        if (Strings.isNullOrEmpty(trimmed)) {
            return
        }

        val emoteMessage = Utils.findEmotes(trimmed!!.replace('\u02D0', ':'), emoteSet)

        steamClient.getHandler<SteamFriends>()?.sendChatMessage(id, EChatEntryType.ChatMsg, message)

        db.chatMessageDao().insert(
            ChatMessage(
                emoteMessage,
                System.currentTimeMillis(),
                id.convertToUInt64(),
                true,
                false,
                false
            )
        )

        clearMessageNotifications(id)
    }

    inline fun <reified T : CallbackMsg> subscribe(noinline callbackFunc: (T) -> Unit): Closeable? =
        when (T::class) {
            DisconnectedCallback::class -> {
                @Suppress("UNCHECKED_CAST")
                disconnectedSubs.add(callbackFunc as (DisconnectedCallback) -> Unit)
                Closeable {
                    disconnectedSubs.remove(callbackFunc)
                }
            }

            else -> callbackMgr.subscribe(T::class.java, { callbackFunc(it) })
        }

    // TODO classic android memory leak :-)
    inner class SteamBinder : Binder() {
        fun getService(): SteamService = this@SteamService
    }

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
            val details = LogOnDetails().apply {
                username = account.username!!
                accessToken = account.loginKey
                shouldRememberPassword = true
            }
            steamClient.getHandler<SteamUser>()?.logOn(details)
        }
    }

    private val onLoggedOn: Consumer<LoggedOnCallback> = Consumer {
        when (it.result) {
            EResult.OK -> {
                isLoggedIn = true
                steamClient.getHandler<SteamNotifications>()?.requestOfflineMessageCount()
            }

            EResult.InvalidPassword -> account.loginKey = null
            else -> {
            }
        }
    }

    private val onLoggedOff: Consumer<LoggedOffCallback> = Consumer {
        steamClient.disconnect()
    }

    private val onPersonaState: Consumer<PersonaStateCallback> = Consumer {
        if (!it.friendID.isIndividualAccount) {
            return@Consumer
        }

        if (it.friendID == steamClient.steamID) {
            account.saveLocalUser(it)
            return@Consumer
        }

        info("${it.state} - ${it.name} - ${it.lastLogOff.time} - ${it.lastLogOn.time}")

        stateBuffer.push(it)

        if (requestsToNotify.contains(it.friendID)) {
            postFriendRequestNotification(it)
            requestsToNotify.remove(it.friendID)
        }
    }

    private val onFriendsList: Consumer<FriendsListCallback> = Consumer {
        val dao = db.steamFriendDao()
        val inc = it.isIncremental

        val friendsToAdd: MutableList<SteamFriend> = LinkedList()
        val friendsToUpdate: MutableList<SteamFriend> = LinkedList()
        val friendsToRemove: MutableList<SteamFriend> = LinkedList()
        it.friendList.forEach {
            if (!it.steamID.isIndividualAccount) {
                return@forEach
            }

            var friend = dao.find(it.steamID.convertToUInt64())

            if (friend == null) {
                if (it.relationship == EFriendRelationship.Friend || it.relationship == EFriendRelationship.RequestRecipient) {
                    friend = SteamFriend(it.steamID.convertToUInt64())
                    friend.relation = it.relationship.code()
                    friendsToAdd.add(friend)
                }
            } else {
                if (it.relationship == EFriendRelationship.Friend || it.relationship == EFriendRelationship.RequestRecipient) {
                    friend.relation = it.relationship.code()
                    friendsToUpdate.add(friend)
                } else {
                    friendsToRemove.add(friend)
                }
            }

            if (inc && friend!!.relation == EFriendRelationship.RequestRecipient.code()) {
                requestsToNotify.add(it.steamID)
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
                db.chatMessageDao().find(it.message, timestamp, friendId, fromLocal, true)

            if (confirmedMessage != null) {
                return@forEach
            }

            val unconfirmedMessages =
                db.chatMessageDao().find(it.message, friendId, fromLocal, false)
                    .sortedWith(kotlin.Comparator { o1, o2 ->
                        (Math.abs(timestamp - o1.timestamp) - Math.abs(timestamp - o2.timestamp)).toInt()
                    })

            if (unconfirmedMessages.isNotEmpty()) {
                unconfirmedMessages[0].timestamp = timestamp
                unconfirmedMessages[0].timestampConfirmed = true

                db.chatMessageDao().update(unconfirmedMessages[0])
            } else {
                db.chatMessageDao().insert(
                    ChatMessage(
                        message = it.message,
                        timestamp = timestamp,
                        friendId = friendId,
                        fromLocal = fromLocal,
                        unread = it.unread,
                        timestampConfirmed = true
                    )
                )
            }
        }
    }

    private val onFriendMsg: Consumer<FriendMsgCallback> = Consumer {
        when (it.entryType) {
            EChatEntryType.ChatMsg -> {
                db.chatMessageDao().insert(
                    ChatMessage(
                        message = it.message.orEmpty(),
                        timestamp = System.currentTimeMillis(),
                        friendId = it.sender.convertToUInt64(),
                        fromLocal = false,
                        unread = chatFriendId != it.sender.convertToUInt64(),
                        timestampConfirmed = false
                    )
                )

                if (it.sender.convertToUInt64() != chatFriendId) {
                    postMessageNotification(it.sender, it.message.orEmpty())
                }
            }

            EChatEntryType.Typing -> {
                val friend = db.steamFriendDao().find(it.sender.convertToUInt64())

                if (friend != null) {
                    friend.typingTs = System.currentTimeMillis()
                    db.steamFriendDao().update(friend)
                }
            }

            else -> {
            }
        }
    }

    private val onNicknameList: Consumer<NicknameListCallback> = Consumer {
        db.steamFriendDao().clearNicknames()

        it.nicknames.forEach {
            val friend = db.steamFriendDao().find(it.steamID.convertToUInt64())

            if (friend != null) {
                friend.nickname = it.nickname
                db.steamFriendDao().update(friend)
            }
        }
    }

    private val onOfflineMessages: Consumer<OfflineMessageNotificationCallback> =
        Consumer {
            if (it.messageCount > 0) {
                steamClient.getHandler<SteamFriends>()?.requestOfflineMessages()
            }
        }

    private val onEmoticonList: Consumer<EmoticonListCallback> = Consumer {
        val emoticons =
            it.emoticons.map { Emoticon(it.name.substring(1, it.name.length - 1)) }.toTypedArray()

        db.emoticonDao().delete()
        db.emoticonDao().insert(*emoticons)
    }

    private val onFriendMsgEcho: Consumer<FriendMsgEchoCallback> = Consumer {
        lastEcho = System.currentTimeMillis()

        db.chatMessageDao().insert(
            ChatMessage(
                message = it.message.orEmpty(),
                timestamp = System.currentTimeMillis(),
                friendId = it.recipient.convertToUInt64(),
                fromLocal = true,
                unread = false,
                timestampConfirmed = false
            )
        )
        db.chatMessageDao().markRead(it.recipient.convertToUInt64())

        clearMessageNotifications(it.recipient)
    }

    //endregion
}
