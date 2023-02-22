package `in`.dragonbra.vapulla.compose.screens.login

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import kotlinx.coroutines.delay

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

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationGraphicsApi::class,
    ExperimentalComposeUiApi::class
)
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
    val snackBarHostState = remember { SnackbarHostState() } // TODO Not used

    Surface {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) }
        ) { pv ->
            Column(
                modifier = Modifier
                    .imePadding()
                    .padding(pv)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                /* Logo */
                var atEnd by remember { mutableStateOf(false) }
                val pumperMiddle =
                    AnimatedImageVector.animatedVectorResource(R.drawable.animated_vapulla_middle)
                val pumperBottom =
                    AnimatedImageVector.animatedVectorResource(R.drawable.animated_vapulla_bottom)

                LaunchedEffect(loginState.isLoading) {
                    // TODO animation isnt right.
                    while (loginState.isLoading) {
                        delay(300)
                        atEnd = !atEnd
                        delay(pumperBottom.totalDuration.toLong()) // 2000
                    }
                }

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Image(
                        modifier = Modifier.size(150.dp),
                        painter = rememberAnimatedVectorPainter(pumperBottom, atEnd),
                        contentDescription = null
                    )
                    Image(
                        modifier = Modifier.size(150.dp),
                        painter = rememberAnimatedVectorPainter(pumperMiddle, !atEnd),
                        contentDescription = null
                    )
                    Image(
                        modifier = Modifier.size(150.dp),
                        painter = painterResource(id = R.drawable.vapulla_top),
                        contentDescription = null
                    )
                }

                Text(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = loginState.generalMessage ?: "",
                    color = Color.Red
                )

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
                OutlinedTextField(
                    modifier = Modifier
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
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    isError = !loginState.usernameError.isNullOrEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                    onValueChange = { onUsername(it) },
                    supportingText = { Text(loginState.usernameError ?: "") },
                    singleLine = true,
                    value = loginState.username
                )

                /* Password */
                val passwordTransformation = if (loginState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                }

                OutlinedTextField(
                    modifier = Modifier
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
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    isError = !loginState.passwordError.isNullOrEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    label = { Text(text = stringResource(id = R.string.editTextHintPassword)) },
                    onValueChange = { onPassword(it) },
                    singleLine = true,
                    supportingText = { Text(loginState.passwordError ?: "") },
                    value = loginState.password,
                    visualTransformation = passwordTransformation,
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
                    }
                )

                /* SteamGuard */
                if (loginState.expectSteamGuard) { // TODO animate visibility?
                    val string = if (loginState.is2Fa) {
                        stringResource(id = R.string.loadingTextSteamGuardMobile)
                    } else {
                        stringResource(id = R.string.loadingTextSteamGuardEmail)
                    }

                    on2faMessage(string) // I don't like this

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        isError = !loginState.steamGuardError.isNullOrEmpty(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        label = {
                            Text(text = stringResource(id = R.string.editTextHintSteamGuard))
                        },
                        onValueChange = { onSteamGuard(it) },
                        supportingText = { Text(loginState.steamGuardError ?: "") },
                        singleLine = true,
                        value = loginState.steamGuard
                    )
                }

                /* Login Button */
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    onClick = onLogin,
                    content = { Text(text = stringResource(id = R.string.buttonLogin)) }
                )
            }
        }
    }
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
