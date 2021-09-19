package `in`.dragonbra.vapulla.retrofit.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Games(
    val appid: Int,
    val name: String,
    val playtime_2weeks: Int?,
    val playtime_forever: Int,
    val img_logo_url: String?
) : Parcelable
