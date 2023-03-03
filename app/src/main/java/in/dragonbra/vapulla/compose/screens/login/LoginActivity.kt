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
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.Notifications
import `in`.dragonbra.vapulla.threading.executeAsyncTask
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject

// TODO: Add Splash Screen, need older android device to test

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

        setContent {
            VapullaTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onBindService = { onServiceStart() },
                    onStartService = {
                        startSteamService { viewModel.onLoadingVisible(true) }
                    },
                    onSettings = {
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null)
                        ).also(::startActivity)
                    },
                    onReset = {
                        accountManager.clear()
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun onConnected(showLoading: () -> Unit, isLoggedIn: () -> Unit) {
        steamService?.let { service ->
            if (service.isLoggedIn) {
                isLoggedIn()
                return
            }

            Timber.d("viewModel.logOnDetails ${viewModel.logOnDetails.username}")
            Timber.d("viewModel.logOnDetails ${viewModel.logOnDetails.password}")
            Timber.d("viewModel.logOnDetails ${viewModel.logOnDetails.twoFactorCode}")

            service.logOn(viewModel.logOnDetails)
            showLoading()
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

        CoroutineScope(Dispatchers.Default).executeAsyncTask {
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
        Timber.d("Bound to Steam service")

        // Create our notification channels when the service is bound.
        if (Utils.isAtLeastO) {
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
            startSteamService { viewModel.onLoadingVisible(true) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        Timber.d("Unbound from Steam service")
    }
}
