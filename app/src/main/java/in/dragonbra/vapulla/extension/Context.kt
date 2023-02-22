package `in`.dragonbra.vapulla.extension

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.R
import timber.log.Timber

fun Context.getCompatDrawable(@DrawableRes res: Int): Drawable? {
    return AppCompatResources.getDrawable(this, res)
}

fun Context.getErrorMessage(eResult: EResult, extendedResult: EResult? = null): String {
    val result = when (eResult) {
        EResult.NoConnection -> getString(R.string.errorMessageLostConnection)
        EResult.InvalidPassword -> getString(R.string.errorMessageInvalidPassword)
        EResult.TwoFactorCodeMismatch -> getString(R.string.errorMessageTwoFactorCodeMismatch)
        EResult.InvalidLoginAuthCode -> getString(R.string.errorMessageInvalidLoginAuthCode)
        else -> eResult.toString()
    }

    Timber.w("getErrorMessage(): $extendedResult")

    return result
}
