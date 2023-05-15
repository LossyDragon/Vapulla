package `in`.dragonbra.vapulla.compose.screens.invites

import `in`.dragonbra.javasteam.enums.EUniverse
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.vapulla.core.Constants.COMMUNITY_BASE_URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

object InviteUtils {
    private const val communityUrl = COMMUNITY_BASE_URL + "user/"
    private const val publicUrl = "https://s.team/p/"
    private const val r = "0123456789abcdef"
    private const val w = "bcdfghjkmnpqrtvw"

    /**
     * @param inviteToken a Token from CUserAccount_GetFriendInviteTokens_Response
     */
    fun getInviteURL(universe: EUniverse, steamID: SteamID, inviteToken: String): String {
        val baseUrl = if (universe == EUniverse.Public) publicUrl else communityUrl

        @Suppress("RegExpRedundantEscape")
        val pattern = Pattern.compile(":([0-9]+)\\]")
        val matcher = pattern.matcher(steamID.render())

        return if (matcher.find()) {
            val id = matcher.group(1)!!.toLong()
            baseUrl + convertSteamID(id) + "/" + inviteToken.lowercase()
        } else {
            "ERROR"
        }
    }

    /**
     * @param accountid your STEAMID 3 number
     */
    private fun convertSteamID(accountid: Long): String {
        var convertedSteamID = accountid.toString(16)
        val regex = Regex("[0-9a-f]", RegexOption.IGNORE_CASE)

        convertedSteamID = convertedSteamID.replace(regex) { matchResult ->
            w[r.indexOf(matchResult.value.lowercase())].toString()
        }
        convertedSteamID = when {
            convertedSteamID.length >= 8 -> {
                val slice1 = convertedSteamID.slice(0..3)
                val slice2 = convertedSteamID.slice(4 until convertedSteamID.length)
                "$slice1-$slice2"
            }

            convertedSteamID.length >= 6 -> {
                val slice1 = convertedSteamID.slice(0..2)
                val slice2 = convertedSteamID.slice(3 until convertedSteamID.length)
                "$slice1-$slice2"
            }
            else -> convertedSteamID
        }

        return convertedSteamID
    }

    fun getCreatedTime(time: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = time.times(1000)

        val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        return formatter.format(calendar.time)
    }
}
