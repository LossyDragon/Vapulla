package `in`.dragonbra.vapulla.compose.screens.login

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.home.HomeActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.getErrorMessage
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.Notifications
import `in`.dragonbra.vapulla.util.Utils
import timber.log.Timber
import javax.inject.Inject

// TODO: Add 'Try Again' option
// TODO: Add Splash Screen

@AndroidEntryPoint
class LoginActivity : VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("LoginActivity")
        setContent {
            VapullaTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onStartService = {
                        startSteamService { viewModel.onEvent(LoginEvent.ShowLoading(true)) }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.loginState = viewModel.loginState.copy(expectSteamGuard = false)
    }

    override fun onConnected(showLoading: () -> Unit, isLoggedIn: () -> Unit) {
        super.onConnected(showLoading, isLoggedIn)
        steamService?.let { service ->
            if (service.isLoggedIn) {
                isLoggedIn()
                return
            }

            service.logOn(viewModel.logOnDetails)
            showLoading()
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Timber.d("onDisconnected")
        with(viewModel) {
            if (!loginState.expectSteamGuard) {
                if (accountManager.hasLoginKey) {
                    val event = LoginEvent.ShowFailedScreen
                    viewModel.onEvent(event)
                }
            }
        }
    }

    override fun onLoggedOn(callback: LoggedOnCallback) {
        super.onLoggedOn(callback)
        if (callback.result != EResult.OK) {
            val eResult =
                listOf(EResult.AccountLogonDenied, EResult.AccountLoginDeniedNeedTwoFactor)

            if (eResult.any { callback.result == it }) {
                if (callback.result == EResult.AccountLoginDeniedNeedTwoFactor) {
                    val is2Fa = callback.result == EResult.AccountLoginDeniedNeedTwoFactor
                    val event = LoginEvent.ShowSteamGuard(is2fa = is2Fa, expectSteamGuard = true)
                    viewModel.onEvent(event)
                }
            } else {
                Timber.w("Failed to log in ${callback.result} / ${callback.extendedResult}")
                val failedEvent = LoginEvent.ShowSteamGuard(is2fa = false, expectSteamGuard = false)
                viewModel.onEvent(failedEvent)

                // SnackBar this?
                val errorMessage = getErrorMessage(callback.result, callback.extendedResult)
                val authEResult = listOf(
                    EResult.TwoFactorCodeMismatch,
                    EResult.InvalidLoginAuthCode
                )

                if (authEResult.any { callback.result == it }) {
                    val event = LoginEvent.ShowSteamGuard(
                        expectSteamGuard = true,
                        is2fa = false,
                        error = errorMessage
                    )
                    viewModel.onEvent(event)
                } else {
                    val event = LoginEvent.ShowLoginForm(errorMessage)
                    viewModel.onEvent(event)
                }
            }
            steamService?.disconnect()
            return
        }

        viewModel.onEvent(LoginEvent.ShowSteamGuard(is2fa = false, expectSteamGuard = false))

        steamService?.getHandler<SteamFriends>()?.setPersonaState(EPersonaState.Online)

        accountManager.username = viewModel.logOnDetails.username

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
        Timber.d("Bound to Steam service")

        // Create our notification channels when the service is bound.
        if (Utils.isGreaterThanO) {
            Notifications.createMessagesNotificationChannel(notificationManager)
            Notifications.createRequestNotificationChannel(notificationManager)
            Notifications.createServiceNotificationChannel(notificationManager)
        }

        if (accountManager.hasLoginKey) {
            with(viewModel.logOnDetails) {
                loginKey = accountManager.loginKey
                password = null
                username = accountManager.username
            }
            startSteamService { viewModel.onEvent(LoginEvent.ShowLoading(true)) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.d("Unbound from Steam service")
    }
}
