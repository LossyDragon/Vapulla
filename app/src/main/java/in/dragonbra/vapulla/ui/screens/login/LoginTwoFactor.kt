package `in`.dragonbra.vapulla.ui.screens.login

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@Composable
fun LoginTwoFactor(
    code: String,
    onTwoFactorChange: (String) -> Unit,
    onTwoFactorSubmit: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                singleLine = true,
                value = code,
                onValueChange = onTwoFactorChange,
                label = { Text("Multi-Factor") },
                placeholder = { Text("Enter your Two Factor Code") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Numbers,
                        contentDescription = null
                    )
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onTwoFactorSubmit,
                content = {
                    Text(text = "Submit")
                }
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VapullaTheme {
        Surface {
            LoginTwoFactor(
                code = "A1B2C3",
                onTwoFactorChange = { },
                onTwoFactorSubmit = { },
            )
        }
    }
}