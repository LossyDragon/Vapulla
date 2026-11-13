package `in`.dragonbra.vapulla.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import `in`.dragonbra.vapulla.db.VapullaDatabase
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.api.SteamApis
import `in`.dragonbra.vapulla.service.auth.SteamAuthenticator
import `in`.dragonbra.vapulla.service.download.DownloadManager
import `in`.dragonbra.vapulla.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Foreground service that keeps the Steam session alive for the whole app.
 *
 * This class is the Android side only: lifecycle, the foreground notification, and the
 * notification actions sent through [actionIntent]. The Steam work is split by role:
 * - [SteamConnection] owns the JavaSteam client: connect, log on, reconnect, tear down.
 * - [SteamAuthenticator] ([auth]) runs the credential and QR sign-in flows.
 * - [SteamApis] ([apis]) holds one small class per feature the screens call.
 * - The `sync` package mirrors what Steam pushes (friends, licenses, PICS app info) into
 *   the Room database, which the UI observes.
 *
 * Screens reach the service through [ServiceConnection].
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

        private const val STATUS_STARTING = "Starting..."

        val isLoading: StateFlow<Boolean> = SteamState.isLoading.asStateFlow()
        val isRunning: StateFlow<Boolean> = SteamState.isRunning.asStateFlow()
        val isLoggedIn: StateFlow<Boolean> = SteamState.isLoggedIn.asStateFlow()
        val loginResult: SharedFlow<LoginResult> = SteamState.loginResult.asSharedFlow()

        /**
         * Why Steam ended the last session, if it did and nobody has been told yet. Reading
         * it clears it, so the reason is shown once.
         */
        fun takeSignOutReason(): String? = SteamState.signOutReason.getAndUpdate { null }
    }

    // Coroutines. A failed job is logged instead of taking the whole process down.
    private val scope = CoroutineScope(
        context = Dispatchers.IO + SupervisorJob() + CoroutineName("SteamService") +
            CoroutineExceptionHandler { _, error -> Timber.e(error, "Uncaught service error") },
    )

    // Koin DI Injection
    private val db: VapullaDatabase by inject()
    private val account: AccountManager by inject()
    private val downloads: DownloadManager by inject()

    // Lazy because the application context is not available before onCreate.
    private val connection: SteamConnection by lazy {
        SteamConnection(
            context = applicationContext,
            scope = scope,
            db = db,
            account = account,
            onStatus = ::updateStatus,
            onShutDown = ::onConnectionShutDown,
            downloads = downloads,
        )
    }

    /** The sign-in flows. */
    val auth: SteamAuthenticator by lazy { SteamAuthenticator(scope, account, connection) }

    /** The feature APIs; available while connected. */
    val apis: SteamApis? get() = connection.apis

    /** Text of the foreground notification, kept so a new start command does not reset it. */
    private var statusText = STATUS_STARTING

    private var binder: Binder? = ServiceBinder()

    inner class ServiceBinder : Binder() {
        val service: SteamService = this@SteamService
    }

    override fun onBind(intent: Intent): IBinder? = binder

    override fun onCreate() {
        super.onCreate()

        Timber.i("onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("onStartCommand")

        // Re-post the current status rather than a fixed text: this runs for every
        // notification action too, not just the first start.
        val notification = NotificationHelper.createServiceNotification(
            context = this,
            text = statusText,
        )

        startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)

        // The service is wanted again (the app was opened, or a sign-in started), so the
        // notice left by a lost session is out of date.
        NotificationHelper.cancelSignedOutNotification(context = this)

        intent?.getStringExtra(EXTRA_ACTION)?.let { action -> handleAction(action, intent) }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")

        // Cancel any ongoing auth operations
        auth.cancel()

        // Clean shutdown
        connection.close()

        // Cancel coroutines
        scope.cancel(CancellationException("Service Destroyed"))
    }

    /**
     * Signs off and stops the service. With [clearCredentials] the saved sign-in is forgotten
     * too, so the login screen does not sign straight back in.
     */
    fun logOut(clearCredentials: Boolean = true) {
        connection.logOut(clearCredentials)
    }

    /** Runs a notification action sent through [actionIntent]. */
    private fun handleAction(action: String, intent: Intent) {
        val friendId = intent.getLongExtra(EXTRA_ID, 0L)
        Timber.d("handleAction($action) for $friendId")

        when (action) {
            // Works whether or not Steam is reachable, and keeps the saved sign-in.
            ACTION_STOP -> logOut(clearCredentials = false)

            ACTION_REPLY -> {
                val message = intent.getCharSequenceExtra(EXTRA_MESSAGE)?.toString().orEmpty()
                scope.launch {
                    val chat = apis?.chat
                    if (chat?.send(friendId, message) != true) {
                        Timber.w("Reply to $friendId was not sent")
                    }
                    // Replying means the conversation was read; this also clears the
                    // notification, which otherwise keeps showing its "sending" state.
                    if (chat != null) {
                        chat.ack(friendId)
                    } else {
                        db.chatMessageDao().markMessagesAsRead(friendId)
                        NotificationHelper
                            .cancelFriendMessageNotification(this@SteamService, friendId)
                    }
                }
            }

            else -> {
                val friends = apis?.friends ?: run {
                    Timber.w("Ignoring action '$action': not connected")
                    return
                }

                when (action) {
                    ACTION_ACCEPT_REQUEST -> friends.acceptRequest(friendId)
                    ACTION_IGNORE_REQUEST -> friends.declineRequest(friendId)
                    ACTION_BLOCK_REQUEST -> friends.blockRequest(friendId)
                    else -> Timber.w("Unknown action '$action'")
                }
            }
        }
    }

    /** Sets the foreground notification text. */
    private fun updateStatus(text: String) {
        statusText = text
        NotificationHelper.updateServiceNotification(context = this, text = text)
    }

    /**
     * The connection ended for good: stop any sign-in attempt and the service itself. When a
     * signed-in session was lost ([sessionLost]) a "Signed out" notification is left behind,
     * so the user is not left guessing why messages stopped arriving.
     */
    private fun onConnectionShutDown(sessionLost: Boolean) {
        auth.cancel()

        statusText = STATUS_STARTING
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (sessionLost) {
            NotificationHelper.showSignedOutNotification(context = this)
        }
        stopSelf()
    }
}
