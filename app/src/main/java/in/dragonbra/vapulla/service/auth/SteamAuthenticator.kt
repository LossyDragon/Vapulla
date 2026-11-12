package `in`.dragonbra.vapulla.service.auth

import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.LoginResult
import `in`.dragonbra.vapulla.service.SteamConnection
import `in`.dragonbra.vapulla.service.SteamState
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The two ways of signing in: account name and password (or the saved refresh token), and a
 * QR code approved in the Steam mobile app. Either one opens a fresh [SteamConnection], gets a
 * refresh token from Steam's authentication service, saves it, and logs on with it. Progress
 * is reported through [SteamState.loginResult].
 */
class SteamAuthenticator(
    private val scope: CoroutineScope,
    private val account: AccountManager,
    private val connection: SteamConnection,
) {

    private var isWaitingForQRAuth: Boolean = false
    private var currentAuthJob: Job? = null

    /** Stops whichever sign-in attempt is in progress. */
    fun cancel() {
        isWaitingForQRAuth = false

        currentAuthJob?.cancel(CancellationException("Auth operation cancelled"))
        currentAuthJob = null
    }

    fun handleCredentialLogin(
        username: String? = null,
        password: String? = null,
        authenticator: IAuthenticator,
    ) {
        Timber.d("handleCredentialLogin() - Starting and Logging into Steam")

        // Cancel any ongoing QR auth
        cancel()

        currentAuthJob = scope.launch {
            try {
                connection.open()
                connection.awaitConnected()

                SteamState.loginResult.emit(LoginResult.Loading)

                var username = username?.trim()
                var loginKey = account.refreshToken.first()

                if (loginKey.isNullOrBlank() || username.isNullOrBlank()) {
                    // Normal sign in
                    val authDetails = AuthSessionDetails().apply {
                        this.authenticator = authenticator
                        this.deviceFriendlyName = "Vapulla ${BuildConfig.VERSION_NAME}"
                        this.username = username
                        this.password = password?.trim()
                        this.persistentSession = true
                    }

                    val authSession = connection.authentication
                        ?.beginAuthSessionViaCredentials(authDetails)
                        ?.await() ?: throw IllegalStateException("Steam client not available")

                    val pollResult = authSession.pollingWaitForResult().await()

                    if (pollResult.accountName.isBlank() || pollResult.refreshToken.isBlank()) {
                        val result = LoginResult.Error("Account Name or Refresh Token is blank")
                        SteamState.loginResult.emit(result)
                        return@launch
                    }

                    username = pollResult.accountName
                    loginKey = pollResult.refreshToken

                    account.setUsername(username)
                    account.setRefreshToken(loginKey)
                } else {
                    // Auto sign in
                    username = account.username.first()!!
                    loginKey = account.refreshToken.first()
                }

                connection.logOn(
                    accountName = username,
                    refreshToken = loginKey,
                )
            } catch (e: CancellationException) {
                Timber.i(e, "Credential login cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Error during credential login")
                SteamState.loginResult.emit(LoginResult.Error("Login failed: ${e.message}"))
            } finally {
                currentAuthJob = null
            }
        }
    }

    fun handleQRLogin() {
        Timber.i("Logging in via QR.")

        if (isWaitingForQRAuth) {
            Timber.w("QR login already in progress")
            return
        }

        // Cancel any existing auth operation
        cancel()

        isWaitingForQRAuth = true

        currentAuthJob = scope.launch {
            try {
                connection.open()
                connection.awaitConnected()

                // Check if cancelled while connecting
                if (!isWaitingForQRAuth) {
                    Timber.i("QR login cancelled while connecting")
                    return@launch
                }

                val authDetails = AuthSessionDetails().apply {
                    this.deviceFriendlyName = "Vapulla ${BuildConfig.VERSION_NAME}"
                }

                val authSession = connection.authentication
                    ?.beginAuthSessionViaQR(authDetails)
                    ?.await() ?: throw IllegalStateException("Steam client not available")

                // Check if cancelled while creating session
                if (!isWaitingForQRAuth) {
                    Timber.i("QR login cancelled while creating auth session")
                    return@launch
                }

                authSession.challengeUrlChanged = IChallengeUrlChanged { qrAuthSession ->
                    if (isWaitingForQRAuth) {
                        scope.launch {
                            val qrCode = qrAuthSession?.challengeUrl.orEmpty()
                            SteamState.loginResult.emit(LoginResult.QRCode(qrCode))
                        }
                    }
                }

                // Send initial QR code
                SteamState.loginResult.emit(LoginResult.QRCode(authSession.challengeUrl))

                var authPollResult: AuthPollResult? = null

                // Poll for authentication result
                while (isWaitingForQRAuth && authPollResult == null) {
                    try {
                        authPollResult = authSession.pollAuthSessionStatus().await()
                        if (authPollResult == null && isWaitingForQRAuth) {
                            ensureActive()
                            delay(authSession.pollingInterval.toLong().milliseconds)
                        }
                    } catch (e: Exception) {
                        if (isWaitingForQRAuth) {
                            Timber.e(e, "Error polling auth session")
                            break
                        }
                    }
                }

                // Handle completion
                if (authPollResult != null) {
                    account.setUsername(authPollResult.accountName)
                    account.setRefreshToken(authPollResult.refreshToken)
                    connection.logOn(
                        accountName = authPollResult.accountName,
                        refreshToken = authPollResult.refreshToken,
                    )
                } else {
                    Timber.w("QR login completed without auth result")
                    connection.disconnect()
                }
            } catch (e: CancellationException) {
                Timber.i(e, "QR login cancelled")
            } catch (e: Exception) {
                Timber.e(e, "Error during QR login")
                SteamState.loginResult.emit(LoginResult.Error("QR login failed: ${e.message}"))
            } finally {
                isWaitingForQRAuth = false
                withContext(NonCancellable) {
                    SteamState.loginResult.emit(LoginResult.QRCodeEnded)
                }
                currentAuthJob = null
            }
        }
    }

    fun handleQrCodeCancel() {
        Timber.i("Cancelling QR Login")
        cancel()
    }
}
