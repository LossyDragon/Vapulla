package `in`.dragonbra.vapulla.ui.screens.chat.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

/** How many messages up from the bottom a chat list still follows incoming messages down. */
internal const val FOLLOW_THRESHOLD = 5

/**
 * Shows a chat view model's one-off events in the snackbar. The text of a message that could
 * not be sent is also handed back to the message field.
 */
@Composable
internal fun ChatEvents(
    sendFailures: Flow<String>,
    notices: Flow<String>,
    textFieldState: TextFieldState,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(sendFailures) {
        sendFailures.collect { text ->
            // Hand the text back unless something new has been typed in the meantime.
            if (textFieldState.text.isEmpty()) {
                textFieldState.setTextAndPlaceCursorAtEnd(text)
            }
            snackbarHostState.showSnackbar("Message not sent")
        }
    }

    LaunchedEffect(notices) {
        notices.collect { snackbarHostState.showSnackbar(it) }
    }
}

/**
 * The system photo picker, which needs no storage permission and hands back one image.
 *
 * @return opens the picker; [onPicked] is called if an image is chosen.
 */
@Composable
internal fun rememberImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    return { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
}
