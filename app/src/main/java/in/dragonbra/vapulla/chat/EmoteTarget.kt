package `in`.dragonbra.vapulla.chat

import `in`.dragonbra.vapulla.util.Utils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ImageSpan
import android.widget.TextView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.math.roundToInt

class EmoteTarget(val context: Context,
                  val view: TextView,
                  private val span: Spannable,
                  val start: Int,
                  val end: Int,
                  sizeDp: Float,
                  private val targets: MutableList<Any>?) :
        CustomTarget<Bitmap>(
                Utils.convertDpToPixel(sizeDp, context).roundToInt(),
                Utils.convertDpToPixel(sizeDp, context).roundToInt()
        ) {

    @Volatile
    private var cancelled = false

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        if (!cancelled) {
            val imageSpan = ImageSpan(context, resource)
            span.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.text = span
            view.requestLayout()
            targets?.remove(this)
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        // Nothing
    }

    fun cancel() {
        cancelled = true
    }
}
