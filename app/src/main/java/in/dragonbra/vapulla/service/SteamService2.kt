package `in`.dragonbra.vapulla.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import `in`.dragonbra.javasteam.handlers.ClientMsgHandler
import `in`.dragonbra.javasteam.steam.discovery.FileServerListProvider
import `in`.dragonbra.javasteam.steam.handlers.steamapps.SteamApps
import `in`.dragonbra.javasteam.steam.handlers.steamcloud.SteamCloud
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.PersonaState
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.*
import `in`.dragonbra.javasteam.steam.handlers.steamgamecoordinator.SteamGameCoordinator
import `in`.dragonbra.javasteam.steam.handlers.steamgameserver.SteamGameServer
import `in`.dragonbra.javasteam.steam.handlers.steammasterserver.SteamMasterServer
import `in`.dragonbra.javasteam.steam.handlers.steamnotifications.callback.OfflineMessageNotificationCallback
import `in`.dragonbra.javasteam.steam.handlers.steamscreenshots.SteamScreenshots
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodNotification
import `in`.dragonbra.javasteam.steam.handlers.steamunifiedmessages.callback.ServiceMethodResponse
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.SteamUser
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOffCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoginKeyCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.UpdateMachineAuthCallback
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.javasteam.steam.handlers.steamworkshop.SteamWorkshop
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.steam.steamclient.callbackmgr.CallbackManager
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.ConnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.callbacks.DisconnectedCallback
import `in`.dragonbra.javasteam.steam.steamclient.configuration.SteamConfiguration
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.compat.Consumer
import `in`.dragonbra.vapulla.MainActivity
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.ChatActivity
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.broadcastreceiver.*
import `in`.dragonbra.vapulla.data.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.steam.callback.EmoticonListCallback
import `in`.dragonbra.vapulla.steam.callback.RecentMessagesResponseCallback
import `in`.dragonbra.vapulla.util.Utils
import org.spongycastle.util.encoders.Hex
import timber.log.Timber
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private val remoteInput: RemoteInput =
    RemoteInput.Builder(ReplyReceiver.KEY_TEXT_REPLY)
        .setLabel("Reply")
        .build()

private val pendingIntentFlags: Int
    get() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

class SteamService2 : Service() {

    companion object {
        private const val NOTIFICATION_ID = 100
        private const val MAX_RETRY_COUNT = 5
        private const val SERVERS_FILE = "servers.bin"

        private var lastEcho = 0L
        private const val ECHO_BACKOFF = 5L * DateUtils.MINUTE_IN_MILLIS
        private const val NEW_MESSAGE_BACKOFF = DateUtils.MINUTE_IN_MILLIS
    }

    @Inject
    lateinit var db: VapullaDatabase

    @Inject
    lateinit var account: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var serviceNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var messageNotificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var requestNotificationBuilder: NotificationCompat.Builder

    /* Service */
    private val binder: SteamService2.SteamBinder = SteamBinder()

    /* Steam */
    private var expectDisconnect = false
    private var isLoggedIn: Boolean = false
    private var isRunning: Boolean = false
    private var retryCount: Int = 0
    private lateinit var callbackMgr: CallbackManager
    private val newMessages = mutableMapOf<SteamID, MutableList<MessagingStyle.Message>>()
    lateinit var steamClient: SteamClient
        private set

    override fun onCreate() {
        super.onCreate()

        Timber.d("Creating Steam service")

        val config = SteamConfiguration.create {
            it.withServerListProvider(FileServerListProvider(File(filesDir, SERVERS_FILE)))
        }

        steamClient = SteamClient(config).apply {
            removeHandler(SteamApps::class.java)
            removeHandler(SteamCloud::class.java)
            removeHandler(SteamGameCoordinator::class.java)
            removeHandler(SteamGameServer::class.java)
            removeHandler(SteamMasterServer::class.java)
            removeHandler(SteamScreenshots::class.java)
            removeHandler(SteamUserStats::class.java)
            removeHandler(SteamWorkshop::class.java)
        }

        callbackMgr = CallbackManager(steamClient).apply {
            subscribe(ConnectedCallback::class.java, onConnected)
            subscribe(DisconnectedCallback::class.java, onDisconnected)
            subscribe(EmoticonListCallback::class.java, onEmoticonList)
            subscribe(FriendMsgEchoCallback::class.java, onFriendMsgEcho)
            subscribe(FriendMsgHistoryCallback::class.java, onFriendMsgHistory)
            subscribe(FriendsListCallback::class.java, onFriendsList)
            subscribe(LoggedOffCallback::class.java, onLoggedOff)
            subscribe(LoggedOnCallback::class.java, onLoggedOn)
            subscribe(LoginKeyCallback::class.java, onNewLoginKey)
            subscribe(NicknameListCallback::class.java, onNicknameList)
            subscribe(OfflineMessageNotificationCallback::class.java, onOfflineMessageNotification)
            subscribe(PersonaStatesCallback::class.java, onPersonaStates)
            subscribe(RecentMessagesResponseCallback::class.java, onRecentMessagesCallback)
            subscribe(ServiceMethodNotification::class.java, onServiceMethodNotification)
            subscribe(ServiceMethodResponse::class.java, onServiceMethodResponse)
            subscribe(UpdateMachineAuthCallback::class.java, onUpdateMachineAuth)
        }
    }

    private val steamThread: Runnable = Runnable {
        Timber.i("Connecting to Steam...")

        isRunning = true
        steamClient.connect()

        while (isRunning) {
            callbackMgr.runWaitCallbacks(1000)
        }

        Timber.i("Steam thread stopped")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Pending Intents parsed here

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Destroying Service")
        disconnect()

    }

    override fun onBind(intent: Intent?): IBinder {
        Timber.d("Binding Service")
        return binder
    }

    fun connect() {
        if (!isRunning) {
            expectDisconnect = false
            retryCount = 0
            Thread(steamThread, "Steam Thread").start()
            // TODO display notification
        }
    }

    fun disconnect() {
        expectDisconnect = true
        steamClient.disconnect()
    }

    fun logOn(loginDetails: LogOnDetails) {
        if (isLoggedIn) {
            Timber.w("Tried to log in while already logged in")
            return
        }

        loginDetails.isShouldRememberPassword = true

        if (account.hasSentryFile()) {
            loginDetails.sentryFileHash = account.readSentryFile()
        }

        getHandler<SteamUser>().logOn(loginDetails)
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createMessagesNotificationChannel()
            createRequestNotificationChannel()
            createServiceNotificationChannel()
        }
        startForeground(NOTIFICATION_ID, serviceNotificationBuilder.build())
    }

    // region [REGION] Notifications
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createServiceNotificationChannel() {
        val notificationChannel = NotificationChannel(
            "vapulla-service",
            "Vapulla Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createRequestNotificationChannel() {
        val notificationChannel = NotificationChannel(
            "vapulla-friend-request",
            "Vapulla Friend Requests",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMessagesNotificationChannel() {
        val notificationChannel = NotificationChannel(
            "vapulla-messages",
            "Vapulla Chat Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun postServiceNotification(@StringRes string: Int) {
        val text = getString(string)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(this, LogOutReceiver::class.java),
            if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
        )

        serviceNotificationBuilder.apply {
            setContentIntent(contentIntent)
            setContentText(text)
            addAction(
                R.drawable.ic_exit_to_app,
                getString(R.string.notificationActionLogOut),
                pendingIntent
            )
        }

        notificationManager.notify(NOTIFICATION_ID, serviceNotificationBuilder.build())
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

        val currentTs = System.currentTimeMillis()
        val backoff = messages.isNotEmpty() &&
            currentTs < messages[messages.size - 1].timestamp + NEW_MESSAGE_BACKOFF

        val bitmap: Bitmap = Glide.with(applicationContext)
            .asBitmap()
            .load(Utils.getAvatarUrl(friend.avatar))
            .apply(Utils.avatarOptions)
            .submit()
            .get(5, TimeUnit.SECONDS)

        val iconBitmap = IconCompat.createWithBitmap(bitmap)
        val steamUser = Person
            .Builder()
            .setName(friend.name ?: "")
            .setIcon(iconBitmap)
            .build()

        val newMessage = MessagingStyle.Message(message, currentTs, steamUser)
        messages.add(newMessage)

        val style = MessagingStyle(steamUser)

        messages.forEach {
            style.addMessage(it)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            friendId.convertToUInt64().toInt(),
            Intent(this, ReplyReceiver::class.java).apply {
                putExtra(ReplyReceiver.EXTRA_ID, friendId.convertToUInt64())
            },
            pendingIntentFlags
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

        messageNotificationBuilder.apply {
            addAction(replyAction)
            setContentIntent(pendingIntent)
            setLargeIcon(bitmap)
            setOnlyAlertOnce(backoff)
            setStyle(style)
        }

        val steamId = friendId.convertToUInt64().toInt()
        notificationManager.notify(steamId, messageNotificationBuilder.build())
    }

    private fun postRequestNotification(state: PersonaState) {
        val steamId = state.friendID.convertToUInt64().toInt()

        val bitmap = Glide.with(applicationContext)
            .asBitmap()
            .load(Utils.getAvatarUrl(Hex.toHexString(state.avatarHash)))
            .apply(Utils.avatarOptions)
            .submit()
            .get(5, TimeUnit.SECONDS)

        val acceptReceiver = Intent(this, AcceptRequestReceiver::class.java).apply {
            putExtra(AcceptRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            steamId,
            acceptReceiver,
            pendingIntentFlags
        )

        val ignoreReceiver = Intent(this, IgnoreRequestReceiver::class.java).apply {
            putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            steamId,
            ignoreReceiver,
            pendingIntentFlags
        )

        val blockPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            steamId,
            Intent(this, BlockRequestReceiver::class.java).apply {
                putExtra(IgnoreRequestReceiver.EXTRA_ID, state.friendID.convertToUInt64())
            },
            pendingIntentFlags
        )

        requestNotificationBuilder.apply {
            setContentText(getString(R.string.notificationMessageFriendRequest, state.name))
            setContentTitle(getString(R.string.notificationTitleFriendRequest))
            setLargeIcon(bitmap)
            setContentIntent(
                PendingIntent.getActivity(
                    this@SteamService2,
                    0,
                    Intent(this@SteamService2, HomeActivity::class.java),
                    if (Utils.isGreaterThanM) PendingIntent.FLAG_IMMUTABLE else 0
                )
            )
            addAction(
                R.drawable.ic_check,
                getString(R.string.notificationActionAccept),
                acceptPendingIntent
            )
            addAction(
                R.drawable.ic_close,
                getString(R.string.notificationActionIgnore),
                ignorePendingIntent
            )
            addAction(
                R.drawable.ic_block,
                getString(R.string.notificationActionBlock),
                blockPendingIntent
            )
        }

        notificationManager.notify(steamId, requestNotificationBuilder.build())
    }

    // endregion

    // region [REGION] Callback Handlers
    private val onConnected = Consumer<ConnectedCallback> {}
    private val onDisconnected = Consumer<DisconnectedCallback> {}
    private val onEmoticonList = Consumer<EmoticonListCallback> {}
    private val onFriendMsgEcho = Consumer<FriendMsgEchoCallback> {}
    private val onFriendMsgHistory = Consumer<FriendMsgHistoryCallback> {}
    private val onFriendsList = Consumer<FriendsListCallback> {}
    private val onLoggedOff = Consumer<LoggedOffCallback> {}
    private val onLoggedOn = Consumer<LoggedOnCallback> {}
    private val onNewLoginKey = Consumer<LoginKeyCallback> {}
    private val onNicknameList = Consumer<NicknameListCallback> {}
    private val onOfflineMessageNotification = Consumer<OfflineMessageNotificationCallback> {}
    private val onPersonaStates = Consumer<PersonaStatesCallback> {}
    private val onRecentMessagesCallback = Consumer<RecentMessagesResponseCallback> {}
    private val onServiceMethodNotification = Consumer<ServiceMethodNotification> {}
    private val onServiceMethodResponse = Consumer<ServiceMethodResponse> {}
    private val onUpdateMachineAuth = Consumer<UpdateMachineAuthCallback> {}
    // endregion

    private inline fun <reified T : ClientMsgHandler> getHandler(): T {
        return steamClient.getHandler(T::class.java)
    }

    inner class SteamBinder : Binder() {
        fun getService(): SteamService2 = this@SteamService2
    }
}