package `in`.dragonbra.vapulla.retrofit.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class GamesLibraryResponse(
    @SerializedName("response")
    var gamesResponse: GamesResponse? = null
)

data class GamesResponse(
    @SerializedName("game_count")
    var gameCount: Int = 0,
    @SerializedName("games")
    var games: ArrayList<Game> = arrayListOf()
)

@Parcelize
data class Game(
    val appid: Int,
    val name: String,
    @SerializedName("playtime_2weeks")
    val playtimeTwoWeeks: Int?,
    @SerializedName("playtime_forever")
    val playtimeForever: Int,
    @SerializedName("img_icon_url")
    val imgIconUrl: String?
) : Parcelable
