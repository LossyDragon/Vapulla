package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.colorSecondary

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VapullaEditDialog(
    name: String,
    currentName: String?,
    openDialog: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!openDialog) {
        return
    }

    val keyboard = LocalSoftwareKeyboardController.current
    var newName by remember { mutableStateOf(TextFieldValue(currentName ?: "")) }

    Dialog(onDismissRequest = onDismiss) {
        DialogLayout(
            title = stringResource(id = R.string.dialogTitleNickname, name),
            content = {
                OutlinedTextField(
                    modifier = Modifier.padding(vertical = 12.dp),
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text(text = stringResource(id = R.string.nickname)) },
                    keyboardActions = KeyboardActions(
                        onDone = { keyboard?.hide() }
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    )
                )
            },
            positiveText = stringResource(id = R.string.change),
            onPositive = { onConfirm(newName.text) },
            negativeText = stringResource(id = R.string.dialogCancel),
            onNegative = onDismiss
        )
    }
}

@Composable
fun VapullaListDialog(
    title: String,
    list: List<Any>,
    openDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!openDialog) {
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        DialogLayout(
            title = title,
            content = {
                LazyColumn(
                    modifier = Modifier
                        .padding(12.dp)
                        .heightIn(50.dp, 150.dp)
                        .fillMaxWidth()
                ) {
                    items(list) {
                        Text(text = it.toString())
                    }
                }
            },
            positiveText = stringResource(id = R.string.dialogClose),
            onPositive = onDismiss
        )
    }
}

@Composable
fun VapullaMessageDialog(
    title: String,
    message: String,
    openDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!openDialog) {
        return
    }

    Dialog(onDismissRequest = onDismiss) {
        DialogLayout(
            title = title,
            content = {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = message,
                    textAlign = TextAlign.Start,
                    style = TextStyle(fontSize = 12.sp)
                )
            },
            positiveText = stringResource(id = R.string.menuBlock),
            onPositive = onConfirm,
            negativeText = stringResource(id = R.string.dialogCancel),
            onNegative = onDismiss
        )
    }
}

@Composable
private fun DialogLayout(
    title: String,
    content: @Composable () -> Unit,
    positiveText: String,
    onPositive: () -> Unit,
    negativeText: String? = null,
    onNegative: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shadowElevation = 4.dp
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(vertical = 12.dp),
                text = title,
                textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 20.sp)
            )

            content()

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorSecondary),
                onClick = onPositive
            ) {
                Text(
                    text = positiveText,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }

            negativeText?.let {
                TextButton(
                    modifier = Modifier.padding(vertical = 6.dp),
                    onClick = { onNegative?.invoke() }
                ) {
                    Text(
                        text = it,
                        color = Color.LightGray,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_DialogListContent() {
    val list = mutableListOf<String>()
    repeat(20) {
        list.add("Cool name: $it")
    }
    VapullaTheme {
        VapullaListDialog(
            title = stringResource(id = R.string.dialogTitleAliases),
            list = list,
            openDialog = true,
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun Preview_DialogEditContent() {
    VapullaTheme {
        VapullaEditDialog(
            name = "Mr. Fancy Pants",
            currentName = "Fancy Pants",
            openDialog = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun Preview_DialogMessageContent() {
    VapullaTheme {
        VapullaMessageDialog(
            title = stringResource(id = R.string.dialogTitleBlockFriend, "Mr.Fancy Pants"),
            message = stringResource(id = R.string.dialogMessageBlockFriend, "Mr.Fancy Pants"),
            openDialog = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
