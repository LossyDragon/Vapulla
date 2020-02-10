package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.retrofit.SteamApi
import `in`.dragonbra.vapulla.retrofit.response.Games

class ProfileManager(private val steamApi: SteamApi) {

    fun getGames(steamId: SteamID): Pair<Int?, MutableList<Games>?> {
        val args = java.util.HashMap<String, String>()
        args["key"] = BuildConfig.STEAM_API_KEY
        args["steamid"] = steamId.convertToUInt64().toString()
        args["include_played_free_games"] = "1"

        var pair: Pair<Int?, MutableList<Games>?> = Pair(0, mutableListOf())

        val call = steamApi.getGamesOwned(args)
        val response = call.execute()

        if (response.isSuccessful) {
            if (response.body() != null) {
                pair = Pair(
                        response.body()!!.gamesResponse!!.gameCount,
                        response.body()!!.gamesResponse!!.games
                )
            }
        } else {
            pair = Pair(null, null)
        }

        return pair
    }

    fun getLevel(steamId: SteamID): String? {
        val args = HashMap<String, String>()
        args["key"] = BuildConfig.STEAM_API_KEY
        args["steamid"] = steamId.convertToUInt64().toString()

        val call = steamApi.getSteamLevel(args)
        val response = call.execute()

        return if (response.isSuccessful) {
            var level = 0
            if (response.body() != null)
                level = response.body()!!.level!!.playerLevel

            level.toString()
        } else {
            null
        }
    }
}
