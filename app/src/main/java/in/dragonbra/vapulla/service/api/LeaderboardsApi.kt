package `in`.dragonbra.vapulla.service.api

import `in`.dragonbra.javasteam.enums.ELeaderboardDataRequest
import `in`.dragonbra.javasteam.enums.EResult
import `in`.dragonbra.javasteam.steam.handlers.steamuserstats.SteamUserStats
import `in`.dragonbra.vapulla.util.Utils.decodeHtml
import `in`.dragonbra.vapulla.util.httpGetText
import timber.log.Timber

/**
 * A game's leaderboards. The scores on one come from the `SteamUserStats` handler, and the
 * names of the players on it from [ProfileApi]. Steam has no call that lists the
 * leaderboards a game has, so the list is read from the community website.
 */
class LeaderboardsApi(private val userStats: SteamUserStats, private val profile: ProfileApi) {

    /**
     * Every leaderboard of [appId], in the website's order. Empty for a game without any.
     *
     * @throws java.io.IOException if the community website cannot be reached.
     */
    suspend fun boards(appId: Int): List<Leaderboard> {
        val xml = httpGetText(LIST_URL.format(appId))

        return BOARD.findAll(xml).mapNotNull { match ->
            val board = match.groupValues[1]
            val id = board.tag("lbid")?.toIntOrNull() ?: return@mapNotNull null

            Leaderboard(
                id = id,
                // Some games leave the display name empty and only have the internal one.
                name = board.tag("display_name")?.ifEmpty { null } ?: board.tag("name").orEmpty(),
                entries = board.tag("entries")?.toIntOrNull() ?: 0,
                displayType = board.tag("displaytype")?.toIntOrNull() ?: DISPLAY_NUMBER,
            )
        }.toList()
    }

    /**
     * The scores on [board]: the best [TOP_COUNT] of everyone, or with [friendsOnly] those
     * of the account's friends. Empty when Steam does not answer.
     */
    suspend fun scores(
        appId: Int,
        board: Leaderboard,
        friendsOnly: Boolean,
    ): List<LeaderboardScore> {
        val callback = userStats.getLeaderboardEntries(
            appId = appId,
            id = board.id,
            rangeStart = 1,
            rangeEnd = TOP_COUNT,
            dataRequest = if (friendsOnly) {
                ELeaderboardDataRequest.Friends
            } else {
                ELeaderboardDataRequest.Global
            },
        ).awaitOrNull("GetLBEntries") ?: return emptyList()

        if (callback.result != EResult.OK) {
            Timber.w("GetLBEntries for ${board.id} answered ${callback.result}")
            return emptyList()
        }

        val names = profile.names(callback.entries.map { it.steamID.convertToUInt64() })

        return callback.entries
            .sortedBy { it.globalRank }
            .map { entry ->
                val steamId = entry.steamID.convertToUInt64()
                LeaderboardScore(
                    rank = entry.globalRank,
                    steamId = steamId,
                    name = names[steamId].orEmpty(),
                    score = formatScore(entry.score, board.displayType),
                )
            }
    }

    /** A score the way its leaderboard means it: a count, or a time in seconds or milliseconds. */
    private fun formatScore(score: Int, displayType: Int): String = when (displayType) {
        DISPLAY_SECONDS -> "%d:%02d".format(score / 60, score % 60)

        DISPLAY_MILLISECONDS -> {
            val seconds = score / 1000
            "%d:%02d.%03d".format(seconds / 60, seconds % 60, score % 1000)
        }

        else -> "%,d".format(score)
    }

    /** The text inside the XML element [name], without a CDATA wrapper. */
    private fun String.tag(name: String): String? =
        Regex("<$name>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL).find(this)
            ?.groupValues?.get(1)
            ?.removePrefix("<![CDATA[")?.removeSuffix("]]>")
            ?.decodeHtml()
            ?.trim()

    private companion object {
        const val LIST_URL = "https://steamcommunity.com/stats/%d/leaderboards/?xml=1"
        const val TOP_COUNT = 25

        /** Steam's `ELeaderboardDisplayType` values. */
        const val DISPLAY_NUMBER = 1
        const val DISPLAY_SECONDS = 2
        const val DISPLAY_MILLISECONDS = 3

        val BOARD = Regex("<leaderboard>(.*?)</leaderboard>", RegexOption.DOT_MATCHES_ALL)
    }
}
