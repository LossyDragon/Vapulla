package `in`.dragonbra.vapulla.ui.screens.login.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.ui.composables.dialog.MessageDialog
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
    onSignInViaCredentials: () -> Unit,
    onSignInViaQR: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            ),
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { onPasswordVisible(!passwordVisible) }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Default.Visibility
                        } else {
                            Icons.Default.VisibilityOff
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSignInViaCredentials() }),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSignInViaCredentials,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Sign in", style = MaterialTheme.typography.titleMedium)
        }

        FilledTonalButton(
            onClick = onSignInViaQR,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = "Sign in with QR code", style = MaterialTheme.typography.titleMedium)
        }

        // Stays enabled while signing in: it only explains, and changes nothing.
        var showPasswordInfo by rememberSaveable { mutableStateOf(false) }

        TextButton(
            onClick = { showPasswordInfo = true },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(text = "Signing in with password?")
        }

        MessageDialog(
            visible = showPasswordInfo,
            icon = Icons.Default.Shield,
            title = "Signing in with a password?",
            message = PASSWORD_INFO,
            useHtmlInMsg = true,
            onDismissRequest = { showPasswordInfo = false },
            onConfirmClick = { showPasswordInfo = false },
        )
    }
}

/**
 * Why the QR code is the better way in, and how to take Vapulla's access away again. The
 * device name is the one sent at logon, which is how Steam lists this session.
 */
private val PASSWORD_INFO = """
    Prefer <b>Sign in with QR code</b>. You approve the sign-in in the Steam app, so your
    password is never typed into Vapulla.<br><br>
    Either way, Vapulla does not keep your password. It saves only the sign-in token Steam
    issues, encrypted on this device, and uses that to sign in next time.<br><br>
    You can revoke that access at any time in the official Steam app:<br><br>
    1. Open <b>Steam Guard</b>.<br>
    2. Tap the gear icon in the bottom left.<br>
    3. Choose <b>Authorized Devices</b>.<br>
    4. Tap <b>PC Steam Client - "Vapulla ${BuildConfig.VERSION_NAME}"</b>.<br>
    5. Tap <b>Remove Access</b>.<br>
    6. Steam asks whether to change your password as well. <b>No Thanks</b> is fine, unless
    you suspect your account has been compromised.
""".trimIndent().replace("\n", " ")

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
                onSignInViaCredentials = { },
                onSignInViaQR = { },
            )
        }
    }
}
