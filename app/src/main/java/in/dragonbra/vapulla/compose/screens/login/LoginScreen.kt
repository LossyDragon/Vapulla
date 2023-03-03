package `in`.dragonbra.vapulla.compose.screens.login

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.LoginTextField
import `in`.dragonbra.vapulla.compose.components.PermissionsDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.colorSecondary
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.util.Utils
import kotlinx.coroutines.delay
import androidx.compose.animation.graphics.vector.AnimatedImageVector as Animation

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onStartService: () -> Unit,
    onBindService: () -> Unit,
    onSettings: () -> Unit,
    onReset: () -> Unit
) {
    val state by viewModel.loginState.collectAsState()

    @SuppressLint("InlinedApi")
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(Unit) {
        if (Utils.isAtLeastT) {
            permissionState.launchPermissionRequest()
        }

        viewModel.loginEvents.collect { event ->
            when (event) {
                is ValidationEvent.StartService -> onStartService()
                is ValidationEvent.BindService -> onBindService()
            }
        }
    }

    PermissionsDialog(
        permissionState = permissionState,
        onPermGranted = { onBindService() },
        onSettings = onSettings
    )

    /* Content */
    LoginScreenContent(
        loginState = state,
        onUsername = { viewModel.onUsernameUpdate(it) },
        onPassword = { viewModel.onPasswordUpdate(it) },
        onSteamGuard = { viewModel.onSteamGuardUpdate(it) },
        onPasswordVisible = { viewModel.onPasswordVisible(it) },
        onLogin = { viewModel.doLogin() },
        onRetry = { viewModel.doRetry() },
        onClear = onReset,
        on2faMessage = { viewModel.onShowMessage(it, false) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoginScreenContent(
    loginState: LoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    on2faMessage: (string: String) -> Unit
) {
    Column(
        modifier = Modifier
            .waterfallPadding()
            .systemBarsPadding()
            .imePadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /* Logo */
        LoginAnimatedLogo(loginState = loginState)

        /* Status Message */
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = loginState.generalMessage,
            color = MaterialTheme.colorScheme.error
        )

        /* Login TextFields */
        LoginTextFields(
            modifier = Modifier.padding(horizontal = 16.dp),
            loginState = loginState,
            onUsername = { onUsername(it) },
            onPassword = { onPassword(it) },
            onSteamGuard = { onSteamGuard(it) },
            onPasswordVisible = { onPasswordVisible(it) },
            on2faMessage = { on2faMessage(it) }
        )

        /* Login Button */
        LoginButtons(
            modifier = Modifier.padding(horizontal = 16.dp),
            isRetryVisible = loginState.isRetryVisible,
            onLogin = onLogin,
            onRetry = onRetry
        )

        val haptics = LocalHapticFeedback.current
        Text(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClear()
                    }
                ),
            color = friendOffline,
            fontSize = 12.sp,
            text = "Not working right? Long press to reset settings"
        )
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun LoginAnimatedLogo(
    modifier: Modifier = Modifier,
    loginState: LoginState
) {
    var atEnd by remember { mutableStateOf(false) }

    LaunchedEffect(loginState.isLoading) {
        // NOTE: animation isn't quite right. Not "Pumping" like the original
        while (loginState.isLoading) {
            delay(300)
            atEnd = !atEnd
            delay(2000)
        }
    }

    Box(
        modifier = modifier
            .size(150.dp)
            .padding(vertical = 12.dp)
    ) {
        val pumperMiddle = Animation.animatedVectorResource(R.drawable.animated_vapulla_middle)
        val pumperBottom = Animation.animatedVectorResource(R.drawable.animated_vapulla_bottom)

        val image: @Composable (painter: Painter) -> Unit = {
            Image(
                modifier = modifier.size(150.dp),
                painter = it,
                contentDescription = null
            )
        }

        image(rememberAnimatedVectorPainter(pumperBottom, atEnd))
        image(rememberAnimatedVectorPainter(pumperMiddle, !atEnd))
        image(painterResource(id = R.drawable.vapulla_top))
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginTextFields(
    modifier: Modifier = Modifier,
    loginState: LoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
    on2faMessage: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    /* Autofill Nodes */
    val usernameAutofillNode = AutofillNode(listOf(AutofillType.Username), null) { onUsername(it) }
    val passwordAutofillNode = AutofillNode(listOf(AutofillType.Password), null) { onPassword(it) }
    val autofill = LocalAutofill.current
    LocalAutofillTree.current += usernameAutofillNode
    LocalAutofillTree.current += passwordAutofillNode

    /* Username */
    LoginTextField(
        modifier = modifier
            .onGloballyPositioned {
                usernameAutofillNode.boundingBox = it.boundsInWindow()
            }
            .onFocusChanged { state ->
                autofill?.run {
                    if (state.isFocused) {
                        requestAutofillForNode(usernameAutofillNode)
                    } else {
                        cancelAutofillForNode(usernameAutofillNode)
                    }
                }
            },
        label = R.string.editTextHintUsername,
        isError = loginState.usernameError.isNotEmpty(),
        isEnabled = !loginState.isLoading,
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        onValueChange = { onUsername(it) },
        supportingText = loginState.usernameError,
        value = loginState.username
    )

    /* Password */
    LoginTextField(
        modifier = modifier
            .onGloballyPositioned {
                passwordAutofillNode.boundingBox = it.boundsInWindow()
            }
            .onFocusChanged { state ->
                autofill?.run {
                    if (state.isFocused) {
                        requestAutofillForNode(passwordAutofillNode)
                    } else {
                        cancelAutofillForNode(passwordAutofillNode)
                    }
                }
            },
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() },
            onNext = { focusManager.moveFocus(FocusDirection.Down) }
        ),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            keyboardType = KeyboardType.Password,
            imeAction = if (!loginState.expectSteamGuard) ImeAction.Done else ImeAction.Next
        ),
        trailingIcon = {
            IconButton(onClick = { onPasswordVisible(!loginState.isPasswordVisible) }) {
                Icon(
                    imageVector = if (loginState.isPasswordVisible) {
                        Icons.Default.Visibility
                    } else {
                        Icons.Default.VisibilityOff
                    },
                    contentDescription = "Toggle password visibility"
                )
            }
        },
        isEnabled = !loginState.isLoading,
        isError = loginState.passwordError.isNotEmpty(),
        label = R.string.editTextHintPassword,
        onValueChange = onPassword,
        supportingText = loginState.passwordError,
        value = loginState.password,
        visualTransformation = if (loginState.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    )

    /* SteamGuard */
    AnimatedVisibility(
        visible = loginState.expectSteamGuard,
        enter = slideInHorizontally() + fadeIn(),
        exit = slideOutHorizontally() + fadeOut()
    ) {
        if (loginState.expectSteamGuard) {
            val string = if (loginState.is2Fa) {
                stringResource(id = R.string.loadingTextSteamGuardMobile)
            } else {
                stringResource(id = R.string.loadingTextSteamGuardEmail)
            }
            on2faMessage(string) // I don't like this
        }

        LoginTextField(
            modifier = modifier,
            label = R.string.editTextHintSteamGuard,
            isEnabled = !loginState.isLoading,
            isError = loginState.steamGuardError.isNotEmpty(),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            onValueChange = { onSteamGuard(it) },
            supportingText = loginState.steamGuardError,
            value = loginState.steamGuard
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LoginButtons(
    modifier: Modifier = Modifier,
    isRetryVisible: Boolean,
    onLogin: () -> Unit,
    onRetry: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorSecondary,
            contentColor = Color.White
        ),
        onClick = onLogin,
        content = { Text(text = stringResource(id = R.string.buttonLogin)) }
    )

    AnimatedVisibility(
        visible = isRetryVisible,
        enter = fadeIn() + expandIn(),
        exit = scaleOut() + fadeOut()
    ) {
        OutlinedButton(
            modifier = modifier.padding(vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            onClick = onRetry,
            content = { Text(text = stringResource(id = R.string.buttonRetry)) }
        )
    }
}

@Preview
@Composable
private fun Preview_LoginScreenContent() {
    val string = stringResource(id = R.string.loadingTextSteamGuardMobile)
    val loginState = LoginState(
        generalMessage = string,
        is2Fa = true,
        isRetryVisible = true,
        isPasswordVisible = true,
        expectSteamGuard = true,
        password = "Password",
        passwordError = "Password Error",
        steamGuard = "1A2B3C",
        steamGuardError = "SteamGuard Error",
        username = "Username",
        usernameError = "Username Error"
    )
    VapullaTheme {
        LoginScreenContent(
            loginState = loginState,
            onUsername = {},
            onPassword = {},
            onSteamGuard = {},
            onPasswordVisible = {},
            onLogin = {},
            onRetry = {},
            onClear = {},
            on2faMessage = {}
        )
    }
}
