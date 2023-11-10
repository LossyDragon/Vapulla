package `in`.dragonbra.vapulla.compose.screens.login

import android.Manifest
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.LoginTextField
import `in`.dragonbra.vapulla.compose.components.PermissionsDialog
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.fontFamily
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape
import `in`.dragonbra.vapulla.compose.util.VectorAnimCompat
import `in`.dragonbra.vapulla.core.Constants
import kotlinx.coroutines.launch
import qrcode.QRCode

// TODO: QR sign in doesn't want to work after a cancel/time out.
// TODO: Implement frowny face to errors
// TODO splash screen logo too big now
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class
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
    val snackbarHostState = remember { SnackbarHostState() }

    if (Constants.isAtLeastT) {
        val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            permissionState.launchPermissionRequest()
        }
        /* Notifications Permissions Dialog */
        PermissionsDialog(
            permissionState = permissionState,
            onPermGranted = onPermissionsGranted,
            onSettings = onSettings
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is ValidationEvent.CancelService -> onCancelService()
                is ValidationEvent.StartService -> {
                    viewModel.onLoadingVisible(true)
                    onStartService()
                }

                is ValidationEvent.Message -> snackbarHostState.showSnackbar(
                    message = event.message
                )
            }
        }
    }

    /* Content */
    val sheetState = rememberStandardBottomSheetState(skipHiddenState = false)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )

    BottomSheet(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        qrState = viewModel.qrCodeState,
        onCancel = {
            scope.launch {
                sheetState.hide()
                viewModel.cancelLoginQR()
            }
        },
        content = { paddingValues ->
            val keyboard = LocalSoftwareKeyboardController.current
            LoginScreenContent(
                modifier = Modifier.padding(paddingValues),
                snackbarHostState = snackbarHostState,
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
    )
}

@Composable
private fun LoginScreenContent(
    modifier: Modifier = Modifier,
    loginState: LoginState,
    snackbarHostState: SnackbarHostState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
    onLogin: () -> Unit,
    onLoginQR: () -> Unit,
    onTwoFactorSubmit: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(visible = !loginState.isLoading) {
                ExtendedFloatingActionButton(
                    text = { Text(text = "Sign in via QR") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null
                        )
                    },
                    onClick = onLoginQR
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    content = { LoginAnimatedLogo(isLoading = loginState.isLoading) }
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    fontFamily = fontFamily,
                    style = MaterialTheme.typography.displayLarge
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                /* Login fields */
                AnimatedVisibility(
                    visible = !loginState.expectSteamGuardApp && !loginState.expectSteamGuardCode
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginTextFields(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            loginState = loginState,
                            onUsername = onUsername,
                            onPassword = onPassword,
                            onPasswordVisible = onPasswordVisible
                        )
                        /* Login Button */
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .fillMaxWidth(),
                            onClick = onLogin,
                            shape = iconSmallCornerShape,
                            content = { Text(text = stringResource(id = R.string.login)) }
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
                            isEnabled = loginState.expectSteamGuardCode,
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
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 12.dp
                            ),
                            onClick = onTwoFactorSubmit,
                            content = { Text(text = "Submit") }
                        )
                    }
                }

                /* Use Mobile app */
                AnimatedVisibility(visible = loginState.expectSteamGuardApp) {
                    Text(
                        modifier = Modifier.padding(
                            horizontal = 24.dp,
                            vertical = 12.dp
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        text = "Use your Steam mobile app to Approve or Deny the login request"
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginAnimatedLogoError(
    modifier: Modifier = Modifier,
    isError: Boolean
) {
    val density = LocalDensity.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val sizeInPx = with(density) { 150.dp.toPx().toInt() }
            fun getDrawable(drawable: Int) = AppCompatResources.getDrawable(context, drawable)
            FrameLayout(context).apply {
                ImageView(context).apply {
                    id = R.id.vapulla_logo_to_face
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    getDrawable(R.drawable.animated_vapulla_to_face).apply(this::setImageDrawable)
                }.also(this::addView)
            }

        }, update = { view ->
            fun getDrawable(drawable: Int) = AppCompatResources.getDrawable(view.context, drawable)
            val d = view.findViewById<ImageView>(R.id.vapulla_logo_to_face)
            if (isError) {
                (d.drawable as Animatable).stop()
                d.setImageDrawable(getDrawable(R.drawable.animated_vapulla_to_face))
                (d.drawable as Animatable).start()
            } else {
                (d.drawable as Animatable).stop()
                d.setImageDrawable(getDrawable(R.drawable.animated_vapulla_from_face))
                (d.drawable as Animatable).start()
            }
        }
    )
}

@Composable
private fun LoginAnimatedLogo(
    modifier: Modifier = Modifier,
    isLoading: Boolean
) {
    val density = LocalDensity.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val sizeInPx = with(density) { 150.dp.toPx().toInt() }
            fun getDrawable(drawable: Int) =
                AppCompatResources.getDrawable(context, drawable)
            FrameLayout(context).apply {
                ImageView(context).apply {
                    id = R.id.vapulla_logo_bottom
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    getDrawable(R.drawable.animated_vapulla_bottom).apply(this::setImageDrawable)
                }.also(this::addView)
                ImageView(context).apply {
                    id = R.id.vapulla_logo_middle
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    getDrawable(R.drawable.animated_vapulla_middle).apply(this::setImageDrawable)
                }.also(this::addView)
                ImageView(context).apply {
                    id = R.id.vapulla_logo_top
                    layoutParams = FrameLayout.LayoutParams(sizeInPx, sizeInPx)
                    getDrawable(R.drawable.vapulla_top).apply(this::setImageDrawable)
                }.also(this::addView)
            }
        },
        update = { view ->
            val handler = android.os.Handler(Looper.getMainLooper())
            val d =
                view.findViewById<ImageView>(R.id.vapulla_logo_bottom).drawable as Animatable
            val d2 =
                view.findViewById<ImageView>(R.id.vapulla_logo_middle).drawable as Animatable
            if (isLoading) {
                VectorAnimCompat.registerAnimationCallback(
                    d,
                    object : Animatable2Compat.AnimationCallback() {
                        override fun onAnimationEnd(drawable: Drawable) {
                            d.start()
                            handler.postDelayed({
                                d2.stop()
                                d2.start()
                            }, 300)
                        }
                    }
                )
                d.start()
                handler.postDelayed({ d2.start() }, 300)
            } else {
                handler.removeCallbacksAndMessages(null)
                VectorAnimCompat.clearAnimationCallbacks(d)
                VectorAnimCompat.clearAnimationCallbacks(d2)
            }
        }
    )
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
    val usernameAutofillNode =
        AutofillNode(listOf(AutofillType.Username), null) { onUsername(it) }
    val passwordAutofillNode =
        AutofillNode(listOf(AutofillType.Password), null) { onPassword(it) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheet(
    modifier: Modifier,
    scaffoldState: BottomSheetScaffoldState,
    qrState: QrState,
    onCancel: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetSwipeEnabled = false,
        sheetContent = {
            Column(modifier = modifier) {
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
                    val qrCodeBackground = MaterialTheme.colorScheme.surfaceVariant
                    val qrCodeColor = MaterialTheme.colorScheme.secondary
                    when (qrState) {
                        is QrState.Ready -> {
                            CoilImage(
                                imageModel = {
                                    QRCode.ofRoundedSquares()
                                        .withColor(qrCodeColor.toArgb())
                                        .withBackgroundColor(qrCodeBackground.toArgb())
                                        .build(qrState.code)
                                        .render()
                                        .nativeImage()
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
                    onClick = onCancel,
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
        },
        content = content
    )
}

/**
 * Previews
 */

@Preview
@Composable
private fun Preview_VapullaLoginLogo() {
    var isLoading by remember { mutableStateOf(false) }
    VapullaTheme {
        Column {
            LoginAnimatedLogo(isLoading = isLoading)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isLoading = !isLoading }) {
                Text(text = "IsLoading: $isLoading")
            }
        }
    }
}

@Preview
@Composable
private fun Preview_VapullaLoginLogoError() {
    var isError by remember { mutableStateOf(true) }
    VapullaTheme {
        Column {
            LoginAnimatedLogoError(isError = isError)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isError = !isError }) {
                Text(text = "isError: $isError")
            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LoginScreenContent3() {
    val loginState = LoginState(
        isLoading = true,
        expectSteamGuardCode = true,
        steamGuard = "1A2B3C"
    )
    VapullaTheme {
        LoginScreenContent(
            loginState = loginState,
            snackbarHostState = SnackbarHostState(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview_BottomSheet() {
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = false
    )
    VapullaTheme {
        BottomSheet(
            modifier = Modifier,
            scaffoldState = rememberBottomSheetScaffoldState(sheetState),
            qrState = QrState.Loading,
            onCancel = { },
            content = { }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LoginScreenContent2() {
    val loginState = LoginState(
        isLoading = true,
        expectSteamGuardApp = true
    )
    VapullaTheme {
        LoginScreenContent(
            loginState = loginState,
            snackbarHostState = SnackbarHostState(),
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

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_LoginScreenContent() {
    val loginState = LoginState(
        isPasswordValid = PasswordValidation.LetterOrDigit,
        isPasswordVisible = true,
        isUsernameValid = false,
        password = "Password",
        username = "Username"
    )
    VapullaTheme {
        LoginScreenContent(
            loginState = loginState,
            snackbarHostState = SnackbarHostState(),
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
