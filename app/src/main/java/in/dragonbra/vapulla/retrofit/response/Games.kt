package `in`.dragonbra.vapulla.retrofit.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Games(
    val appid: Int,
    val name: String,
    val playtime_2weeks: Int?,
    val playtime_forever: Int,
    val img_icon_url: String? // img_logo_url no longer exists in response.
) : Parcelable
