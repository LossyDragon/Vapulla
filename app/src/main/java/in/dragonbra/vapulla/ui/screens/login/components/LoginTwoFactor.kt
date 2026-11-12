package `in`.dragonbra.vapulla.ui.screens.login.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LoginTwoFactor(
    code: String,
    onTwoFactorChange: (String) -> Unit,
    onTwoFactorSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            value = code,
            onValueChange = onTwoFactorChange,
            label = { Text("Steam Guard code") },
            leadingIcon = { Icon(imageVector = Icons.Default.Numbers, contentDescription = null) },
            textStyle = MaterialTheme.typography.titleLarge.copy(letterSpacing = 6.sp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onTwoFactorSubmit() }),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onTwoFactorSubmit,
            enabled = code.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonDefaults.MediumContainerHeight),
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.MediumContentPadding,
        ) {
            Text(text = "Verify", style = MaterialTheme.typography.titleMedium)
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
