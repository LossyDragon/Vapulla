package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.adapter.GamesListItem
import `in`.dragonbra.vapulla.retrofit.SteamApi

class ProfileManager(
    private val steamApi: SteamApi
) {

    fun getGames(steamId: SteamID): GamesListItem {
        val args = java.util.HashMap<String, String>()
        args["key"] = BuildConfig.STEAM_API_KEY
        args["steamid"] = steamId.convertToUInt64().toString()
        args["include_played_free_games"] = "1"
        args["include_appinfo"] = "true"

        var list = GamesListItem(0, arrayListOf())

        val call = steamApi.getGamesOwned(args)
        val response = call.execute()

        if (response.isSuccessful) {
            if (response.body() != null) {
                list = GamesListItem(
                    response.body()!!.gamesResponse!!.gameCount,
                    response.body()!!.gamesResponse!!.games
                )
            }
        }

        return list
    }

    fun getLevel(steamId: SteamID): String? {
        val args = HashMap<String, String>()
        args["key"] = BuildConfig.STEAM_API_KEY
        args["steamid"] = steamId.convertToUInt64().toString()

        val call = steamApi.getSteamLevel(args)
        val response = call.execute()

        return if (response.isSuccessful) {
            var level = 0
            if (response.body() != null) {
                level = response.body()!!.level!!.playerLevel
            }

            level.toString()
        } else {
            null
        }
    }
}
