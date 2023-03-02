package `in`.dragonbra.vapulla.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsDialog(
    permissionState: PermissionState,
    onPermGranted: () -> Unit,
    onSettings: () -> Unit
) {
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showRationale by remember { mutableStateOf(false) }
    var permissionsMsg by remember { mutableStateOf("") }

    LaunchedEffect(permissionState.status) {
        showPermissionsDialog = !permissionState.status.isGranted

        if (permissionState.status.isGranted) {
            onPermGranted()
            return@LaunchedEffect
        }

        showRationale = permissionState.status.shouldShowRationale
        permissionsMsg = if (showRationale) {
            "This app requires permission to post notifications. " +
                "This allows a status icon to be displayed while " +
                "the app is running and to receive messages and requests. " +
                "Without this permission, you won't be notified and the app " +
                "may prematurely terminate"
        } else {
            "This app cannot post notifications. " +
                "The permission can be changed in the App's Settings."
        }
    }

    /* Permissions Dialog */
    VapullaMessageDialog(
        title = "Permission required",
        message = permissionsMsg,
        openDialog = showPermissionsDialog,
        onPositive = {
            if (showRationale) {
                permissionState.launchPermissionRequest()
            } else {
                onSettings()
            }

            showPermissionsDialog = false
        },
        positiveText = if (showRationale) "Grant" else "Settings",
        onNegative = {
            showPermissionsDialog = false
        },
        negativeText = "Dismiss"
    )
}
