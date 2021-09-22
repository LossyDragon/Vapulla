package `in`.dragonbra.vapulla.extension

import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.vapulla.R
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

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

    Log.w(this::class.java.simpleName, "getErrorMessage(): $extendedResult")

    return result
}
