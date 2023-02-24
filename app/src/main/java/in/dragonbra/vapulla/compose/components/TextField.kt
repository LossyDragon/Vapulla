package `in`.dragonbra.vapulla.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginTextField(
    modifier: Modifier = Modifier,
    @StringRes label: Int,
    isError: Boolean,
    keyboardActions: KeyboardActions,
    keyboardOptions: KeyboardOptions,
    onValueChange: (String) -> Unit,
    supportingText: String,
    trailingIcon: @Composable (() -> Unit)? = null,
    value: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        label = { Text(text = stringResource(id = label)) },
        onValueChange = { onValueChange(it) },
        singleLine = true,
        supportingText = { Text(supportingText) },
        value = value,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon
    )
}
