package `in`.dragonbra.vapulla.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.javasteam.enums.EPersonaState
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.compose.screens.home.HomeState
import `in`.dragonbra.vapulla.compose.ui.theme.VapullaTheme
import `in`.dragonbra.vapulla.compose.ui.theme.friendOffline
import `in`.dragonbra.vapulla.compose.ui.theme.friendOnline
import `in`.dragonbra.vapulla.compose.ui.theme.getAccountStatusColor
import `in`.dragonbra.vapulla.compose.util.StaticImage
import `in`.dragonbra.vapulla.compose.util.getAvatarUrl

@Composable
fun ProfileStatusItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = CircleShape,
    colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Tab }
            .height(32.dp)
            .fillMaxWidth(.5f),
        shape = shape,
        color = colors.containerColor(selected).value,
        interactionSource = interactionSource
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = colors.iconColor(selected).value
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = colors.textColor(selected).value
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = colors.badgeColor(selected).value
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

@Composable
fun VapullaProfileDialog(
    openDialog: Boolean,
    state: HomeState,
    onStatusChange: (EPersonaState) -> Unit,
    onPersonAdd: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!openDialog) {
        return
    }

    val borderStroke = BorderStroke(4.dp, getAccountStatusColor(state.status))
    val cornerShape = RoundedCornerShape(16.dp)
    var selectedItem by remember { mutableStateOf(EPersonaState.Online) }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                /* Icon, Name, and Status */
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StaticImage(
                        modifier = Modifier
                            .size(64.dp)
                            .border(borderStroke, cornerShape)
                            .clip(cornerShape),
                        url = getAvatarUrl(state.avatarHash)
                    )
                    Column(Modifier.padding(6.dp)) {
                        Text(
                            modifier = Modifier,
                            text = state.nickname
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            modifier = Modifier,
                            text = state.status.name
                        )
                    }
                }
                /* Online Status */
                Spacer(modifier = Modifier.height(16.dp))
                ProfileStatusItem(
                    modifier = Modifier.padding(0.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = friendOnline
                        )
                    },
                    label = { Text(EPersonaState.Online.name) },
                    selected = selectedItem == EPersonaState.Online,
                    onClick = {
                        selectedItem = EPersonaState.Online
                        onStatusChange(EPersonaState.Online)
                    }
                )

                ProfileStatusItem(
                    modifier = Modifier.padding(0.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = friendOffline
                        )
                    },
                    label = { Text(EPersonaState.Invisible.name) },
                    selected = selectedItem == EPersonaState.Invisible,
                    onClick = {
                        selectedItem = EPersonaState.Invisible
                        onStatusChange(EPersonaState.Invisible)
                    }
                )

                /* Action Buttons */
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onPersonAdd) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(text = "Add Friend")
                }
                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onSettings) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(text = "Settings")
                }

                FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onLogout) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(text = "Log Out")
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
        mutableLongStateOf(items.values.find { it == currentSelection }!!)
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

/**
 * Previews
 */

@Preview
@Composable
private fun Preview_VapullaDialog() {
    val state = HomeState(nickname = "Some Cool Name")

    VapullaTheme {
        VapullaProfileDialog(
            openDialog = true,
            state = state,
            onStatusChange = {},
            onPersonAdd = {},
            onSettings = {},
            onLogout = {},
            onDismiss = {}
        )
    }
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
            icon = Icons.AutoMirrored.Filled.Message,
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
            title = stringResource(id = R.string.dialogTitleNickname),
            editTextLabel = stringResource(id = R.string.textLabelNickname),
            currentName = "Gamer Nickname",
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
        val name = "Gamer Name"
        VapullaMessageDialog(
            icon = Icons.Default.Block,
            title = stringResource(id = R.string.dialogTitleBlockFriend),
            message = stringResource(id = R.string.dialogMessageBlockFriend, name),
            openDialog = true,
            onPositive = {},
            positiveText = stringResource(id = R.string.block),
            onNegative = {},
            negativeText = stringResource(id = R.string.cancel)
        )
    }
}
