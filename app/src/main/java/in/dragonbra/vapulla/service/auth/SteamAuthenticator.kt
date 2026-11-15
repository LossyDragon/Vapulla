package `in`.dragonbra.vapulla.service.auth

import `in`.dragonbra.javasteam.steam.authentication.AuthPollResult
import `in`.dragonbra.javasteam.steam.authentication.AuthSessionDetails
import `in`.dragonbra.javasteam.steam.authentication.IAuthenticator
import `in`.dragonbra.javasteam.steam.authentication.IChallengeUrlChanged
import `in`.dragonbra.javasteam.steam.authentication.QrAuthSession
import `in`.dragonbra.vapulla.manager.AccountManager
import `in`.dragonbra.vapulla.service.DEVICE_NAME
import `in`.dragonbra.vapulla.service.LoginResult
import `in`.dragonbra.vapulla.service.SteamConnection
import `in`.dragonbra.vapulla.service.SteamState
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The ways of signing in: account name and password (or the saved refresh token), a QR code
 * approved in the Steam mobile app, and without an account. Each one opens a fresh
 * [SteamConnection]; the first two get a refresh token from Steam's authentication service,
 * save it, and log on with it. Progress is reported through [SteamState.loginResult].
 *
 * Only one attempt runs at a time: starting one cancels the one before.
 */
class SteamAuthenticator(
    private val scope: CoroutineScope,
    private val account: AccountManager,
    private val connection: SteamConnection,
) {

    @Volatile
    private var currentAuthJob: Job? = null

    /** The attempt in [currentAuthJob] when it is a QR sign-in. */
    @Volatile
    private var qrLoginJob: Job? = null

    /** True while an attempt is on its way to a logon. */
    val isSigningIn: Boolean get() = currentAuthJob?.isActive == true

    /** Stops whichever sign-in attempt is in progress, a QR code on screen included. */
    @Synchronized
    fun cancel() {
        currentAuthJob?.cancel(CancellationException("Auth operation cancelled"))
        currentAuthJob = null
    }

    /**
     * Signs in with an account name and password. [authenticator] is asked for the Steam
     * Guard code, or told to wait for the approval in the mobile app, when Steam wants one.
     *
     * The password is not needed when [username] is the account whose refresh token is
     * saved: that token then signs in directly, which is how the app signs in by itself.
     */
    fun handleCredentialLogin(
        username: String? = null,
        password: String? = null,
        authenticator: IAuthenticator,
    ) {
        Timber.d("handleCredentialLogin() - Starting and Logging into Steam")

        startAttempt(what = "Login") {
            SteamState.loginResult.emit(LoginResult.Loading)

            val accountName = username?.trim().orEmpty()
            val savedName = account.username.first()
            val savedToken = account.refreshToken.first()

            // A saved token is only good for the account it was issued to.
            if (!savedToken.isNullOrBlank() &&
                accountName.isNotEmpty() &&
                accountName.equals(savedName, ignoreCase = true)
            ) {
                connection.logOn(accountName = accountName, refreshToken = savedToken)
                return@startAttempt
            }

            val authDetails = AuthSessionDetails().apply {
                this.authenticator = authenticator
                this.deviceFriendlyName = DEVICE_NAME
                this.username = accountName
                this.password = password?.trim()
                this.persistentSession = true
            }

            val authSession = connection.authentication
                ?.beginAuthSessionViaCredentials(authDetails)
                ?.await() ?: throw IllegalStateException("Steam client not available")

            logOnWith(authSession.pollingWaitForResult().await())
        }
    }

    /**
     * Signs in with a QR code: Steam hands out a challenge URL to show as a code, and is then
     * asked again and again whether someone approved it in the mobile app. Each code to show
     * is sent as a [LoginResult.QRCode], and [LoginResult.QRCodeEnded] once it is over.
     */
    fun handleQRLogin() {
        Timber.i("Logging in via QR.")

        if (qrLoginJob?.isActive == true) {
            Timber.w("QR login already in progress")
            return
        }

        qrLoginJob = startAttempt(
            what = "QR login",
            onEnd = { SteamState.loginResult.emit(LoginResult.QRCodeEnded) },
        ) {
            val authDetails = AuthSessionDetails().apply {
                this.deviceFriendlyName = DEVICE_NAME
            }

            val authSession = connection.authentication
                ?.beginAuthSessionViaQR(authDetails)
                ?.await() ?: throw IllegalStateException("Steam client not available")

            // Steam replaces the code every so often; polling is what brings the new one.
            authSession.challengeUrlChanged = IChallengeUrlChanged { session ->
                if (isActive) {
                    launch {
                        val qrCode = session?.challengeUrl.orEmpty()
                        SteamState.loginResult.emit(LoginResult.QRCode(qrCode))
                    }
                }
            }
            SteamState.loginResult.emit(LoginResult.QRCode(authSession.challengeUrl))

            val result = awaitApproval(authSession)
            if (result != null) {
                logOnWith(result)
            } else {
                Timber.w("QR login completed without auth result")
                connection.disconnect()
            }
        }
    }

    /** The user closed the QR code: stops the attempt and drops the connection made for it. */
    fun handleQrCodeCancel() {
        Timber.i("Cancelling QR Login")
        cancel()
        connection.disconnect()
    }

    /**
     * Signs in without an account, the way SteamCMD does. Steam then answers what it tells
     * anyone: its public catalogue, depots and servers. Nothing is saved.
     */
    fun handleAnonymousLogin() {
        Timber.i("Logging in anonymously.")

        startAttempt(what = "Login", anonymous = true) {
            SteamState.loginResult.emit(LoginResult.Loading)
            connection.logOnAnonymous()
        }
    }

    /**
     * Runs one sign-in attempt in place of any other: opens a new connection, waits for it to
     * come up and then runs [signIn]. A failure is told to the login screen as "[what] failed".
     *
     * @param onEnd run when the attempt is over, however it ended.
     */
    @Synchronized
    private fun startAttempt(
        what: String,
        anonymous: Boolean = false,
        onEnd: suspend () -> Unit = {},
        signIn: suspend CoroutineScope.() -> Unit,
    ): Job {
        cancel()

        return scope.launch {
            try {
                connection.open(anonymous)
                connection.awaitConnected()
                signIn()
            } catch (e: CancellationException) {
                Timber.i(e, "$what cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error during $what")
                SteamState.loginResult.emit(LoginResult.Error("$what failed: ${e.message}"))
            } finally {
                withContext(NonCancellable) { onEnd() }
            }
        }.also { currentAuthJob = it }
    }

    /**
     * Asks Steam whether the QR code was approved, as often as the session says to.
     *
     * @return null when asking failed, which ends the attempt.
     */
    private suspend fun awaitApproval(authSession: QrAuthSession): AuthPollResult? {
        while (true) {
            val result = try {
                authSession.pollAuthSessionStatus().await()
            } catch (e: Exception) {
                // Not a failure when it is this attempt being cancelled.
                currentCoroutineContext().ensureActive()
                Timber.e(e, "Error polling auth session")
                return null
            }
            if (result != null) return result

            delay(authSession.pollingInterval.toLong().seconds)
        }
    }

    /** Saves the account name and refresh token Steam issued, and logs on with them. */
    private suspend fun logOnWith(result: AuthPollResult) {
        if (result.accountName.isBlank() || result.refreshToken.isBlank()) {
            val error = LoginResult.Error("Account Name or Refresh Token is blank")
            SteamState.loginResult.emit(error)
            return
        }

        account.setUsername(result.accountName)
        account.setRefreshToken(result.refreshToken)

        connection.logOn(accountName = result.accountName, refreshToken = result.refreshToken)
    }
}
