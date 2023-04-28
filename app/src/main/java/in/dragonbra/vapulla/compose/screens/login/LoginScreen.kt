package `in`.dragonbra.vapulla.compose.screens.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector as Animation
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.LoginTextField
import `in`.dragonbra.vapulla.compose.components.PermissionsDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.core.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onStartService: () -> Unit,
    onCancelService: () -> Unit,
    onBindService: () -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.loginState.collectAsState()
    val scope = rememberCoroutineScope()

    @SuppressLint("InlinedApi")
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(Unit) {
        if (Constants.isAtLeastT) {
            permissionState.launchPermissionRequest()
        }

        viewModel.loginEvents.collect { event ->
            when (event) {
                is ValidationEvent.BindService -> onBindService()
                is ValidationEvent.CancelService -> onCancelService()
                is ValidationEvent.StartService -> {
                    viewModel.onLoadingVisible(true)
                    onStartService()
                }
            }
        }
    }

    /* Notifications Permissions Dialog */
    PermissionsDialog(
        permissionState = permissionState,
        onPermGranted = onBindService,
        onSettings = onSettings
    )

    /* Content */
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        )
    )
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Sign in using your Steam App authenticator")

                Spacer(modifier = Modifier.height(12.dp))

                val bitmapStateFlow = viewModel.qrCodeStateFlow.collectAsState(initial = null)
                val bitmap = bitmapStateFlow.value

                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null)
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(128.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.hide()
                            viewModel.cancelLoginQR()
                        }
                    },
                    content = { Text(text = stringResource(id = R.string.cancel)) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) { paddingValues ->
        LoginScreenContent(
            modifier = Modifier.padding(paddingValues),
            loginState = state,
            onUsername = viewModel::onUsernameUpdate,
            onPassword = viewModel::onPasswordUpdate,
            onSteamGuard = viewModel::onSteamGuardUpdate,
            onPasswordVisible = viewModel::onPasswordVisible,
            onLogin = viewModel::doLogin,
            onLoginQR = {
                scope.launch {
                    scaffoldState.bottomSheetState.expand()
                    viewModel.doLoginQR()
                }
            },
            onRetry = viewModel::doRetry,
            on2faMessage = { viewModel.onShowMessage(it) }
        )
    }
}

@Composable
private fun LoginScreenContent(
    modifier: Modifier = Modifier,
    loginState: LoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onLoginQR: () -> Unit,
    onRetry: () -> Unit,
    on2faMessage: (string: String) -> Unit
) {
    Column(
        modifier = modifier
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
            onUsername = onUsername,
            onPassword = onPassword,
            onSteamGuard = onSteamGuard,
            onPasswordVisible = onPasswordVisible,
            on2faMessage = on2faMessage
        )

        /* Login Button */
        LoginButtons(
            modifier = Modifier.padding(horizontal = 16.dp),
            onLogin = onLogin,
            onLoginQR = onLoginQR,
            onRetry = onRetry
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
        label = R.string.textLabelUsername,
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
        onValueChange = onUsername,
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
        label = R.string.textLabelPassword,
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
                stringResource(id = R.string.errorMessageSteamGuardMobile)
            } else {
                stringResource(id = R.string.errorMessageSteamGuardEmail)
            }
            on2faMessage(string) // I don't like this
        }

        LoginTextField(
            modifier = modifier,
            label = R.string.textLabelSteamGuard,
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
            onValueChange = onSteamGuard,
            supportingText = loginState.steamGuardError,
            value = loginState.steamGuard
        )
    }
}

@Composable
private fun LoginButtons(
    modifier: Modifier = Modifier,
    onLogin: () -> Unit,
    onLoginQR: () -> Unit,
    onRetry: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        onClick = onLogin,
        content = { Text(text = stringResource(id = R.string.login)) }
    )

    OutlinedButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        onClick = onLoginQR,
        content = { Text(text = "Sign in via QR") }
    )

    // TODO merge with login button
//    AnimatedVisibility(
//        visible = isRetryVisible,
//        enter = fadeIn() + expandIn(),
//        exit = scaleOut() + fadeOut()
//    ) {
//        OutlinedButton(
//            modifier = modifier.padding(vertical = 12.dp),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = Color.White
//            ),
//            onClick = onRetry,
//            content = { Text(text = stringResource(id = R.string.retry)) }
//        )
//    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LoginScreenContent() {
    val string = stringResource(id = R.string.errorMessageSteamGuardMobile)
    val loginState = LoginState(
        generalMessage = string,
        is2Fa = true,
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
            onLoginQR = {},
            onRetry = {},
            on2faMessage = {}
        )
    }
}
