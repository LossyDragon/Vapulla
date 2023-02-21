package `in`.dragonbra.vapulla.compose.screens.login

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.activity.HomeActivity
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is ValidationEvent.Login -> {
                    Intent(context, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }.also { intent ->
                        context.startActivity(intent)
                        activity.finish()
                    }
                }
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

        },
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
) {
    val snackBarHostState = remember { SnackbarHostState() }

    Surface {
        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) }
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                /* Logo */
                Image(
                    modifier = Modifier.size(150.dp),
                    painter = painterResource(id = R.drawable.vapulla),
                    contentDescription = "App Logo"
                )

                /* Username */
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    isError = !loginState.usernameError.isNullOrEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                    onValueChange = { onUsername(it) },
                    singleLine = true,
                    value = loginState.username,
                )

                /* Password */
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    isError = !loginState.passwordError.isNullOrEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    label = { Text(text = stringResource(id = R.string.editTextHintPassword)) },
                    onValueChange = { onPassword(it) },
                    singleLine = true,
                    value = loginState.password,
                    visualTransformation = if (loginState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                if (loginState.isSteamGuardVisible) { // TODO animate visibility?
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        isError = !loginState.steamGuardError.isNullOrEmpty(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                        onValueChange = { onSteamGuard(it) },
                        singleLine = true,
                        value = loginState.steamGuard,
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
private fun Preview_LoginScreen() {
    val loginState = LoginState(
        username = "Username",
        usernameError = "Username Error",
        password = "Password",
        passwordError = "PasswordError",
        steamGuard = "1A2B3C",
        steamGuardError = "SteamGuard Error",
        isPasswordVisible = true,
        isSteamGuardVisible = true,
    )
    VapullaTheme {
        LoginScreenContent(
            loginState = loginState,
            onUsername = {},
            onPassword = {},
            onSteamGuard = {},
            onPasswordVisible = {},
            onLogin = {},
        )
    }
}