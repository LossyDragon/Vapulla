package `in`.dragonbra.vapulla.anim

import `in`.dragonbra.vapulla.util.Utils.isLessThanN
import android.graphics.drawable.Animatable
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

object VectorAnimCompat {

    fun registerAnimationCallback(
            drawable: Animatable,
            callback: Animatable2Compat.AnimationCallback
    ) {
        if (isLessThanN()) {
            val d = drawable as? AnimatedVectorDrawableCompat
            d?.registerAnimationCallback(callback)
        } else {
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
    }

    fun clearAnimationCallbacks(drawable: Animatable) {
        if (isLessThanN()) {
            (drawable as? AnimatedVectorDrawableCompat)?.clearAnimationCallbacks()
        } else {
            (drawable as? AnimatedVectorDrawable)?.clearAnimationCallbacks()
        }
    }
}
