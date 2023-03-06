package `in`.dragonbra.vapulla.retrofit.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

class GamesLibraryResponse {
    @SerializedName("response")
    var gamesResponse: GamesResponse? = null
}

class GamesResponse {
    @SerializedName("game_count")
    var gameCount: Int = 0

    @SerializedName("games")
    var games: ArrayList<Games> = arrayListOf()
}

@Parcelize
data class Games(
    val appid: Int,
    val name: String,
    val playtime_2weeks: Int?,
    val playtime_forever: Int,
    val img_icon_url: String?
) : Parcelable
