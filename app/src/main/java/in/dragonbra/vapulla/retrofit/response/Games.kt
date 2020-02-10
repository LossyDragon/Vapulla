package `in`.dragonbra.vapulla.retrofit.response

data class Games(
        val appid: String,
        val name: String,
        val playtime_2weeks: Long,
        val playtime_forever: String,
        val img_logo_url: String
)
