package `in`.dragonbra.vapulla.ui.composables.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.dragonbra.vapulla.ui.theme.VapullaTheme

/**
 * Confirms logging out. [onConfirm] is told whether the box was ticked: the saved sign-in is
 * then forgotten too, along with what is stored about the account. Left unticked, logging
 * out only goes offline.
 */
@Composable
fun LogOutDialog(onConfirm: (clearCredentials: Boolean) -> Unit, onDismiss: () -> Unit) {
    var clearCredentials by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
        title = { Text("Log out?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (clearCredentials) {
                        "Your sign-in is forgotten, and this account's friends, chats and " +
                            "library are erased from the device. Downloaded games are kept."
                    } else {
                        "You go offline, but your sign-in stays on this device, so you can " +
                            "sign back in without your password."
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = clearCredentials,
                            role = Role.Checkbox,
                            onValueChange = { clearCredentials = it },
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The row is the click target; the box only shows the state.
                    Checkbox(checked = clearCredentials, onCheckedChange = null)
                    Text("Forget my sign-in on this device")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clearCredentials) }) { Text("Log out") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Preview
@Composable
private fun Preview_LogOutDialog() {
    VapullaTheme {
        Box(Modifier.fillMaxSize()) {
            LogOutDialog(onConfirm = {}, onDismiss = {})
        }
    }
}
