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
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.compose.screens.home.HomeActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.util.getErrorMessage
import `in`.dragonbra.vapulla.core.Constants
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("onCreate")

        viewModel.prefillInputs(accountManager.username)

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isServiceNotBound.value
            }
        }

        setContent {
            VapullaTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onBindService = ::onServiceStart,
                    onStartService = ::startSteamService,
                    onSettings = ::onSettings,
                    onReset = ::onReset
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onConnected() {
        steamService?.let { service ->
            if (service.isLoggedIn) {
                onLoginSuccess()
                return
            }

            service.logOn(viewModel.logOnDetails)
            viewModel.onLoadingVisible(true)
        }
    }

    override fun onDisconnected() {
        super.onDisconnected()
        Timber.d("onDisconnected")
        with(viewModel) {
            if (!loginState.value.expectSteamGuard) {
                if (!accountManager.loginKey.isNullOrEmpty()) {
                    viewModel.showFailedScreen()
                }
            }
        }
    }

    override fun onLoggedOn(callback: LoggedOnCallback) {
        super.onLoggedOn(callback)
        if (callback.result != EResult.OK) {
            val eResult = listOf(
                EResult.AccountLogonDenied,
                EResult.AccountLoginDeniedNeedTwoFactor
            )

            if (eResult.any { callback.result == it }) {
                if (callback.result == EResult.AccountLoginDeniedNeedTwoFactor) {
                    val is2Fa = callback.result == EResult.AccountLoginDeniedNeedTwoFactor
                    viewModel.onShowSteamGuard(true, "", is2Fa)
                }
            } else {
                Timber.w("Failed to log in ${callback.result} / ${callback.extendedResult}")
                viewModel.onShowSteamGuard(false, "", false)

                // SnackBar this?
                val errorMessage = getErrorMessage(callback.result, callback.extendedResult)
                val authEResult = listOf(
                    EResult.TwoFactorCodeMismatch,
                    EResult.InvalidLoginAuthCode
                )

                if (authEResult.any { callback.result == it }) {
                    viewModel.onShowSteamGuard(true, errorMessage, true)
                } else {
                    viewModel.onShowMessage(errorMessage, true)
                }
            }
            steamService?.disconnect()
            return
        }

        viewModel.onShowSteamGuard(false, "", false)

        scope.launch(Dispatchers.IO) {
            steamService?.getHandler<SteamFriends>()?.setPersonaState(EPersonaState.Online)
        }

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

        // Create our notification channels when the service is bound.
        if (Constants.isAtLeastO) {
            Notifications.createMessagesNotificationChannel(notificationManager)
            Notifications.createRequestNotificationChannel(notificationManager)
            Notifications.createServiceNotificationChannel(notificationManager)
        }

        if (!accountManager.loginKey.isNullOrEmpty() && !accountManager.username.isNullOrEmpty()) {
            with(viewModel.logOnDetails) {
                loginKey = accountManager.loginKey
                password = null
                username = accountManager.username
            }

            startSteamService()
            viewModel.onLoadingVisible(true)
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

    private fun onReset() {
        accountManager.clear()
        finish()
    }
}
