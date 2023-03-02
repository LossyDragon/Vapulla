package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    Dialog(onDismissRequest = onNegative) {
        DialogLayoutUI(
            icon = icon,
            title = title,
            content = {
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
                                    .height(56.dp)
                                    .selectable(
                                        selected = (selectedItem == value),
                                        onClick = { selectedItem = value },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
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
            positiveText = positiveText,
            onPositive = { onPositive(selectedItem) },
            negativeText = negativeText,
            onNegative = onNegative
        )
    }
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

    Dialog(onDismissRequest = onDismiss) {
        DialogLayoutUI(
            icon = icon,
            title = title,
            content = {
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
            positiveText = stringResource(id = R.string.change),
            onPositive = { onConfirm(newName.text) },
            negativeText = stringResource(id = R.string.dialogCancel),
            onNegative = onDismiss
        )
    }
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

    Dialog(onDismissRequest = onDismiss) {
        DialogLayoutUI(
            icon = icon,
            title = title,
            content = {
                Divider(Modifier.fillMaxWidth())
                LazyColumn(
                    modifier = Modifier
                        .heightIn(100.dp, 250.dp)
                        .fillMaxWidth()
                ) {
                    items(list) {
                        Text(text = it.toString())
                    }
                }
                Divider(Modifier.fillMaxWidth())
            },
            positiveText = stringResource(id = R.string.dialogClose),
            onPositive = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        onDismissRequest = onNegative
    ) {
        DialogLayoutUI(
            icon = icon,
            title = title,
            message = message,
            positiveText = positiveText,
            onPositive = onPositive,
            negativeText = negativeText,
            onNegative = onNegative
        )
    }
}

// Referenced from https://stackoverflow.com/a/70588212/13225929
// The 2 Compose Dialog Libraries aren't maintained as often
@Composable
fun DialogLayoutUI(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    message: String? = null,
    onPositive: () -> Unit,
    positiveText: String,
    onNegative: (() -> Unit)? = null,
    negativeText: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .padding(10.dp, 5.dp, 10.dp, 10.dp)
            .widthIn(280.dp, 560.dp)
            .wrapContentHeight(unbounded = true),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
            Spacer(modifier = Modifier.height(24.dp))
            icon?.let {
                Icon(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .height(24.dp)
                        .fillMaxWidth(),
                    imageVector = it,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null
                )
            }

            if (message == null && content == null) {
                throw IllegalArgumentException("Message or Content is null")
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                message?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth(),
                        text = it,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                content?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    it.invoke()
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .background(MaterialTheme.colorScheme.secondary),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TextButton(
                    modifier = Modifier.padding(vertical = 4.dp),
                    onClick = onPositive
                ) {
                    Text(
                        text = positiveText,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer

                    )
                }
                if (onNegative != null && negativeText != null) {
                    TextButton(
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = onNegative
                    ) {
                        Text(
                            text = negativeText,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
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
            editTextLabel = stringResource(id = R.string.nickname),
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
            positiveText = stringResource(id = R.string.menuBlock),
            onNegative = {},
            negativeText = stringResource(id = R.string.dialogCancel)
        )
    }
}
