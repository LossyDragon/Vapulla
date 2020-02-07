package `in`.dragonbra.vapulla.chat

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.style.ImageSpan
import android.widget.TextView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.penfeizhou.animation.apng.APNGDrawable
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class StickerTarget(val context: Context,
                    val view: TextView,
                    private val span: Spannable,
                    val start: Int,
                    private val end: Int,
                    private val targets: MutableList<Any>?
) : CustomTarget<File>() {

    @Volatile
    private var cancelled = false

    fun cancel() {
        cancelled = true
    }

    // Adapted from: https://github.com/penfeizhou/APNG4Android/issues/14
    override fun onResourceReady(resourceFile: File, transition: Transition<in File>?) {
        if (!cancelled) {
            val resource: Drawable = APNGDrawable.fromFile(resourceFile.absolutePath)
            resource.setBounds(0, 0, 150, 150)
            val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BOTTOM)
            span.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.text = span
            resource.setVisible(true, true)
            view.requestLayout()
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(
                            {
                                view.postInvalidate()
                            }, 0, 100, TimeUnit.MILLISECONDS)

            targets?.remove(this)
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {
    }
}
