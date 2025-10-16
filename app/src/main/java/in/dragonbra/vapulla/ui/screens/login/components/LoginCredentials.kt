package `in`.dragonbra.vapulla.ui.screens.login.components

import android.R
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
internal fun LoginCredentials(
    isLoading: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    passwordVisible: Boolean,
    password: String,
    onPasswordVisible: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    buttonContent: @Composable () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsernameTextField(
                isLoading = isLoading,
                username = username,
                onUsernameChange = onUsernameChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            PasswordTextField(
                isLoading = isLoading,
                passwordVisible = passwordVisible,
                password = password,
                onPasswordVisible = onPasswordVisible,
                onPasswordChange = onPasswordChange,
            )

            Spacer(modifier = Modifier.height(16.dp))

            buttonContent()
        }
    }
}

@Composable
private fun UsernameTextField(
    isLoading: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
) {
    OutlinedTextField(
        //modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true,
        value = username,
        onValueChange = onUsernameChange,
        label = { Text("Username") },
        placeholder = { Text("Enter your username") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null
            )
        },
    )
}

@Composable
private fun PasswordTextField(
    isLoading: Boolean,
    passwordVisible: Boolean,
    password: String,
    onPasswordVisible: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    OutlinedTextField(
        // modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
        singleLine = true,
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        placeholder = { Text("Enter your password") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_lock_idle_lock),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        trailingIcon = {
            IconButton(onClick = { onPasswordVisible(!passwordVisible) }) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Default.Visibility
                    } else {
                        Icons.Default.VisibilityOff
                    },
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            LoginCredentials(
                isLoading = false,
                username = "My Username",
                onUsernameChange = { },
                passwordVisible = false,
                password = "My Password",
                onPasswordVisible = { },
                onPasswordChange = { },
                buttonContent = { }
            )
        }
    }
}