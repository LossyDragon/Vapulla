package `in`.dragonbra.vapulla.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast

/**
 * Puts [text] on the clipboard. Android shows its own confirmation from version 13 on;
 * before that the app says "Copied" itself.
 *
 * @param label what the text is, for apps that show the clipboard's contents.
 */
fun Context.copyToClipboard(label: String, text: String) {
    getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText(label, text))

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }
}
