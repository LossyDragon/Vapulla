package `in`.dragonbra.vapulla.compose.screens.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector as Animation
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.LoginTextField
import `in`.dragonbra.vapulla.compose.components.PermissionsDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.core.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onStartService: () -> Unit,
    onCancelService: () -> Unit,
    onPermissionsGranted: () -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    @SuppressLint("InlinedApi")
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    LaunchedEffect(Unit) {
        if (Constants.isAtLeastT) {
            permissionState.launchPermissionRequest()
        }

        viewModel.loginEvents.collect { event ->
            when (event) {
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
        onPermGranted = onPermissionsGranted,
        onSettings = onSettings
    )

    /* Content */
    val sheetState = rememberStandardBottomSheetState(skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetSwipeEnabled = false,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource(id = R.string.bottomSheetLoginQRTitle),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    when (val qrState = viewModel.qrCodeState) {
                        is QrState.Ready -> {
                            val qrCodeBackground = MaterialTheme.colorScheme.surfaceVariant
                            val qrCodeColor = MaterialTheme.colorScheme.secondary
                            CoilImage(
                                imageModel = {
                                    qrState.qrCode.render(
                                        margin = 75,
                                        brightColor = qrCodeBackground.toArgb(),
                                        marginColor = qrCodeBackground.toArgb(),
                                        darkColor = qrCodeColor.toArgb()
                                    ).nativeImage()
                                },
                                imageOptions = ImageOptions(
                                    alignment = Alignment.Center,
                                    contentScale = ContentScale.Fit
                                )
                            )
                        }
                        else -> {
                            Box(Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(id = R.string.bottomSheetLoginQRMessage),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            viewModel.cancelLoginQR()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(imageVector = Icons.Rounded.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        }
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        LoginScreenContent(
            modifier = Modifier,
            loginState = state,
            onUsername = viewModel::onUsernameUpdate,
            onPassword = viewModel::onPasswordUpdate,
            onSteamGuard = viewModel::onSteamGuardUpdate,
            onPasswordVisible = viewModel::onPasswordVisible,
            onLogin = viewModel::doLogin,
            onLoginQR = {
                scope.launch {
                    keyboard?.hide()
                    sheetState.expand()
                    viewModel.doLoginQR()
                }
            },
            onTwoFactorSubmit = viewModel::onTwoFactorSubmit
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
    onTwoFactorSubmit: () -> Unit
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

        /* Login fields */
        AnimatedVisibility(
            visible = !loginState.expectSteamGuardApp || !loginState.expectSteamGuardCode
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LoginTextFields(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    loginState = loginState,
                    onUsername = onUsername,
                    onPassword = onPassword,
                    onPasswordVisible = onPasswordVisible
                )

                /* Login Button */
                LoginButtons(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onLogin = onLogin,
                    onLoginQR = onLoginQR
                )
            }
        }

        /* Two Factor */
        AnimatedVisibility(visible = loginState.expectSteamGuardCode) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val focusManager = LocalFocusManager.current
                LoginTextField(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    label = R.string.textLabelSteamGuard,
                    isEnabled = !loginState.isLoading,
                    isError = !loginState.isSteamGuardValid,
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    onValueChange = onSteamGuard,
                    supportingText = "Steam Guard code is not valid.",
                    value = loginState.steamGuard
                )

                Button(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    onClick = onTwoFactorSubmit,
                    content = { Text(text = "Submit") }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun LoginAnimatedLogo(
    modifier: Modifier = Modifier,
    loginState: LoginState
) {
    var atEnd by remember { mutableStateOf(false) }
    val pumperMiddle = Animation.animatedVectorResource(R.drawable.animated_vapulla_middle)
    val pumperBottom = Animation.animatedVectorResource(R.drawable.animated_vapulla_bottom)

    val image: @Composable (painter: Painter) -> Unit = {
        Image(
            modifier = modifier.size(150.dp),
            painter = it,
            contentDescription = null
        )
    }

    LaunchedEffect(loginState.isLoading) {
        // NOTE: animation isn't quite right. Not "Pumping" like the original
        while (loginState.isLoading) {
            delay(300)
            atEnd = !atEnd
            delay(2000)
        }
    }

    Box(modifier = modifier.size(150.dp)) {
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
    onPasswordVisible: (Boolean) -> Unit
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
        isError = !loginState.isUsernameValid,
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
        supportingText = "Username is not valid.",
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
            imeAction = if (!loginState.expectSteamGuardCode) ImeAction.Done else ImeAction.Next
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
        isError = loginState.isPasswordValid != PasswordValidation.Valid,
        label = R.string.textLabelPassword,
        onValueChange = onPassword,
        supportingText = "Password is not Valid.",
        value = loginState.password,
        visualTransformation = if (loginState.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        }
    )
}

@Composable
private fun LoginButtons(
    modifier: Modifier = Modifier,
    onLogin: () -> Unit,
    onLoginQR: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        onClick = onLogin,
        content = { Text(text = stringResource(id = R.string.login)) }
    )

    TextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        onClick = onLoginQR,
        content = { Text(text = "Sign in via QR") }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LoginScreenContent() {
    val string = stringResource(id = R.string.errorMessageSteamGuardMobile)
    val loginState = LoginState(
        generalMessage = string,
        isPasswordValid = PasswordValidation.LetterOrDigit,
        isPasswordVisible = true,
        isSteamGuardValid = false,
        isUsernameValid = false,
        password = "Password",
        steamGuard = "1A2B3C",
        username = "Username"
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
            onTwoFactorSubmit = {}
        )
    }
}
