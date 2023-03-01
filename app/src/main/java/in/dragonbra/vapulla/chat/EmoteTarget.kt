package `in`.dragonbra.vapulla.chat

import `in`.dragonbra.vapulla.util.Utils
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import coil.target.Target
import timber.log.Timber
import kotlin.math.roundToInt

class EmoteTarget(
    val context: Context,
    private val view: TextView,
    private val span: Spannable,
    private val start: Int,
    private val end: Int,
    sizeDp: Float,
    private val targets: MutableList<Any>?
) : Target {

    val size = Utils.convertDpToPixel(sizeDp, context).roundToInt()
    //  Utils.convertDpToPixel(sizeDp, context).roundToInt()

    @Volatile
    private var cancelled = false

    override fun onError(error: Drawable?) {
        super.onError(error)
        Timber.d("onError")
    }

    override fun onStart(placeholder: Drawable?) {
        super.onStart(placeholder)
        Timber.d("onStart")
    }

    override fun onSuccess(result: Drawable) {
        super.onSuccess(result)
        Timber.d("onSuccess")
        if (!cancelled) {
            val imageSpan = ImageSpan(context, result.toBitmap(size, size))
            span.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.text = span
            view.requestLayout()
            targets?.remove(this)
        }
    }

    fun cancel() {
        cancelled = true
    }
}
