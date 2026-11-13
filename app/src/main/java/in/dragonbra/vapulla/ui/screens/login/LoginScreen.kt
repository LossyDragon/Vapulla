package `in`.dragonbra.vapulla.ui.screens.login

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.ui.composables.VapullaLoadingAnimation
import `in`.dragonbra.vapulla.ui.screens.login.components.LoginCredentials
import `in`.dragonbra.vapulla.ui.screens.login.components.LoginQRCode
import `in`.dragonbra.vapulla.ui.screens.login.components.LoginTwoFactor
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun LoginScreen(viewModel: LoginViewModel, onNavigateToHome: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { result ->
            if (result) onNavigateToHome()
        }
    }

    LoginScreenContent(
        snackbarHostState = snackbarHostState,
        uiState = uiState,
        onSignInViaCredentials = viewModel::onSignInViaCredentials,
        onSignInViaQR = viewModel::onSignInViaQR,
        onQrCodeCancel = viewModel::onQrCodeCancel,
        onTwoFactorChange = viewModel::onTwoFactorChange,
        onTwoFactorSubmit = viewModel::onTwoFactorSubmit,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
    )
}

@Composable
private fun LoginScreenContent(
    snackbarHostState: SnackbarHostState,
    uiState: LoginUiState,
    onSignInViaCredentials: () -> Unit,
    onSignInViaQR: () -> Unit,
    onQrCodeCancel: () -> Unit,
    onTwoFactorSubmit: () -> Unit,
    onTwoFactorChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            var passwordVisible by rememberSaveable { mutableStateOf(false) }

            VapullaLoadingAnimation(isAnimating = uiState.isLoading, size = 120.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmallEmphasized,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when (uiState.loginStep) {
                    LoginStep.CREDENTIALS -> "Sign in with your Steam account"

                    LoginStep.QRCODE -> "Scan the code with the Steam mobile app"

                    LoginStep.TWOFACTOR -> uiState.twoFactorMessage.ifEmpty {
                        "Enter your Steam Guard code"
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Fixed-height slot so the form does not jump when the indicator or the error
            // appears. They never show together: an error ends the loading state.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!uiState.isLoading && uiState.error != null) {
                    Text(
                        text = uiState.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val indicatorAlpha by animateFloatAsState(
                    targetValue = if (uiState.isLoading) 1f else 0f,
                    label = "loading_indicator_alpha",
                )
                if (indicatorAlpha > 0f) {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .widthIn(max = 200.dp)
                            .fillMaxWidth()
                            .alpha(indicatorAlpha),
                    )
                }
            }

            AnimatedContent(
                targetState = uiState.loginStep,
                modifier = Modifier.widthIn(max = 420.dp),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
                },
                label = "login_step_animation",
            ) { step ->
                when (step) {
                    LoginStep.CREDENTIALS -> LoginCredentials(
                        isLoading = uiState.isLoading,
                        username = uiState.username,
                        onUsernameChange = onUsernameChange,
                        passwordVisible = passwordVisible,
                        password = uiState.password,
                        onPasswordVisible = { passwordVisible = it },
                        onPasswordChange = onPasswordChange,
                        onSignInViaCredentials = onSignInViaCredentials,
                        onSignInViaQR = onSignInViaQR,
                    )

                    LoginStep.QRCODE -> LoginQRCode(
                        code = uiState.qrCode,
                        isWaitingForConfirmation = uiState.isWaitingForConfirmation,
                        onQrCodeCancel = onQrCodeCancel,
                    )

                    LoginStep.TWOFACTOR -> LoginTwoFactor(
                        code = uiState.twoFactorCode,
                        onTwoFactorChange = onTwoFactorChange,
                        onTwoFactorSubmit = onTwoFactorSubmit,
                    )
                }
            }
        }
    }
}

class LoginUiStateProvider : PreviewParameterProvider<LoginUiState> {
    override val values = sequenceOf(
        LoginUiState(
            loginStep = LoginStep.CREDENTIALS,
            username = "My Username",
            password = "My Password",
        ),
        LoginUiState(
            loginStep = LoginStep.CREDENTIALS,
            username = "My Username",
            isLoading = true,
        ),
        LoginUiState(
            loginStep = LoginStep.CREDENTIALS,
            username = "My Username",
            error = "Sign-in failed: TryAnotherCM",
        ),
        LoginUiState(
            loginStep = LoginStep.QRCODE,
            qrCode = "Hello World!",
        ),
        LoginUiState(
            loginStep = LoginStep.TWOFACTOR,
            twoFactorCode = "123XYZ",
        ),
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview(@PreviewParameter(LoginUiStateProvider::class) uiState: LoginUiState) {
    VapullaTheme {
        LoginScreenContent(
            snackbarHostState = SnackbarHostState(),
            uiState = uiState,
            onSignInViaCredentials = { },
            onSignInViaQR = { },
            onQrCodeCancel = { },
            onTwoFactorChange = { },
            onTwoFactorSubmit = { },
            onUsernameChange = { },
            onPasswordChange = { },
        )
    }
}
