package `in`.dragonbra.vapulla.compose.screens.login

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.OnChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.javasteam.steam.authentication.SteamAuthentication
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.LogOnDetails
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.home.HomeActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.getErrorMessage
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.service.Notifications
import java.lang.IllegalArgumentException
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO: Using the mobile steam app, we cannot view this Authorized Device info.
//  ie: we cannot sign out of it remotely
//  displays: "something went wrong loading this page" error.

@AndroidEntryPoint
class LoginActivity : VapullaBaseActivity(), IAuthenticator, OnChallengeUrlChanged {

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private val viewModel: LoginViewModel by viewModels()

    private val loginScope = CoroutineScope(Dispatchers.IO + Job())

    private suspend fun qrLogin(coroutineScope: CoroutineScope): Pair<String, String> {
        val steamClient = steamService?.steamClient
        val unifiedMessages = steamService?.unifiedMessages

        val auth = SteamAuthentication(steamClient!!, unifiedMessages!!)

        val authSessionDetails = AuthSessionDetails().apply {
            deviceFriendlyName = "Vapulla - Android"
            persistentSession = true
        }

        val authSession: QrAuthSession = auth.beginAuthSessionViaQR(authSessionDetails)

        authSession.challengeUrlChanged = this@LoginActivity

        viewModel.drawQRCode(authSession)

        val pollResponse = authSession.pollingWaitForResult(coroutineScope)

        Timber.i("Connected to Steam! Logging in " + pollResponse.accountName + "...")

        return Pair(pollResponse.accountName, pollResponse.refreshToken)
    }

    private suspend fun accountLogin(coroutineScope: CoroutineScope): Pair<String, String>? {
        val accountUsername = viewModel.accountManager.username
        val accountRefreshToken = viewModel.accountManager.loginKey
        if (accountUsername.isNullOrEmpty() && accountRefreshToken.isNullOrEmpty()) {
            val authSessionDetails = AuthSessionDetails().apply {
                username = viewModel.loginState.value.username.trim()
                password = viewModel.loginState.value.password
                persistentSession = true
                authenticator = this@LoginActivity
            }
            val steamClient = steamService!!.steamClient
            val unifiedMessages = steamService!!.unifiedMessages
            val auth = SteamAuthentication(steamClient, unifiedMessages)
            return try {
                val authSession = auth.beginAuthSessionViaCredentials(authSessionDetails)

                val authPollResult = authSession.pollingWaitForResult(coroutineScope)

                // Save our results (username and refresh token) to account manager.
                Pair(authPollResult.accountName, authPollResult.refreshToken)
            } catch (e: IllegalArgumentException) {
                Timber.e("WOAH!", e)
                null
            }
        }

        return Pair(accountUsername!!, accountRefreshToken!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        viewModel.prefillInputs()

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !viewModel.loginState.value.isServiceConnected
            }
        }

        setContent {
            VapullaTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onPermissionsGranted = {},
                    onCancelService = ::onServiceCancel,
                    onStartService = ::startSteamService,
                    onSettings = ::onSettings
                )
            }
        }
    }

    private fun onServiceCancel() {
        val cancellationException = CancellationException("Cancel button clicked")
        loginScope.cancel(cancellationException)

        steamService?.disconnect()
    }

    override fun onResume() {
        super.onResume()
        if (steamService?.isLoggedIn == true) {
            onLoginSuccess()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onConnected() {
        if (steamService?.isLoggedIn == true) {
            onLoginSuccess()
            return
        }

        viewModel.onLoadingVisible(true)

        try {
            // TODO should 'really' let the service handle this
            // TODO scope not re-usable! 😱
            // TODO What happens after CM kick after ~60 sec?
            loginScope.launch {
                val deferredLogin = async {
                    if (viewModel.loginState.value.isSigningInViaQR) {
                        qrLogin(this)
                    } else {
                        accountLogin(this)
                    }
                }

                // We wait patiently until completion or cancel.
                val response = deferredLogin.await()

                if (response == null) {
                    Timber.w("Login response was null")
                    viewModel.showFailedScreen("Login response received no data")
                    return@launch
                }

                viewModel.accountManager.username = response.first
                viewModel.accountManager.loginKey = response.second

                val logonDetails = LogOnDetails().apply {
                    username = viewModel.accountManager.username
                    accessToken = viewModel.accountManager.loginKey
                    loginID = 149
                }

                steamService?.logOn(logonDetails)
            }
        } catch (e: Exception) {
            Timber.w("Aye yo wth")
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Timber.d("onDisconnected")
        with(viewModel.loginState.value) {
            if (!expectSteamGuard) {
                if (refreshToken.isNotEmpty()) {
                    viewModel.showFailedScreen("Failed to connect to steam!")
                }
            }
        }
    }

    override fun onLoggedOn(callback: LoggedOnCallback) {
        super.onLoggedOn(callback)
        Timber.d("WERE LOGGED ON! ${callback.result}")
        if (callback.result != EResult.OK) {
            val errorMessage = getErrorMessage(callback.result, callback.extendedResult)

            Timber.w("Unable to logon to Steam: ${callback.result} / ${callback.extendedResult}")

            viewModel.onShowMessage(errorMessage)

            steamService?.disconnect()
            return
        }

        scope.launch(Dispatchers.IO) {
            val friends: SteamFriends? = getHandler()
            friends?.setPersonaState(EPersonaState.Online)
        }

        onLoginSuccess()
    }

    override fun onLoginSuccess() {
        super.onLoginSuccess()
        Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also {
            startActivity(it)
        }
        finish()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)

        // Create our notification channels when the service is bound.
        if (Constants.isAtLeastO) {
            Notifications.createMessagesNotificationChannel(notificationManager)
            Notifications.createRequestNotificationChannel(notificationManager)
            Notifications.createServiceNotificationChannel(notificationManager)
        }

        viewModel.onServiceBoundVerifyLoginDetails {
            startSteamService()
        }

        viewModel.onServiceBound()
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.d("Unbound from Steam service")
    }

    private fun onSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        ).also(::startActivity)
    }

    override fun acceptDeviceConfirmation(): CompletableFuture<Boolean> {
        Timber.i("STEAM GUARD! Use the Steam Mobile App to confirm your sign in...")
        viewModel.onShowSteamGuard(
            expectSteamGuard = true,
            useAppSignIn = true,
            error = "Use the Steam Mobile App to confirm your sign in..."
        )
        return CompletableFuture.completedFuture(true)
    }

    override fun getDeviceCode(previousCodeWasIncorrect: Boolean): CompletableFuture<String> {
        Timber.i("Steam Guard, use code on app")
        viewModel.onShowSteamGuard(
            expectSteamGuard = true,
            error = "Please enter your 2-factor auth code from your authenticator app."
        )

        if (previousCodeWasIncorrect) {
            Timber.i("The previous 2-factor auth code you have provided is incorrect.")
            viewModel.onShowMessage(
                "The previous 2-factor auth code you have provided is incorrect."
            )
        }

        val code = viewModel.twoFactorFuture.get()
        return CompletableFuture.completedFuture(code)
    }

    override fun getEmailCode(
        email: String?,
        previousCodeWasIncorrect: Boolean
    ): CompletableFuture<String> {
        Timber.i("Steam Guard, use code sent to $email.")
        viewModel.onShowSteamGuard(
            expectSteamGuard = true,
            error = "Please enter the auth code sent to the email at $email."
        )

        if (previousCodeWasIncorrect) {
            Timber.i("Previous code was incorrect")
            viewModel.onShowMessage(
                "The previous 2-factor auth code you have provided is incorrect."
            )
        }

        val code = viewModel.twoFactorFuture.get()
        return CompletableFuture.completedFuture(code)
    }

    override fun onChanged(qrAuthSession: QrAuthSession) {
        viewModel.drawQRCode(qrAuthSession)
    }
}
