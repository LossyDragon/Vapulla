package `in`.dragonbra.vapulla.retrofit

import `in`.dragonbra.vapulla.retrofit.response.GamesLibraryResponse
import `in`.dragonbra.vapulla.retrofit.response.ProfileLevelResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface SteamApi {

    @GET("IPlayerService/GetSteamLevel/v0001/")
    fun getSteamLevel(@QueryMap args: Map<String, String>): Call<ProfileLevelResponse>

    @GET("IPlayerService/GetOwnedGames/v0001/?include_appinfo=1&format=json")
    fun getGamesOwned(@QueryMap args: Map<String, String>): Call<GamesLibraryResponse>
}
