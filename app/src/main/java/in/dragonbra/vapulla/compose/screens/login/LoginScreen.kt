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
import com.ramcosta.composedestinations.annotation.Destination
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.R

@OptIn(ExperimentalMaterial3Api::class)
@Destination(start = true)
@Composable
fun LoginScreen() {
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

                var usernameInput by remember { mutableStateOf("") }
                var passwordInput by remember { mutableStateOf("") }
                var steamGuardInput by remember { mutableStateOf("") }
                var isPasswordVisible by remember { mutableStateOf(false) }
                var isSteamGuardVisible by remember { mutableStateOf(false) }

                /* Username */
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text(text = stringResource(id = R.string.editTextHintUsername)) },
                )

                /* Password */
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text(text = stringResource(id = R.string.editTextHintPassword)) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val trailingIcon = if (isPasswordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        }
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )

                /* SteamGuard */
                if (isSteamGuardVisible) { // TODO animate visibility?
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true,
                        value = steamGuardInput,
                        onValueChange = { steamGuardInput = it },
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
        LoginScreen()
    }
}