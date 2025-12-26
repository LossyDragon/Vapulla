package `in`.dragonbra.vapulla.data

import androidx.compose.runtime.Immutable
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamfriends.callback.ProfileInfoCallback
import `in`.dragonbra.javasteam.types.SteamID
import java.util.Date

@Immutable
data class ProfileInfo(
    val steamID: SteamID,
    val timeCreated: Date,
    val realName: String,
    val cityName: String,
    val stateName: String,
    val countryName: String,
    val headline: String,
    val summary: String,
) {
    companion object {
        fun deserialize(response: ProfileInfoCallback): ProfileInfo = ProfileInfo(
            steamID = response.steamID,
            timeCreated = response.timeCreated,
            realName = response.realName,
            cityName = response.cityName,
            stateName = response.stateName,
            countryName = response.countryName,
            headline = response.headline,
            summary = response.summary,
        )
    }
}
