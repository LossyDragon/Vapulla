package `in`.dragonbra.vapulla.compose.components

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.iconSmallCornerShape

// TODO redo the styling of this
@Composable
fun LoginTextField(
    modifier: Modifier = Modifier,
    @StringRes label: Int,
    isEnabled: Boolean = true,
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
        modifier = modifier,
        enabled = isEnabled,
        isError = isError,
        keyboardActions = keyboardActions,
        keyboardOptions = keyboardOptions,
        label = { Text(text = stringResource(id = label)) },
        onValueChange = { onValueChange(it) },
        singleLine = true,
        shape = iconSmallCornerShape,
        supportingText = if (isError) {
            { Text(supportingText) }
        } else {
            null
        },
        trailingIcon = trailingIcon,
        value = value,
        visualTransformation = visualTransformation
    )
}

@Preview
@Composable
private fun LoginTextField_Preview() {
    VapullaTheme {
        LoginTextField(
            label = R.string.textLabelUsername,
            isError = true,
            keyboardActions = KeyboardActions(),
            keyboardOptions = KeyboardOptions(),
            onValueChange = { },
            supportingText = "Username is not valid.",
            value = "Sample Text"
        )
    }
}
