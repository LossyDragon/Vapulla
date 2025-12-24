package `in`.dragonbra.vapulla.ui.screens.login

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.ui.composables.VapullaLoadingAnimation
import `in`.dragonbra.vapulla.ui.screens.login.components.LoginButtons
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = {
                var passwordVisible by rememberSaveable { mutableStateOf(false) }

                VapullaLoadingAnimation(isAnimating = uiState.isLoading)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    fontSize = 48.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(
                    targetState = uiState.loginStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                    },
                    label = "login_step_animation",
                ) { step ->
                    when (step) {
                        LoginStep.CREDENTIALS -> {
                            LoginCredentials(
                                isLoading = uiState.isLoading,
                                username = uiState.username,
                                onUsernameChange = onUsernameChange,
                                passwordVisible = passwordVisible,
                                password = uiState.password,
                                onPasswordVisible = { passwordVisible = it },
                                onPasswordChange = onPasswordChange,
                                buttonContent = {
                                    LoginButtons(
                                        onSignInViaCredentials = onSignInViaCredentials,
                                        onSignInViaQR = onSignInViaQR,
                                    )
                                },
                            )
                        }

                        LoginStep.QRCODE -> {
                            LoginQRCode(
                                code = uiState.qrCode,
                                isWaitingForConfirmation = uiState.isWaitingForConfirmation,
                                onQrCodeCancel = onQrCodeCancel,
                            )
                        }

                        LoginStep.TWOFACTOR -> {
                            LoginTwoFactor(
                                code = uiState.twoFactorCode,
                                onTwoFactorChange = onTwoFactorChange,
                                onTwoFactorSubmit = onTwoFactorSubmit,
                            )
                        }
                    }
                }
            },
        )
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
