package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme

@Composable
fun VapullaSelectionDialog(
    icon: ImageVector? = null,
    title: String,
    currentSelection: Long,
    items: Map<String, Long>,
    openDialog: Boolean,
    onPositive: (Long) -> Unit,
    positiveText: String,
    onNegative: () -> Unit,
    negativeText: String
) {
    if (!openDialog) {
        return
    }

    var selectedItem by remember {
        mutableStateOf(items.values.find { it == currentSelection }!!)
    }

    AlertDialog(
        onDismissRequest = onNegative,
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .heightIn(100.dp, 250.dp)
                    .fillMaxWidth()
            ) {
                items.forEach { (key, value) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (selectedItem == value),
                                    onClick = { selectedItem = value },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                modifier = Modifier.padding(end = 16.dp),
                                selected = (selectedItem == value),
                                onClick = null
                            )
                            Text(text = key)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPositive(selectedItem) }) {
                Text(text = positiveText)
            }
        },
        dismissButton = {
            TextButton(onClick = onNegative) {
                Text(text = negativeText)
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VapullaEditDialog(
    icon: ImageVector? = null,
    title: String,
    editTextLabel: String,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = {
            OutlinedTextField(
                modifier = Modifier.padding(vertical = 12.dp),
                value = newName,
                onValueChange = { newName = it },
                singleLine = true,
                label = { Text(text = editTextLabel) },
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
        confirmButton = {
            TextButton(onClick = { onConfirm(newName.text) }) {
                Text(text = stringResource(id = R.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        }
    )
}

@Composable
fun VapullaListDialog(
    icon: ImageVector? = null,
    title: String,
    list: List<Any>,
    openDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (!openDialog) {
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .heightIn(100.dp, 250.dp)
                    .fillMaxWidth()
            ) {
                items(list) {
                    Text(text = it.toString())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        }
    )
}

@Composable
fun VapullaMessageDialog(
    icon: ImageVector? = null,
    title: String,
    message: String,
    openDialog: Boolean,
    onPositive: () -> Unit,
    positiveText: String,
    onNegative: () -> Unit,
    negativeText: String
) {
    if (!openDialog) {
        return
    }

    AlertDialog(
        modifier = Modifier.wrapContentHeight(),
        onDismissRequest = onNegative,
        icon = { icon?.let { Icon(imageVector = it, contentDescription = null) } },
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onPositive) {
                Text(text = positiveText)
            }
        },
        dismissButton = {
            TextButton(onClick = onNegative) {
                Text(text = negativeText)
            }
        }
    )
}

@Preview
@Composable
private fun Preview_DialogSelectionContent() {
    val recentsMap = mapOf(
        "Disable" to -1L,
        "1 day" to 86400000L,
        "3 days" to 259200000L,
        "1 week" to 604800000L,
        "2 weeks" to 1209600000L,
        "1 month" to 2592000000L,
        "Forever" to 0L
    )

    VapullaTheme {
        VapullaSelectionDialog(
            icon = Icons.Default.Badge,
            title = stringResource(id = R.string.dialogTitleRecentFriendChats),
            currentSelection = 86400000L,
            items = recentsMap,
            openDialog = true,
            onPositive = {},
            positiveText = "Confirm",
            onNegative = {},
            negativeText = "Cancel"
        )
    }
}

@Preview
@Composable
private fun Preview_DialogListContent() {
    val list = mutableListOf<String>()
    repeat(25) {
        list.add("Cool name: ${it + 1}")
    }
    VapullaTheme {
        VapullaListDialog(
            icon = Icons.Default.History,
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
            icon = Icons.Default.Edit,
            title = stringResource(id = R.string.dialogTitleNickname, "Blackhole Comet"),
            editTextLabel = stringResource(id = R.string.textLabelNickname),
            currentName = "Google Assistant",
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
        val name = "Blackhole Comet"
        VapullaMessageDialog(
            icon = Icons.Default.Block,
            title = stringResource(id = R.string.dialogTitleBlockFriend, name),
            message = stringResource(id = R.string.dialogMessageBlockFriend, name).repeat(2),
            openDialog = true,
            onPositive = {},
            positiveText = stringResource(id = R.string.block),
            onNegative = {},
            negativeText = stringResource(id = R.string.cancel)
        )
    }
}
