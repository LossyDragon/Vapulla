package `in`.dragonbra.vapulla.extension

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

fun Context.getCompatDrawable(@DrawableRes res: Int): Drawable? {
    return AppCompatResources.getDrawable(this, res)
}
