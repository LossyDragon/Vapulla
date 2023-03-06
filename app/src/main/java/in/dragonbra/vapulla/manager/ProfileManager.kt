package `in`.dragonbra.vapulla.manager

import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.BuildConfig
import `in`.dragonbra.vapulla.model.GamesListItem
import `in`.dragonbra.vapulla.retrofit.SteamApi

class ProfileManager(private val steamApi: SteamApi) {

    fun getGames(steamId: SteamID): GamesListItem {
        val args = hashMapOf(
            "key" to BuildConfig.STEAM_API_KEY,
            "steamid" to steamId.convertToUInt64().toString(),
            "include_played_free_games" to "1",
            "include_appinfo" to "true"
        )

        val response = steamApi.getGamesOwned(args).execute()

        if (!response.isSuccessful) {
            return GamesListItem(0, arrayListOf())
        }

        return GamesListItem(
            response.body()?.gamesResponse?.gameCount ?: 0,
            response.body()?.gamesResponse?.games ?: arrayListOf()
        )
    }

    fun getLevel(steamId: SteamID): Int {
        val args = mapOf(
            "key" to BuildConfig.STEAM_API_KEY,
            "steamid" to steamId.convertToUInt64().toString()
        )

        val response = steamApi.getSteamLevel(args).execute()

        if (!response.isSuccessful) {
            return 0
        }

        return response.body()?.level?.playerLevel ?: 0
    }
}
