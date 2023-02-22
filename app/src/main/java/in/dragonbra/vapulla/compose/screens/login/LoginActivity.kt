package `in`.dragonbra.vapulla.compose.screens.login

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.SteamFriends
import `in`.dragonbra.javasteam.steam.handlers.steamuser.callback.LoggedOnCallback
import `in`.dragonbra.vapulla.VapullaBaseActivity
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.extension.getErrorMessage
import `in`.dragonbra.vapulla.manager.AccountManager
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : VapullaBaseActivity() {

    @Inject
    lateinit var accountManager: AccountManager

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

    override fun onConnected(
        showLoading: () -> Unit,
        isLoggedIn: () -> Unit
    ) {
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
        Timber.d("onDisconnected")
        if (!viewModel.loginState.expectSteamGuard && !viewModel.loginState.is2Fa) {
            viewModel.onEvent(LoginEvent.ShowFailedScreen)
        }
    }

    override fun onLoggedOn(callback: LoggedOnCallback) {
        if (callback.result != EResult.OK) {
            val eResult =
                listOf(EResult.AccountLogonDenied, EResult.AccountLoginDeniedNeedTwoFactor)

            if (eResult.any { callback.result == it }) {
                if (callback.result == EResult.AccountLoginDeniedNeedTwoFactor) {
                    val is2Fa = callback.result == EResult.AccountLoginDeniedNeedTwoFactor
                    viewModel.onEvent(
                        LoginEvent.ShowSteamGuard(is2fa = is2Fa, expectSteamGuard = true)
                    )
                }
            } else {
                Timber.w("Failed to log in ${callback.result} / ${callback.extendedResult}")
                viewModel.onEvent(
                    LoginEvent.ShowSteamGuard(is2fa = false, expectSteamGuard = false)
                )

                val errorMessage = getErrorMessage(callback.result, callback.extendedResult)
                val authEResult =
                    listOf(EResult.TwoFactorCodeMismatch, EResult.InvalidLoginAuthCode)

                if (authEResult.any { callback.result == it }) {
                    viewModel.onEvent(
                        LoginEvent.ShowSteamGuard(is2fa = false, error = errorMessage)
                    )
                } else {
                    viewModel.onEvent(LoginEvent.ShowLoginForm(errorMessage))
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
        Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also {
            startActivity(it)
        }
        finish()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        Timber.d("Bound to Steam service")

        if (accountManager.hasLoginKey()) {
            viewModel.logOnDetails.loginKey = accountManager.loginKey
            viewModel.logOnDetails.password = null
            viewModel.logOnDetails.username = accountManager.username
            startSteamService { viewModel.onEvent(LoginEvent.ShowLoading(true)) }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        Timber.d("Unbound from Steam service")
    }
}
