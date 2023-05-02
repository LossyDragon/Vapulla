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
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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

        var accountUsername = viewModel.accountManager.username
        var accountRefreshToken = viewModel.accountManager.loginKey

        if (viewModel.loginState.value.isSigningInViaQR) {
            val steamClient = steamService!!.steamClient
            val unifiedMessages = steamService!!.unifiedMessages

            val auth = SteamAuthentication(steamClient, unifiedMessages)

            val authSessionDetails = AuthSessionDetails().apply {
                deviceFriendlyName = "Vapulla - Android"
                persistentSession = true
            }

            val authSession: QrAuthSession = auth.beginAuthSessionViaQR(authSessionDetails)

            authSession.challengeUrlChanged = this

            viewModel.drawQRCode(authSession)

            val pollResponse = authSession.pollingWaitForResult()

            Timber.i("Connected to Steam! Logging in " + pollResponse.accountName + "...")

            // Save our results (username and refresh token) to account manager.
            viewModel.accountManager.username = pollResponse.accountName
            viewModel.accountManager.loginKey = pollResponse.refreshToken
            accountUsername = pollResponse.accountName
            accountRefreshToken = pollResponse.refreshToken
        } else {
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

                try {
                    val authSession = auth.beginAuthSessionViaCredentials(authSessionDetails)
                    val authPollResult = authSession.pollingWaitForResult()

                    // Save our results (username and refresh token) to account manager.
                    viewModel.accountManager.username = authPollResult.accountName
                    viewModel.accountManager.loginKey = authPollResult.refreshToken
                    accountUsername = authPollResult.accountName
                    accountRefreshToken = authPollResult.refreshToken
                } catch (e: IllegalArgumentException) {
                    Timber.e("WOAH!", e)
                }
            }
        }

        val logonDetails = LogOnDetails().apply {
            username = accountUsername
            accessToken = accountRefreshToken
            loginID = 149
        }

        steamService?.logOn(logonDetails)
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Timber.d("onDisconnected")
        with(viewModel.loginState.value) {
            if (!expectSteamGuard) {
                if (refreshToken.isNotEmpty()) {
                    viewModel.showFailedScreen()
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
