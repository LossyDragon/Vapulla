package `in`.dragonbra.vapulla.retrofit.response

import com.google.gson.annotations.SerializedName

class ProfileLevelResponse {
    @SerializedName("response")
    var level: LevelResponse? = null
}

class LevelResponse {
    @SerializedName("player_level")
    var playerLevel: Int = 0
}
