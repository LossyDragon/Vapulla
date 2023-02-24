package `in`.dragonbra.vapulla.compose.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
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
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.components.LoginTextField
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import kotlinx.coroutines.delay
import androidx.compose.animation.graphics.vector.AnimatedImageVector as Animation

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onStartService: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is ValidationEvent.StartService -> onStartService()
            }
        }
    }

    val state by viewModel::loginState
    LoginScreenContent(
        loginState = state,
        onUsername = {
            viewModel.onEvent(LoginEvent.UsernameChanged(it))
        },
        onPassword = {
            viewModel.onEvent(LoginEvent.PasswordChanged(it))
        },
        onSteamGuard = {
            viewModel.onEvent(LoginEvent.SteamGuardChanged(it))
        },
        onPasswordVisible = {
            viewModel.onEvent(LoginEvent.PasswordVisibleChanged(it))
        },
        onLogin = {
            viewModel.onEvent(LoginEvent.Login)
        },
        on2faMessage = {
            viewModel.onEvent(LoginEvent.ShowLoginForm(it))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    loginState: LoginState,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
    onLogin: () -> Unit,
    on2faMessage: (string: String) -> Unit
) {
    Surface {
        Scaffold { pv ->
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(pv)
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
                    loginState = loginState,
                    onUsername = { onUsername(it) },
                    onPassword = { onPassword(it) },
                    onSteamGuard = { onSteamGuard(it) },
                    onPasswordVisible = { onPasswordVisible(it) },
                    on2faMessage = { on2faMessage(it) }
                )

                /* Login Button */
                LoginButtons(onLogin = onLogin)
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

    LaunchedEffect(loginState.isLoading) {
        // TODO animation isn't quite right. Not "Pumping" like the original
        while (loginState.isLoading) {
            delay(300)
            atEnd = !atEnd
            delay(pumperBottom.totalDuration.toLong()) // 2000
        }
    }

    Box(
        modifier = modifier
            .size(150.dp)
            .padding(vertical = 12.dp)
    ) {
        Image(
            modifier = modifier.size(150.dp),
            painter = rememberAnimatedVectorPainter(pumperBottom, atEnd),
            contentDescription = null
        )
        Image(
            modifier = modifier.size(150.dp),
            painter = rememberAnimatedVectorPainter(pumperMiddle, !atEnd),
            contentDescription = null
        )
        Image(
            modifier = modifier.size(150.dp),
            painter = painterResource(id = R.drawable.vapulla_top),
            contentDescription = null
        )
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
    val usernameAutofillNode = AutofillNode(
        autofillTypes = listOf(AutofillType.Username),
        onFill = { onUsername(it) }
    )
    val passwordAutofillNode = AutofillNode(
        autofillTypes = listOf(AutofillType.Password),
        onFill = { onPassword(it) }
    )
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
    val imeAction = if (!loginState.expectSteamGuard) ImeAction.Done else ImeAction.Next
    val passwordTransformation = if (loginState.isPasswordVisible) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }
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
            imeAction = imeAction
        ),
        trailingIcon = {
            val trailingIcon = if (loginState.isPasswordVisible) {
                Icons.Default.Visibility
            } else {
                Icons.Default.VisibilityOff
            }
            IconButton(onClick = { onPasswordVisible(!loginState.isPasswordVisible) }) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = "Toggle password visibility"
                )
            }
        },
        isError = loginState.passwordError.isNotEmpty(),
        label = R.string.editTextHintPassword,
        onValueChange = { onPassword(it) },
        supportingText = loginState.passwordError,
        value = loginState.password,
        visualTransformation = passwordTransformation
    )

    /* SteamGuard */
    AnimatedVisibility(
        visible = loginState.expectSteamGuard,
        enter = fadeIn() + slideInHorizontally(),
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
            label = R.string.editTextHintSteamGuard,
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

@Composable
private fun LoginButtons(
    modifier: Modifier = Modifier,
    onLogin: () -> Unit
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        onClick = onLogin,
        content = { Text(text = stringResource(id = R.string.buttonLogin)) }
    )
}

@Preview
@Composable
private fun Preview_LoginScreenContent() {
    val string = stringResource(id = R.string.loadingTextSteamGuardMobile)
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
            on2faMessage = {}
        )
    }
}
