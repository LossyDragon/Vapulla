package `in`.dragonbra.vapulla.retrofit.response

import com.google.gson.annotations.SerializedName

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
