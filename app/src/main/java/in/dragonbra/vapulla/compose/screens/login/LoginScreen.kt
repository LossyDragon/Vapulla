package `in`.dragonbra.vapulla.compose.screens.login

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.R

@Destination(start = true)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel()
) {
    LoginScreenContent(
        username = viewModel.username,
        password = viewModel.password,
        steamGuard = viewModel.steamGuard,
        passwordVisible = viewModel.isPasswordVisible,
        steamGuardVisible = viewModel.isSteamGuardVisible,
        onUsername = { viewModel.username = it },
        onPassword = { viewModel.password = it },
        onSteamGuard = { viewModel.steamGuard = it },
        onPasswordVisible = { viewModel.isPasswordVisible = it },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreenContent(
    username: String,
    password: String,
    steamGuard: String,
    passwordVisible: Boolean,
    steamGuardVisible: Boolean,
    onUsername: (String) -> Unit,
    onPassword: (String) -> Unit,
    onSteamGuard: (String) -> Unit,
    onPasswordVisible: (Boolean) -> Unit,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    value = username,
                    onValueChange = { onUsername(it) },
                    label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                )

                /* Password */
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    value = password,
                    onValueChange = { onPassword(it) },
                    label = { Text(text = stringResource(id = R.string.editTextHintPassword)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val trailingIcon = if (passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        }
                        IconButton(onClick = { onPasswordVisible(!passwordVisible) }) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )

                /* SteamGuard */
                if (steamGuardVisible) { // TODO animate visibility?
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        value = steamGuard,
                        onValueChange = { onSteamGuard(it) },
                        label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                    )
                }

                /* Login Button */
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = stringResource(id = R.string.buttonLogin))
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_LoginScreen() {
    VapullaTheme {
        LoginScreenContent(
            username = "Username",
            password = "Password",
            steamGuard = "123ABC",
            passwordVisible = true,
            steamGuardVisible = true,
            onUsername = {},
            onPassword = {},
            onSteamGuard = {},
            onPasswordVisible = {},
        )
    }
}