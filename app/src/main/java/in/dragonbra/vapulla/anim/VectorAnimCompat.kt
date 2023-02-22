package `in`.dragonbra.vapulla.anim

import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat

object VectorAnimCompat {

    fun registerAnimationCallback(
        drawable: Animatable,
        callback: Animatable2Compat.AnimationCallback
    ) {
        val d = drawable as? AnimatedVectorDrawable
        d?.registerAnimationCallback(object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                callback.onAnimationEnd(drawable)
            }

            override fun onAnimationStart(drawable: Drawable?) {
                callback.onAnimationStart(drawable)
            }
        })
    }

    fun clearAnimationCallbacks(drawable: Animatable) {
        (drawable as? AnimatedVectorDrawable)?.clearAnimationCallbacks()
    }
}
