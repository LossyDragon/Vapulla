package `in`.dragonbra.vapulla.ui.composables.dialog

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.mapSaver

@Immutable
data class MessageDialogState(
    val visible: Boolean,
    val type: DialogType = DialogType.NONE,
    val confirmBtnText: String = "OK",
    val dismissBtnText: String = "Cancel",
    val title: String? = null,
    val message: String? = null,
) {
    companion object {
        val Saver = mapSaver(
            save = { state ->
                mapOf(
                    "visible" to state.visible,
                    "type" to state.type,
                    "confirmBtnText" to state.confirmBtnText,
                    "dismissBtnText" to state.dismissBtnText,
                    "title" to state.title,
                    "message" to state.message,
                )
            },
            restore = { savedMap ->
                MessageDialogState(
                    visible = savedMap["visible"] as Boolean,
                    type = savedMap["type"] as DialogType,
                    confirmBtnText = savedMap["confirmBtnText"] as String,
                    dismissBtnText = savedMap["dismissBtnText"] as String,
                    title = savedMap["title"] as String?,
                    message = savedMap["message"] as String?,
                )
            },
        )
    }
}
