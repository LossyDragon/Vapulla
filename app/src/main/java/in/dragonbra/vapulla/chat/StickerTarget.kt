package `in`.dragonbra.vapulla.chat

// import android.content.Context
// import android.graphics.drawable.Drawable
// import android.text.Spannable
// import android.text.style.ImageSpan
// import android.widget.TextView
// import coil.ImageLoader
// import coil.decode.DecodeResult
// import coil.decode.Decoder
// import coil.decode.ImageSource
// import coil.fetch.SourceResult
// import coil.request.Options
// import coil.target.Target
// import com.github.penfeizhou.animation.apng.APNGDrawable
// import com.github.penfeizhou.animation.apng.decode.APNGParser
// import timber.log.Timber
// import java.io.File
// import java.util.concurrent.Executors
// import java.util.concurrent.TimeUnit
//
// class AnimatedPngDecoder(private val source: ImageSource) : Decoder {
//    override suspend fun decode(): DecodeResult {
//        return DecodeResult(
//            drawable = APNGDrawable.fromFile(source.file().toString()),
//            isSampled = false
//        )
//    }
//
//    class Factory : Decoder.Factory {
//        override fun create(
//            result: SourceResult,
//            options: Options,
//            imageLoader: ImageLoader
//        ): Decoder? {
//            val filePath = result.source.file().toFile().path
//            if (APNGParser.isAPNG(filePath)) {
//                return AnimatedPngDecoder(result.source)
//            }
//
//            return null
//        }
//    }
// }
//
// class StickerTarget(
//    val context: Context,
//    val view: TextView,
//    private val span: Spannable,
//    val start: Int,
//    private val end: Int,
//    private val targets: MutableList<Any>?
// ) : Target {
//
//    @Volatile
//    private var cancelled = false
//
//    fun cancel() {
//        cancelled = true
//    }
//
//    override fun onError(error: Drawable?) {
//        super.onError(error)
//        Timber.d("onError")
//    }
//
//    override fun onStart(placeholder: Drawable?) {
//        super.onStart(placeholder)
//        Timber.d("onStart")
//    }
//
//    // Adapted from: https://github.com/penfeizhou/APNG4Android/issues/14
//    // Alternative: https://github.com/line/apng-drawable
//    override fun onSuccess(result: Drawable) {
//        super.onSuccess(result)
//        Timber.d("onSuccess")
//        if (!cancelled) {
//            result.
//            val resource: Drawable = APNGDrawable.fromFile(resourceFile.absolutePath)
//            resource.setBounds(0, 0, 150, 150)
//            val imageSpan = ImageSpan(resource, ImageSpan.ALIGN_BOTTOM)
//            span.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//            view.text = span
//            resource.setVisible(true, true)
//            view.requestLayout()
//            Executors.newSingleThreadScheduledExecutor()
//                .scheduleAtFixedRate(
//                    { view.postInvalidate() },
//                    0,
//                    100,
//                    TimeUnit.MILLISECONDS
//                )
//
//            targets?.remove(this)
//        }
//    }
// }
