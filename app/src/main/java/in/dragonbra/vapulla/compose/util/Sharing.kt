package `in`.dragonbra.vapulla.compose.util

import android.content.Context
import android.content.Intent

fun Context.shareLink(url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    Intent.createChooser(sendIntent, null).also(::startActivity)
}
